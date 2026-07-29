package com.togetherly.feature.family.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.result.DataResult
import com.togetherly.core.ui.UiText
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.localdata.usecase.DeleteAllLocalData
import com.togetherly.domain.localdata.usecase.DeleteMemories
import com.togetherly.domain.localdata.usecase.ResetQuestHistory
import com.togetherly.feature.family.model.DataManagementAction
import com.togetherly.feature.family.model.DataManagementConfirmationStage
import com.togetherly.feature.family.model.DataManagementEvent
import com.togetherly.feature.family.model.DataManagementUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.data_management_delete_memories_success
import togetherly.shared.generated.resources.data_management_partial_file_failure_message
import togetherly.shared.generated.resources.data_management_reset_quest_history_success

/**
 * [DataManagementUiState.isBusy] gates every destructive entry point (see
 * [DataManagementAction.DeleteMemoriesClicked]/[DataManagementAction.ResetQuestHistoryClicked]/
 * [DataManagementAction.DeleteAllDataClicked] all no-op while busy) and back navigation
 * ([DataManagementAction.BackClicked]) — a deletion already running can never be duplicated by a
 * second tap, and the screen can't be backed out of mid-operation. This is UI-level throttling on
 * top of (not instead of) each use case's own internal concurrency guard (see
 * [DeleteMemories]/[ResetQuestHistory]/[DeleteAllLocalData]'s own KDoc on their `Mutex`).
 *
 * Never performs multi-repository deletion logic itself — every actual delete is one call to
 * [deleteMemories]/[resetQuestHistory]/[deleteAllLocalData], each already the single coordinated
 * entry point for its own operation.
 */
class DataManagementViewModel(
    private val deleteMemories: DeleteMemories,
    private val resetQuestHistory: ResetQuestHistory,
    private val deleteAllLocalData: DeleteAllLocalData,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataManagementUiState())
    val uiState: StateFlow<DataManagementUiState> = _uiState.asStateFlow()

    private val _events = Channel<DataManagementEvent>(Channel.BUFFERED)
    val events: Flow<DataManagementEvent> = _events.receiveAsFlow()

    fun onAction(action: DataManagementAction) {
        when (action) {
            DataManagementAction.BackClicked -> if (!_uiState.value.isBusy) send(DataManagementEvent.NavigateBack)
            DataManagementAction.DeleteMemoriesClicked -> showConfirmation(DataManagementConfirmationStage.CONFIRM_DELETE_MEMORIES)
            DataManagementAction.ResetQuestHistoryClicked -> showConfirmation(DataManagementConfirmationStage.CONFIRM_RESET_QUEST_HISTORY)
            DataManagementAction.DeleteAllDataClicked -> showConfirmation(DataManagementConfirmationStage.DELETE_ALL_STAGE_ONE)
            DataManagementAction.DeleteAllDataContinueClicked -> showConfirmation(DataManagementConfirmationStage.DELETE_ALL_STAGE_TWO)
            DataManagementAction.ConfirmationDismissed -> showConfirmation(DataManagementConfirmationStage.NONE)
            DataManagementAction.DestructiveActionConfirmed -> confirmCurrentStage()
            DataManagementAction.MessageDismissed -> _uiState.update { it.copy(message = null) }
        }
    }

    private fun showConfirmation(stage: DataManagementConfirmationStage) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(confirmationStage = stage) }
    }

    private fun confirmCurrentStage() {
        when (_uiState.value.confirmationStage) {
            DataManagementConfirmationStage.CONFIRM_DELETE_MEMORIES -> runDeleteMemories()
            DataManagementConfirmationStage.CONFIRM_RESET_QUEST_HISTORY -> runResetQuestHistory()
            DataManagementConfirmationStage.DELETE_ALL_STAGE_TWO -> runDeleteAllData()
            DataManagementConfirmationStage.NONE,
            DataManagementConfirmationStage.DELETE_ALL_STAGE_ONE,
            -> Unit
        }
    }

    private fun runDeleteMemories() {
        _uiState.update { it.copy(confirmationStage = DataManagementConfirmationStage.NONE, isDeletingMemories = true) }
        viewModelScope.launch {
            val message = when (val result = deleteMemories()) {
                is DataResult.Success -> result.value.toOutcomeMessage(Res.string.data_management_delete_memories_success)
                is DataResult.Error -> result.error.toUiText()
            }
            _uiState.update { it.copy(isDeletingMemories = false, message = message) }
        }
    }

    private fun runResetQuestHistory() {
        _uiState.update { it.copy(confirmationStage = DataManagementConfirmationStage.NONE, isResettingQuestHistory = true) }
        viewModelScope.launch {
            val message = when (val result = resetQuestHistory()) {
                is DataResult.Success -> result.value.toOutcomeMessage(Res.string.data_management_reset_quest_history_success)
                is DataResult.Error -> result.error.toUiText()
            }
            _uiState.update { it.copy(isResettingQuestHistory = false, message = message) }
        }
    }

    private fun runDeleteAllData() {
        _uiState.update { it.copy(confirmationStage = DataManagementConfirmationStage.NONE, isDeletingAllData = true) }
        viewModelScope.launch {
            when (val result = deleteAllLocalData()) {
                is DataResult.Success -> {
                    _uiState.update { it.copy(isDeletingAllData = false) }
                    send(DataManagementEvent.LocalDataDeleted)
                }
                is DataResult.Error -> _uiState.update { it.copy(isDeletingAllData = false, message = result.error.toUiText()) }
            }
        }
    }

    private fun send(event: DataManagementEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}

/** 0 failed file deletions means the plain success copy; any failure surfaces the softer "some files couldn't be removed" notice instead — see [DeleteMemories]/[ResetQuestHistory]'s own KDoc on why that's still an overall success. */
private fun Int.toOutcomeMessage(successResource: StringResource): UiText =
    if (this == 0) UiText.Resource(successResource) else UiText.Resource(Res.string.data_management_partial_file_failure_message)

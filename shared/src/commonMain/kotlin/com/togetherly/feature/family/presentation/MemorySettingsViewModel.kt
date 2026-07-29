package com.togetherly.feature.family.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.result.DataResult
import com.togetherly.core.ui.UiText
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.family.MemoryPreferences
import com.togetherly.domain.family.usecase.ObserveFamilySettings
import com.togetherly.domain.family.usecase.UpdateMemoryPreferences
import com.togetherly.feature.family.model.MemorySettingsAction
import com.togetherly.feature.family.model.MemorySettingsEvent
import com.togetherly.feature.family.model.MemorySettingsUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.memory_settings_missing_profile_error
import togetherly.shared.generated.resources.memory_settings_updated_message

/**
 * Same "load a draft once, edit locally" shape as [QuestPreferencesViewModel]/
 * [com.togetherly.feature.reminder.presentation.ReminderViewModel]. No validation object — every
 * combination of these four booleans is valid (see [MemorySettingsUiState]'s own KDoc), so
 * [onSave] never needs to check anything before writing.
 */
class MemorySettingsViewModel(
    private val observeFamilySettings: ObserveFamilySettings,
    private val updateMemoryPreferences: UpdateMemoryPreferences,
    private val saveMessageStore: FamilySaveMessageStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemorySettingsUiState())
    val uiState: StateFlow<MemorySettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<MemorySettingsEvent>(Channel.BUFFERED)
    val events: Flow<MemorySettingsEvent> = _events.receiveAsFlow()

    private var hasStarted = false
    private var original: MemoryPreferences? = null

    fun onScreenStarted() {
        if (hasStarted) return
        hasStarted = true

        viewModelScope.launch {
            when (val result = observeFamilySettings().first()) {
                is DataResult.Success -> {
                    val settings = result.value
                    if (settings != null) {
                        val preferences = settings.memoryPreferences
                        original = preferences
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                allowPhotos = preferences.allowPhotos,
                                allowVoiceMemories = preferences.allowVoiceMemories,
                                allowTextNotes = preferences.allowTextNotes,
                                showMemoryPromptAfterQuests = preferences.showMemoryPromptAfterQuests,
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = UiText.Resource(Res.string.memory_settings_missing_profile_error)) }
                    }
                }
                is DataResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.error.toUiText()) }
            }
        }
    }

    fun onAction(action: MemorySettingsAction) {
        when (action) {
            is MemorySettingsAction.AllowPhotosChanged -> updateDraft { it.copy(allowPhotos = action.value) }
            is MemorySettingsAction.AllowVoiceMemoriesChanged -> updateDraft { it.copy(allowVoiceMemories = action.value) }
            is MemorySettingsAction.AllowTextNotesChanged -> updateDraft { it.copy(allowTextNotes = action.value) }
            is MemorySettingsAction.ShowMemoryPromptAfterQuestsChanged -> updateDraft { it.copy(showMemoryPromptAfterQuests = action.value) }
            MemorySettingsAction.ManageMemoriesClicked -> viewModelScope.launch { _events.send(MemorySettingsEvent.OpenManageMemories) }
            MemorySettingsAction.SaveClicked -> onSave()
            MemorySettingsAction.BackClicked -> onBack()
            MemorySettingsAction.DiscardConfirmed -> onDiscardConfirmed()
            MemorySettingsAction.DismissDiscardDialog -> _uiState.update { it.copy(showDiscardDialog = false) }
        }
    }

    private fun updateDraft(transform: (MemorySettingsUiState) -> MemorySettingsUiState) {
        _uiState.update { current ->
            val next = transform(current)
            next.copy(error = null, hasUnsavedChanges = next.differsFromOriginal(original))
        }
    }

    private fun onBack() {
        if (_uiState.value.hasUnsavedChanges) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            viewModelScope.launch { _events.send(MemorySettingsEvent.NavigatedBackWithoutSaving) }
        }
    }

    private fun onDiscardConfirmed() {
        _uiState.update { it.copy(showDiscardDialog = false) }
        viewModelScope.launch { _events.send(MemorySettingsEvent.NavigatedBackWithoutSaving) }
    }

    private fun onSave() {
        val current = _uiState.value
        if (current.isSaving) return

        val preferences = MemoryPreferences(
            allowPhotos = current.allowPhotos,
            allowVoiceMemories = current.allowVoiceMemories,
            allowTextNotes = current.allowTextNotes,
            showMemoryPromptAfterQuests = current.showMemoryPromptAfterQuests,
        )
        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            when (val result = updateMemoryPreferences(preferences)) {
                is DataResult.Success -> {
                    original = preferences
                    _uiState.update { it.copy(isSaving = false, hasUnsavedChanges = false) }
                    saveMessageStore.publish(UiText.Resource(Res.string.memory_settings_updated_message))
                    _events.send(MemorySettingsEvent.SaveCompleted)
                }
                is DataResult.Error -> _uiState.update { it.copy(isSaving = false, error = result.error.toUiText()) }
            }
        }
    }
}

private fun MemorySettingsUiState.differsFromOriginal(original: MemoryPreferences?): Boolean {
    val baseline = original ?: return false
    return allowPhotos != baseline.allowPhotos ||
        allowVoiceMemories != baseline.allowVoiceMemories ||
        allowTextNotes != baseline.allowTextNotes ||
        showMemoryPromptAfterQuests != baseline.showMemoryPromptAfterQuests
}

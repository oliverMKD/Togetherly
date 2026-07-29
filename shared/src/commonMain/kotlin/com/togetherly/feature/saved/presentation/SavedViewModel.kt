package com.togetherly.feature.saved.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.result.DataResult
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.explore.usecase.EvaluateQuestAccessUseCase
import com.togetherly.domain.explore.usecase.ObserveSavedQuestsUseCase
import com.togetherly.domain.explore.usecase.ToggleSavedQuestUseCase
import com.togetherly.domain.purchase.repository.EntitlementRepository
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.feature.explore.mapper.toExploreQuestUi
import com.togetherly.feature.saved.model.SavedUiState
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Observes [ObserveSavedQuestsUseCase] and [EntitlementRepository.observeAccess] together, the same
 * pairing [com.togetherly.feature.explore.presentation.ExploreViewModel] and
 * [com.togetherly.feature.questdetail.presentation.QuestDetailViewModel] both use — a premium saved
 * quest reactively flips from locked to unlocked the instant an entitlement activates while this
 * screen is on screen, no refresh needed, for the same reason theirs do (see
 * [EntitlementRepository]'s own live-Flow-backed implementation).
 *
 * [SavedAction.SaveClicked] (unsaving) removes the quest from [SavedUiState.quests] simply by the
 * next [ObserveSavedQuestsUseCase] emission no longer including it — never a manual local-list edit
 * — so Explore and Saved never fall out of sync with each other or with the repository.
 */
class SavedViewModel(
    private val observeSavedQuests: ObserveSavedQuestsUseCase,
    private val toggleSavedQuest: ToggleSavedQuestUseCase,
    private val evaluateQuestAccess: EvaluateQuestAccessUseCase,
    private val entitlementRepository: EntitlementRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedUiState.initial())
    val uiState: StateFlow<SavedUiState> = _uiState.asStateFlow()

    private val _events = Channel<SavedEvent>(Channel.BUFFERED)
    val events: Flow<SavedEvent> = _events.receiveAsFlow()

    private val retrySignal = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    private var hasStarted = false

    fun onAction(action: SavedAction) {
        when (action) {
            is SavedAction.QuestClicked -> viewModelScope.launch { _events.send(SavedEvent.OpenQuestDetail(action.questId)) }
            is SavedAction.SaveClicked -> viewModelScope.launch { toggleSavedQuest(action.questId) }
            SavedAction.BackClicked -> viewModelScope.launch { _events.send(SavedEvent.NavigateBack) }
            SavedAction.RetryClicked -> {
                _uiState.update { it.copy(isLoading = true, error = null) }
                retrySignal.tryEmit(Unit)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun onScreenStarted() {
        if (hasStarted) return
        hasStarted = true

        viewModelScope.launch {
            retrySignal.flatMapLatest {
                // The access Flow's own value is unused — its re-emission is what re-triggers this
                // combine and re-runs evaluateQuestAccess with fresh entitlements below.
                combine(observeSavedQuests(), entitlementRepository.observeAccess()) { savedResult, _ -> savedResult }
            }.collect { savedResult -> applyInput(savedResult) }
        }
    }

    private suspend fun applyInput(savedResult: DataResult<List<FamilyQuest>>) {
        if (savedResult is DataResult.Error) {
            _uiState.update { it.copy(isLoading = false, error = savedResult.error.toUiText()) }
            return
        }
        val quests = (savedResult as DataResult.Success).value
        val items = quests
            .map { quest -> quest.toExploreQuestUi(isSaved = true, locked = !evaluateQuestAccess(quest)) }
            .toPersistentList()

        _uiState.update { it.copy(isLoading = false, quests = items, error = null) }
    }
}

package com.togetherly.feature.completion.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.datetime.AppTimeZoneProvider
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.core.datetime.localizedDateTimeDisplay
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.ProductAnalytics
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.repository.CompletionRepository
import com.togetherly.domain.family.MemoryPreferences
import com.togetherly.domain.family.usecase.ObserveFamilySettings
import com.togetherly.domain.quest.repository.QuestRepository
import com.togetherly.feature.completion.model.CompletionCelebrationQuestUi
import com.togetherly.feature.completion.model.CompletionCelebrationUiState
import com.togetherly.feature.today.mapper.toUi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime

/**
 * [completionId] is supplied at construction (Koin `parametersOf`), mirroring
 * [com.togetherly.feature.questmode.presentation.QuestModeViewModel]. A missing quest in the
 * catalogue never hides the completion itself — see [CompletionCelebrationUiState]'s own KDoc.
 */
class CompletionCelebrationViewModel(
    private val completionId: CompletionId,
    private val completionRepository: CompletionRepository,
    private val questRepository: QuestRepository,
    private val observeFamilySettings: ObserveFamilySettings,
    private val timeZoneProvider: AppTimeZoneProvider,
    private val analytics: ProductAnalytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CompletionCelebrationUiState>(CompletionCelebrationUiState.Loading)
    val uiState: StateFlow<CompletionCelebrationUiState> = _uiState.asStateFlow()

    private val _events = Channel<CompletionCelebrationEvent>(Channel.BUFFERED)
    val events: Flow<CompletionCelebrationEvent> = _events.receiveAsFlow()

    private var hasStarted = false

    fun onAction(action: CompletionCelebrationAction) {
        when (action) {
            CompletionCelebrationAction.ScreenStarted -> onScreenStarted()
            CompletionCelebrationAction.RetryClicked -> load()
            CompletionCelebrationAction.ContinueClicked ->
                viewModelScope.launch { _events.send(CompletionCelebrationEvent.NavigateToToday) }
            CompletionCelebrationAction.AddMemoryClicked ->
                viewModelScope.launch { _events.send(CompletionCelebrationEvent.NavigateToMemory(completionId)) }
        }
    }

    fun onScreenStarted() {
        if (hasStarted) return
        hasStarted = true
        analytics.screen(AnalyticsScreen.COMPLETION)
        load()
    }

    private fun load() {
        _uiState.value = CompletionCelebrationUiState.Loading
        viewModelScope.launch {
            val completionResult = completionRepository.getCompletion(completionId)
            if (completionResult is DataResult.Error) {
                _uiState.value = CompletionCelebrationUiState.Error(completionResult.error.toUiText())
                return@launch
            }
            val completion = (completionResult as DataResult.Success).value
            if (completion == null) {
                _uiState.value = CompletionCelebrationUiState.Error(
                    AppError.Validation(ValidationError.COMPLETION_NOT_FOUND).toUiText(),
                )
                return@launch
            }

            val questResult = questRepository.getQuest(completion.questId)
            val quest = (questResult as? DataResult.Success)?.value
            val questUi = quest?.let { CompletionCelebrationQuestUi(title = it.title.value, category = it.category.toUi()) }

            val displayDate = completion.completedAt.toLocalDateTime(timeZoneProvider.current()).localizedDateTimeDisplay()
            val memoryPreferences = when (val result = observeFamilySettings().first()) {
                is DataResult.Success -> result.value?.memoryPreferences ?: MemoryPreferences.defaults()
                is DataResult.Error -> MemoryPreferences.defaults()
            }
            _uiState.value = CompletionCelebrationUiState.Content(
                quest = questUi,
                completedAtDisplay = displayDate,
                showAddMemoryPrompt = memoryPreferences.showMemoryPromptAfterQuests,
            )
        }
    }
}

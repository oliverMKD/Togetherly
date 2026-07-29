package com.togetherly.feature.questmode.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.ProductAnalytics
import com.togetherly.core.telemetry.QuestAbandoned
import com.togetherly.core.telemetry.QuestCompleted
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.usecase.CompleteQuest
import com.togetherly.domain.completion.usecase.CompletionTransitionResult
import com.togetherly.domain.completion.usecase.ResolveCompletionTransition
import com.togetherly.domain.purchase.repository.CustomerAttributesRepository
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.questmode.LoadQuestModeResult
import com.togetherly.domain.questmode.QuestCountdownEngine
import com.togetherly.domain.questmode.QuestModeSession
import com.togetherly.domain.questmode.QuestTimerState
import com.togetherly.domain.questmode.usecase.AbandonQuest
import com.togetherly.domain.questmode.usecase.LoadQuestMode
import com.togetherly.domain.recommendation.durationBandFor
import com.togetherly.feature.questmode.mapper.toContentUi
import com.togetherly.feature.questmode.mapper.toUi
import com.togetherly.feature.questmode.model.QuestTimerUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [completionId] is supplied at construction (Koin `parametersOf`, mirroring
 * [com.togetherly.feature.questdetail.presentation.QuestDetailViewModel]) — never read from a
 * `SavedStateHandle` inside this class.
 *
 * Countdown collection starts at most once per [QuestModeViewModel] instance (see [countdownJob])
 * — recomposition or a repeated [QuestModeAction.ScreenStarted] never spins up a second collector,
 * and [QuestModeEvent.TimerFinished] fires at most once per instance (see [timerFinishedEmitted]),
 * since [com.togetherly.domain.questmode.QuestCountdownEngine.observe]'s own `Flow` already
 * completes after its one `Finished` emission (see that interface's own KDoc).
 */
class QuestModeViewModel(
    private val completionId: CompletionId,
    private val loadQuestMode: LoadQuestMode,
    private val countdownEngine: QuestCountdownEngine,
    private val completeQuest: CompleteQuest,
    private val abandonQuest: AbandonQuest,
    private val resolveCompletionTransition: ResolveCompletionTransition,
    private val analytics: ProductAnalytics,
    private val customerAttributesRepository: CustomerAttributesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuestModeUiState>(QuestModeUiState.Loading)
    val uiState: StateFlow<QuestModeUiState> = _uiState.asStateFlow()

    private val _events = Channel<QuestModeEvent>(Channel.BUFFERED)
    val events: Flow<QuestModeEvent> = _events.receiveAsFlow()

    private var hasStarted = false
    private var countdownJob: Job? = null
    private var timerFinishedEmitted = false
    private var isAbandoning = false
    private var isCompletingGuard = false

    /** [QuestModeContentUi] only carries UI-mapped fields — analytics needs the stable domain enums this caches from [load]'s successful session. */
    private var currentQuest: FamilyQuest? = null

    fun onAction(action: QuestModeAction) {
        when (action) {
            QuestModeAction.ScreenStarted -> onScreenStarted()
            QuestModeAction.RetryClicked -> load()
            QuestModeAction.BackClicked -> updateContent { it.copy(showExitConfirmation = true) }
            QuestModeAction.CloseClicked -> viewModelScope.launch { _events.send(QuestModeEvent.NavigateBack) }
            QuestModeAction.KeepInProgressClicked -> {
                updateContent { it.copy(showExitConfirmation = false) }
                viewModelScope.launch { _events.send(QuestModeEvent.NavigateBack) }
            }
            QuestModeAction.AbandonClicked -> updateContent {
                it.copy(showExitConfirmation = false, showAbandonConfirmation = true)
            }
            QuestModeAction.AbandonConfirmed -> onAbandonConfirmed()
            QuestModeAction.DialogDismissed -> updateContent {
                it.copy(showExitConfirmation = false, showAbandonConfirmation = false)
            }
            QuestModeAction.PhoneDownClicked -> updateContent { it.copy(phoneDown = true) }
            QuestModeAction.ExitPhoneDownClicked -> updateContent { it.copy(phoneDown = false) }
            QuestModeAction.HintsToggled -> updateContent { it.copy(hintsExpanded = !it.hintsExpanded) }
            QuestModeAction.CompleteClicked -> onCompleteClicked()
            QuestModeAction.ErrorDismissed -> updateContent { it.copy(error = null) }
        }
    }

    fun onScreenStarted() {
        if (hasStarted) return
        hasStarted = true
        analytics.screen(AnalyticsScreen.QUEST_MODE)
        load()
    }

    private fun load() {
        _uiState.value = QuestModeUiState.Loading
        viewModelScope.launch {
            when (val result = loadQuestMode(completionId)) {
                is LoadQuestModeResult.Success -> {
                    val contentUi = result.session.activeSession.toContentUi(result.session.quest, result.session.timerState)
                    currentQuest = result.session.quest
                    _uiState.value = QuestModeUiState.Content(quest = contentUi)
                    maybeHandleFinishedOnLoad(result.session.timerState)
                    startCountdownIfNeeded(result.session)
                }
                LoadQuestModeResult.NoActiveSession, LoadQuestModeResult.SessionMismatch -> onNoActiveSession()
                LoadQuestModeResult.QuestUnavailable ->
                    _uiState.value = QuestModeUiState.Error(
                        message = AppError.Validation(ValidationError.NO_ACTIVE_SESSION).toUiText(),
                        canRetry = false,
                        canClose = true,
                    )
                is LoadQuestModeResult.Failure ->
                    _uiState.value = QuestModeUiState.Error(
                        message = result.error.toUiText(),
                        canRetry = true,
                        canClose = true,
                    )
            }
        }
    }

    /**
     * A missing/mismatched active session is not automatically "abandoned or lost" — it's also
     * exactly what a stale [QuestMode][com.togetherly.navigation.destination.RootDestination.QuestMode]
     * route looks like *after* [completeQuest] already succeeded (a double-navigation, a process
     * recreated post-completion, an old back-stack entry reopened). [resolveCompletionTransition]
     * distinguishes the two: if [completionId] already resolved to a real completion, this redirects
     * straight to its celebration instead of showing a dead-end error.
     */
    private suspend fun onNoActiveSession() {
        when (val transition = resolveCompletionTransition(completionId)) {
            is CompletionTransitionResult.AlreadyCompleted ->
                _events.send(QuestModeEvent.NavigateToCompletion(transition.completionId))
            CompletionTransitionResult.NotCompleted ->
                _uiState.value = QuestModeUiState.Error(
                    message = AppError.Validation(ValidationError.NO_ACTIVE_SESSION).toUiText(),
                    canRetry = false,
                    canClose = true,
                )
            is CompletionTransitionResult.Failure ->
                _uiState.value = QuestModeUiState.Error(
                    message = transition.error.toUiText(),
                    canRetry = true,
                    canClose = true,
                )
        }
    }

    private fun startCountdownIfNeeded(session: QuestModeSession) {
        if (countdownJob != null) return
        val timer = session.quest.timer ?: return
        countdownJob = viewModelScope.launch {
            countdownEngine.observe(session.activeSession, timer).collect { timerState ->
                updateContent { it.copy(quest = it.quest.copy(timer = timerState.toUi())) }
                if (timerState is QuestTimerState.Finished) {
                    emitTimerFinishedOnce()
                }
            }
        }
    }

    private fun maybeHandleFinishedOnLoad(timerState: QuestTimerState) {
        if (timerState is QuestTimerState.Finished) {
            emitTimerFinishedOnce()
        }
    }

    private fun emitTimerFinishedOnce() {
        if (timerFinishedEmitted) return
        timerFinishedEmitted = true
        viewModelScope.launch { _events.send(QuestModeEvent.TimerFinished) }
    }

    private fun onAbandonConfirmed() {
        if (isAbandoning) return
        val phoneDown = (_uiState.value as? QuestModeUiState.Content)?.phoneDown ?: false
        isAbandoning = true
        viewModelScope.launch {
            when (val result = abandonQuest()) {
                is DataResult.Success -> {
                    isAbandoning = false
                    currentQuest?.let { quest -> analytics.capture(QuestAbandoned(questId = quest.id, usedPhoneDown = phoneDown)) }
                    _events.send(QuestModeEvent.NavigateToToday)
                }
                is DataResult.Error -> {
                    isAbandoning = false
                    updateContent {
                        it.copy(showAbandonConfirmation = false, error = result.error.toUiText())
                    }
                }
            }
        }
    }

    private fun onCompleteClicked() {
        val content = _uiState.value as? QuestModeUiState.Content ?: return
        if (content.isCompleting || isCompletingGuard) return

        val usedTimer = content.quest.timer is QuestTimerUi.Finished
        val usedPhoneDown = content.phoneDown
        isCompletingGuard = true
        updateContent { it.copy(isCompleting = true, error = null) }
        viewModelScope.launch {
            when (val result = completeQuest()) {
                is DataResult.Success -> {
                    isCompletingGuard = false
                    updateContent { it.copy(isCompleting = false) }
                    currentQuest?.let { quest ->
                        analytics.capture(
                            QuestCompleted(
                                questId = quest.id,
                                category = quest.category,
                                durationBucket = durationBandFor(quest.durationMinutes),
                                energyLevel = quest.energy,
                                accessRequired = quest.access,
                                usedTimer = usedTimer,
                                usedPhoneDown = usedPhoneDown,
                            ),
                        )
                    }
                    customerAttributesRepository.markFirstQuestCompleted()
                    _events.send(QuestModeEvent.NavigateToCompletion(result.value.id))
                }
                is DataResult.Error -> {
                    isCompletingGuard = false
                    updateContent { it.copy(isCompleting = false, error = result.error.toUiText()) }
                }
            }
        }
    }

    private inline fun updateContent(transform: (QuestModeUiState.Content) -> QuestModeUiState.Content) {
        _uiState.update { current -> (current as? QuestModeUiState.Content)?.let(transform) ?: current }
    }
}

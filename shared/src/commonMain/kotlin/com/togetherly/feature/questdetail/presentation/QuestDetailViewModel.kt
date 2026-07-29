package com.togetherly.feature.questdetail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.datetime.AppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.PremiumContentUnlocked
import com.togetherly.core.telemetry.PremiumContentViewed
import com.togetherly.core.telemetry.ProductAnalytics
import com.togetherly.core.telemetry.QuestSaved
import com.togetherly.core.telemetry.QuestStarted
import com.togetherly.core.telemetry.QuestViewed
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.completion.usecase.PrepareQuestStartResult
import com.togetherly.domain.completion.usecase.PrepareQuestStartUseCase
import com.togetherly.domain.completion.usecase.ReplaceActiveQuestSession
import com.togetherly.domain.completion.usecase.StartQuest
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.EntitlementRepository
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.QuestRepository
import com.togetherly.domain.saved.repository.SavedQuestRepository
import com.togetherly.domain.saved.usecase.SetQuestSaved
import com.togetherly.feature.questdetail.mapper.toDetailUi
import com.togetherly.feature.questdetail.model.QuestDetailUiState
import com.togetherly.feature.questdetail.model.QuestOpenSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Quest Detail's single state machine for one fixed [questId] — [questId] is supplied at
 * construction (via Koin's parameterized `viewModel { params -> ... }`, not read from a
 * `SavedStateHandle` inside this class), so a distinct instance backs each distinct
 * [com.togetherly.navigation.destination.RootDestination.QuestDetail] the nav graph creates.
 *
 * [onStartClicked] never resolves domain dependencies itself when a conflict happens — it calls
 * [startQuest], and only on [ValidationError.ACTIVE_SESSION_CONFLICT] does it show the confirmation
 * (see [QuestDetailUiState.Content.activeSessionConflict]'s own KDoc). Confirming that dialog calls
 * [replaceActiveQuestSession] — a distinct use case, never a hidden "retry with force" branch
 * inside [startQuest] (see that use case's own KDoc for why).
 *
 * [prepareQuestStart] (Step 12.6) is checked *before* [startQuest] on every start attempt,
 * regardless of what [QuestDetailUiState.Content.locked] currently shows — that flag is a UX
 * nicety (skip the round trip for the common case), never the actual authorization ("Do not rely
 * only on a disabled button", this feature's own task spec). [startQuest] itself performs the same
 * check again as a non-bypassable safety net at the actual session-creation boundary.
 */
class QuestDetailViewModel(
    private val questId: QuestId,
    private val source: QuestOpenSource,
    private val questRepository: QuestRepository,
    private val savedQuestRepository: SavedQuestRepository,
    private val setQuestSaved: SetQuestSaved,
    private val startQuest: StartQuest,
    private val prepareQuestStart: PrepareQuestStartUseCase,
    private val replaceActiveQuestSession: ReplaceActiveQuestSession,
    private val entitlementRepository: EntitlementRepository,
    private val questAccessPolicy: QuestAccessPolicy,
    private val clock: AppClock,
    private val analytics: ProductAnalytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuestDetailUiState>(QuestDetailUiState.Loading)
    val uiState: StateFlow<QuestDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<QuestDetailEvent>(Channel.BUFFERED)
    val events: Flow<QuestDetailEvent> = _events.receiveAsFlow()

    private var hasStarted = false
    private var isSavingQuest = false

    /** Never exposed in [QuestDetailUiState] — kept only so the live [entitlementRepository] observer in [onScreenStarted] can re-map [QuestDetailUiState.Content.quest] (revealing/hiding instructions) without re-fetching. */
    private var currentQuest: FamilyQuest? = null
    private var currentPackTitle: String? = null

    fun onAction(action: QuestDetailAction) {
        when (action) {
            QuestDetailAction.RetryClicked -> load()
            QuestDetailAction.BackClicked -> viewModelScope.launch { _events.send(QuestDetailEvent.NavigateBack) }
            QuestDetailAction.SaveClicked -> onSaveClicked()
            QuestDetailAction.StartClicked -> onStartClicked()
            QuestDetailAction.UnlockClicked -> viewModelScope.launch { _events.send(QuestDetailEvent.OpenPaywall(questId)) }
            QuestDetailAction.ReplaceActiveSessionConfirmed -> onReplaceActiveSessionConfirmed()
            QuestDetailAction.KeepActiveSessionClicked -> updateContent { it.copy(activeSessionConflict = false) }
        }
    }

    fun onScreenStarted() {
        if (hasStarted) return
        hasStarted = true
        load()

        viewModelScope.launch {
            entitlementRepository.observeAccess().collect { result ->
                val quest = currentQuest ?: return@collect
                if (result !is DataResult.Success) return@collect
                val wasLocked = (_uiState.value as? QuestDetailUiState.Content)?.locked ?: false
                val locked = quest.access is QuestAccess.Premium && !questAccessPolicy.canAccess(quest.access, result.value, clock.now())
                updateContent { it.copy(locked = locked, quest = quest.toDetailUi(locked = locked, packTitle = currentPackTitle)) }
                if (wasLocked && !locked) {
                    analytics.capture(PremiumContentUnlocked(questId = questId, packId = null))
                }
            }
        }
    }

    private fun load() {
        _uiState.value = QuestDetailUiState.Loading
        viewModelScope.launch {
            val questResult = questRepository.getQuest(questId)
            val quest = when (questResult) {
                is DataResult.Error -> {
                    _uiState.value = QuestDetailUiState.Error(questResult.error.toUiText())
                    return@launch
                }
                is DataResult.Success -> questResult.value
            }
            if (quest == null) {
                _uiState.value = QuestDetailUiState.Error(
                    AppError.Content(ContentError.QUEST_NOT_FOUND).toUiText(),
                )
                return@launch
            }

            val packResult = questRepository.getPack(quest.packId)
            val packTitle = (packResult as? DataResult.Success)?.value?.title?.value

            val isSavedResult = savedQuestRepository.isSaved(questId)
            val isSaved = (isSavedResult as? DataResult.Success)?.value ?: false

            currentQuest = quest
            currentPackTitle = packTitle
            val accessSnapshot = currentAccessSnapshot()
            val locked = quest.access is QuestAccess.Premium && !questAccessPolicy.canAccess(quest.access, accessSnapshot, clock.now())

            _uiState.value = QuestDetailUiState.Content(
                quest = quest.toDetailUi(locked = locked, packTitle = packTitle),
                isSaved = isSaved,
                isStarting = false,
                locked = locked,
            )

            analytics.capture(QuestViewed(questId = questId, source = source, category = quest.category, accessRequired = quest.access))
            if (locked) analytics.capture(PremiumContentViewed(questId = questId, packId = null))
        }
    }

    /** A failed entitlement read must never accidentally unlock premium content — falls back to free/no-entitlements, never to "unknown means unlocked." */
    private suspend fun currentAccessSnapshot(): AccessSnapshot {
        val result = entitlementRepository.getAccess()
        return (result as? DataResult.Success)?.value ?: AccessSnapshot(FamilyAccess.free(), emptySet(), clock.now())
    }

    private fun onSaveClicked() {
        if (isSavingQuest) return
        val content = _uiState.value as? QuestDetailUiState.Content ?: return
        val desiredSaved = !content.isSaved

        isSavingQuest = true
        updateContent { it.copy(isSaved = desiredSaved) }
        viewModelScope.launch {
            val result = setQuestSaved(questId, desiredSaved)
            isSavingQuest = false
            if (result is DataResult.Error) {
                updateContent { it.copy(isSaved = !desiredSaved) }
            } else {
                analytics.capture(QuestSaved(questId, desiredSaved))
            }
        }
    }

    private fun onStartClicked() {
        val content = _uiState.value as? QuestDetailUiState.Content ?: return
        if (content.isStarting || content.locked) return

        updateContent { it.copy(isStarting = true, startError = null) }
        viewModelScope.launch {
            when (val prepareResult = prepareQuestStart(questId)) {
                is PrepareQuestStartResult.RequiresFamilyPlus -> {
                    updateContent { it.copy(isStarting = false, locked = true) }
                    _events.send(QuestDetailEvent.OpenPaywall(questId))
                }
                PrepareQuestStartResult.NotFound -> {
                    updateContent { it.copy(isStarting = false, startError = AppError.Content(ContentError.QUEST_NOT_FOUND).toUiText()) }
                }
                is PrepareQuestStartResult.Allowed -> attemptStart()
            }
        }
    }

    private suspend fun attemptStart() {
        when (val result = startQuest(questId)) {
            is DataResult.Success -> {
                updateContent { it.copy(isStarting = false) }
                captureQuestStarted()
                _events.send(QuestDetailEvent.NavigatedToQuestMode(result.value.completionId))
            }
            is DataResult.Error -> {
                if (result.error == AppError.Validation(ValidationError.ACTIVE_SESSION_CONFLICT)) {
                    updateContent { it.copy(isStarting = false, activeSessionConflict = true) }
                } else {
                    updateContent { it.copy(isStarting = false, startError = result.error.toUiText()) }
                }
            }
        }
    }

    private fun onReplaceActiveSessionConfirmed() {
        val content = _uiState.value as? QuestDetailUiState.Content ?: return
        if (content.isStarting) return

        updateContent { it.copy(isStarting = true, activeSessionConflict = false, startError = null) }
        viewModelScope.launch {
            when (val result = replaceActiveQuestSession(questId)) {
                is DataResult.Success -> {
                    updateContent { it.copy(isStarting = false) }
                    captureQuestStarted()
                    _events.send(QuestDetailEvent.NavigatedToQuestMode(result.value.completionId))
                }
                is DataResult.Error -> updateContent {
                    it.copy(isStarting = false, startError = result.error.toUiText())
                }
            }
        }
    }

    private fun captureQuestStarted() {
        currentQuest?.let { quest ->
            analytics.capture(QuestStarted(questId = questId, source = source, category = quest.category, accessRequired = quest.access))
        }
    }

    private inline fun updateContent(transform: (QuestDetailUiState.Content) -> QuestDetailUiState.Content) {
        _uiState.update { current -> (current as? QuestDetailUiState.Content)?.let(transform) ?: current }
    }
}

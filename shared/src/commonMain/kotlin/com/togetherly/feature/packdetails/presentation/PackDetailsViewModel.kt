package com.togetherly.feature.packdetails.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.PackViewed
import com.togetherly.core.telemetry.PremiumContentUnlocked
import com.togetherly.core.telemetry.PremiumContentViewed
import com.togetherly.core.telemetry.ProductAnalytics
import com.togetherly.core.telemetry.QuestSaved
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.explore.QuestPackDetail
import com.togetherly.domain.explore.usecase.EvaluatePackAccessUseCase
import com.togetherly.domain.explore.usecase.EvaluateQuestAccessUseCase
import com.togetherly.domain.explore.usecase.GetQuestPackUseCase
import com.togetherly.domain.explore.usecase.ObserveSavedQuestIdsUseCase
import com.togetherly.domain.explore.usecase.ToggleSavedQuestUseCase
import com.togetherly.domain.purchase.repository.EntitlementRepository
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.feature.explore.mapper.toExplorePackUi
import com.togetherly.feature.explore.mapper.toExploreQuestUi
import com.togetherly.feature.packdetails.model.ContentAccessState
import com.togetherly.feature.packdetails.model.PackDetailsUiState
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Same fixed-id-per-instance shape as [com.togetherly.feature.questdetail.presentation.QuestDetailViewModel]
 * (Koin `parametersOf(packId)`, never a `SavedStateHandle` read here). [currentDetail] mirrors that
 * class's own `currentQuestAccess` field: the *raw* domain result from [getQuestPack] is fetched
 * once in [load], then re-mapped into [PackDetailsUiState] every time the live
 * [EntitlementRepository.observeAccess]/[observeSavedQuestIds] collector in [onScreenStarted] fires
 * — a purchase completing on the paywall above this screen, or a save/unsave from Explore, both
 * reach here the same reactive way, no explicit refresh call needed.
 */
class PackDetailsViewModel(
    private val packId: QuestPackId,
    private val getQuestPack: GetQuestPackUseCase,
    private val observeSavedQuestIds: ObserveSavedQuestIdsUseCase,
    private val toggleSavedQuest: ToggleSavedQuestUseCase,
    private val evaluateQuestAccess: EvaluateQuestAccessUseCase,
    private val evaluatePackAccess: EvaluatePackAccessUseCase,
    private val entitlementRepository: EntitlementRepository,
    private val analytics: ProductAnalytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PackDetailsUiState.initial())
    val uiState: StateFlow<PackDetailsUiState> = _uiState.asStateFlow()

    private val _events = Channel<PackDetailsEvent>(Channel.BUFFERED)
    val events: Flow<PackDetailsEvent> = _events.receiveAsFlow()

    private var hasStarted = false

    /** Never exposed in [PackDetailsUiState] — kept only so the live reactive collector in [onScreenStarted] can recompute access/saved state without re-fetching the pack. */
    private var currentDetail: QuestPackDetail? = null

    fun onAction(action: PackDetailsAction) {
        when (action) {
            PackDetailsAction.BackClicked -> viewModelScope.launch { _events.send(PackDetailsEvent.NavigateBack) }
            is PackDetailsAction.QuestClicked -> viewModelScope.launch { _events.send(PackDetailsEvent.OpenQuestDetail(action.questId)) }
            is PackDetailsAction.SaveClicked -> viewModelScope.launch {
                val result = toggleSavedQuest(action.questId)
                if (result is DataResult.Success) {
                    analytics.capture(QuestSaved(action.questId, result.value))
                }
            }
            PackDetailsAction.UnlockClicked -> viewModelScope.launch { _events.send(PackDetailsEvent.OpenPaywall(packId)) }
            PackDetailsAction.RetryClicked -> load()
        }
    }

    fun onScreenStarted() {
        if (hasStarted) return
        hasStarted = true
        load()

        viewModelScope.launch {
            combine(entitlementRepository.observeAccess(), observeSavedQuestIds()) { _, savedIdsResult -> savedIdsResult }
                .collect { savedIdsResult ->
                    val detail = currentDetail ?: return@collect
                    val wasLocked = _uiState.value.accessState == ContentAccessState.LOCKED
                    applyDetail(detail, savedIdsResult)
                    if (wasLocked && _uiState.value.accessState != ContentAccessState.LOCKED) {
                        analytics.capture(PremiumContentUnlocked(questId = null, packId = packId))
                    }
                }
        }
    }

    private fun load() {
        _uiState.value = PackDetailsUiState.initial()
        viewModelScope.launch {
            when (val result = getQuestPack(packId)) {
                is DataResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.error.toUiText()) }
                is DataResult.Success -> {
                    val detail = result.value
                    if (detail == null) {
                        _uiState.update { it.copy(isLoading = false, error = AppError.Content(ContentError.PACK_NOT_FOUND).toUiText()) }
                        return@launch
                    }
                    currentDetail = detail
                    analytics.capture(PackViewed(packId = packId, accessRequired = detail.pack.access))
                    if (!evaluatePackAccess(detail.pack)) {
                        analytics.capture(PremiumContentViewed(questId = null, packId = packId))
                    }
                    applyDetail(detail, observeSavedQuestIds().first())
                }
            }
        }
    }

    private suspend fun applyDetail(detail: QuestPackDetail, savedIdsResult: DataResult<Set<QuestId>>) {
        val savedIds = (savedIdsResult as? DataResult.Success)?.value ?: emptySet()
        val packLocked = !evaluatePackAccess(detail.pack)
        val accessState = when {
            detail.pack.access is QuestAccess.Free -> ContentAccessState.FREE
            packLocked -> ContentAccessState.LOCKED
            else -> ContentAccessState.UNLOCKED
        }
        val questItems = detail.quests
            .map { quest -> quest.toExploreQuestUi(isSaved = quest.id in savedIds, locked = !evaluateQuestAccess(quest)) }
            .toPersistentList()

        _uiState.update {
            it.copy(
                isLoading = false,
                pack = detail.pack.toExplorePackUi(locked = packLocked, memberQuests = detail.quests),
                quests = questItems,
                accessState = accessState,
                error = null,
            )
        }
    }
}

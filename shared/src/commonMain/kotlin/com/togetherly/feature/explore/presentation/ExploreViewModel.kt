package com.togetherly.feature.explore.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.ExploreFiltered
import com.togetherly.core.telemetry.ExploreSearched
import com.togetherly.core.telemetry.ProductAnalytics
import com.togetherly.core.telemetry.QuestSaved
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.explore.ExploreCatalogue
import com.togetherly.domain.explore.ExploreFilters
import com.togetherly.domain.explore.usecase.EvaluatePackAccessUseCase
import com.togetherly.domain.explore.usecase.EvaluateQuestAccessUseCase
import com.togetherly.domain.explore.usecase.FilterQuestsUseCase
import com.togetherly.domain.explore.usecase.ObserveExploreCatalogueUseCase
import com.togetherly.domain.explore.usecase.ObserveSavedQuestIdsUseCase
import com.togetherly.domain.explore.usecase.SearchQuestsUseCase
import com.togetherly.domain.explore.usecase.ToggleSavedQuestUseCase
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.repository.EntitlementRepository
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPack
import com.togetherly.feature.explore.mapper.toDomain
import com.togetherly.feature.explore.mapper.toExplorePackUi
import com.togetherly.feature.explore.mapper.toExploreQuestUi
import com.togetherly.feature.explore.model.ExploreEmptyState
import com.togetherly.feature.explore.model.ExplorePackUiModel
import com.togetherly.feature.explore.model.ExploreUiState
import com.togetherly.feature.today.mapper.toUi
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Composes the `domain.explore.usecase` use cases into the real, designed Explore home screen
 * (Step 12.3, filters wired for real in Step 12.4) — [ExploreScreen] renders whatever this
 * produces, never talking to a use case itself.
 *
 * Search is debounced (see [debouncedSearchQuery]) but the *text field itself* is not: every
 * keystroke updates [_uiState]'s `searchQuery`/`isSearchActive` immediately in [onAction] (so
 * typing never feels laggy), while the actual quest/pack recompute only runs [SEARCH_DEBOUNCE_MILLIS]
 * after typing settles, and [distinctUntilChanged] skips a recompute entirely when debouncing lands
 * on a query identical to the last one already applied (e.g. type-then-backspace-to-the-same-text).
 * By the time the debounced pipeline's result lands, [_uiState]'s `searchQuery` already matches it,
 * so applying it never causes the visible text to jump.
 *
 * [ExploreFilterStore.filters] is the *committed* filter state (Step 12.4) — the same instance
 * [ExploreFiltersViewModel] writes to on Apply. The quick top-row category chip is just a fast path
 * onto the same store (`filters.category`), not a separate field, so it and the full filter sheet's
 * own Category group always agree. "Search and category selection work together" the same way
 * search combines with every other filter dimension — all through [FilterQuestsUseCase].
 *
 * Packs are never *filtered* by [ExploreFilters] (most bundled packs mix categories on purpose —
 * see [com.togetherly.domain.quest.QuestPack.category]'s own nullability), but *are* matched by a
 * plain title/description text search while [ExploreUiState.isSearchActive] — this feature's own
 * task spec is explicit ("When a query is active, show matching quests and packs"), and no existing
 * domain use case does pack text-search, so it happens locally here rather than growing a new
 * `domain.explore.usecase` for a single presentation-layer feature.
 *
 * [retrySignal] mirrors [com.togetherly.navigation.state.BootstrapViewModel]'s own established
 * retry pattern: a failed catalogue load doesn't resume on its own, so [ExploreAction.RetryClicked]
 * re-subscribes a fresh call via [retrySignal] rather than assuming the existing Flow will retry.
 */
class ExploreViewModel(
    private val observeExploreCatalogue: ObserveExploreCatalogueUseCase,
    private val searchQuests: SearchQuestsUseCase,
    private val filterQuests: FilterQuestsUseCase,
    private val observeSavedQuestIds: ObserveSavedQuestIdsUseCase,
    private val toggleSavedQuest: ToggleSavedQuestUseCase,
    private val evaluateQuestAccess: EvaluateQuestAccessUseCase,
    private val evaluatePackAccess: EvaluatePackAccessUseCase,
    private val entitlementRepository: EntitlementRepository,
    private val filterStore: ExploreFilterStore,
    private val analytics: ProductAnalytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState.initial())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val _events = Channel<ExploreEvent>(Channel.BUFFERED)
    val events: Flow<ExploreEvent> = _events.receiveAsFlow()

    private val _searchQuery = MutableStateFlow("")
    private val retrySignal = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    /**
     * [Flow.debounce] delays *every* emission, including the very first one — left alone, that
     * would stall the initial catalogue load behind an artificial [SEARCH_DEBOUNCE_MILLIS] wait
     * nobody's typing caused. [_searchQuery.take(1)][kotlinx.coroutines.flow.take] passes the
     * starting `""` through immediately; only changes after that (`drop(1)`) are debounced.
     */
    @OptIn(FlowPreview::class)
    private val debouncedSearchQuery: Flow<String> = merge(
        _searchQuery.take(1),
        _searchQuery.drop(1).debounce(SEARCH_DEBOUNCE_MILLIS),
    ).distinctUntilChanged()

    private var hasStarted = false
    private var searchWasActive = false

    fun onAction(action: ExploreAction) {
        when (action) {
            is ExploreAction.SearchChanged -> {
                _searchQuery.value = action.value
                _uiState.update { it.copy(searchQuery = action.value, isSearchActive = action.value.isNotBlank()) }
            }
            ExploreAction.SearchCleared -> {
                _searchQuery.value = ""
                _uiState.update { it.copy(searchQuery = "", isSearchActive = false) }
            }
            is ExploreAction.CategorySelected -> {
                val current = filterStore.filters.value
                val selectedDomain = action.category.toDomain()
                val nextCategory = if (current.category == selectedDomain) null else selectedDomain
                val next = current.copy(category = nextCategory)
                filterStore.commit(next)
                captureExploreFiltered(next)
            }
            ExploreAction.CategoryCleared -> {
                val next = filterStore.filters.value.copy(category = null)
                filterStore.commit(next)
                captureExploreFiltered(next)
            }
            is ExploreAction.QuestClicked -> viewModelScope.launch { _events.send(ExploreEvent.OpenQuestDetail(action.questId)) }
            is ExploreAction.PackClicked -> viewModelScope.launch { _events.send(ExploreEvent.OpenPackDetails(action.packId)) }
            is ExploreAction.SaveClicked -> viewModelScope.launch {
                val result = toggleSavedQuest(action.questId)
                if (result is DataResult.Success) {
                    analytics.capture(QuestSaved(action.questId, result.value))
                }
            }
            ExploreAction.FiltersClicked -> viewModelScope.launch { _events.send(ExploreEvent.OpenFilters) }
            ExploreAction.SavedClicked -> viewModelScope.launch { _events.send(ExploreEvent.OpenSaved) }
            ExploreAction.RetryClicked -> {
                _uiState.update { it.copy(isLoading = true, error = null) }
                retrySignal.tryEmit(Unit)
            }
        }
    }

    private fun captureExploreFiltered(filters: ExploreFilters) {
        analytics.capture(
            ExploreFiltered(
                category = filters.category,
                durationBucket = filters.duration,
                energyLevel = filters.energy,
                location = filters.location,
                accessFilter = filters.access,
            ),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun onScreenStarted() {
        if (hasStarted) return
        hasStarted = true
        analytics.screen(AnalyticsScreen.EXPLORE)

        viewModelScope.launch {
            retrySignal.flatMapLatest {
                combine(
                    observeExploreCatalogue(),
                    entitlementRepository.observeAccess(),
                    observeSavedQuestIds(),
                    debouncedSearchQuery,
                    filterStore.filters,
                ) { catalogueResult, accessResult, savedIdsResult, query, filters ->
                    ExploreCombinedInput(catalogueResult, accessResult, savedIdsResult, query, filters)
                }
            }.collect { input -> applyInput(input) }
        }
    }

    private suspend fun applyInput(input: ExploreCombinedInput) {
        val catalogueResult = input.catalogueResult
        if (catalogueResult is DataResult.Error) {
            _uiState.update { it.copy(isLoading = false, error = catalogueResult.error.toUiText()) }
            return
        }
        val catalogue = (catalogueResult as DataResult.Success).value
        val access = (input.accessResult as? DataResult.Success)?.value?.familyAccess ?: FamilyAccess.free()
        val savedIds = (input.savedIdsResult as? DataResult.Success)?.value ?: emptySet()

        val isSearchActive = input.query.isNotBlank()
        if (isSearchActive && !searchWasActive) {
            analytics.capture(ExploreSearched)
        }
        searchWasActive = isSearchActive
        val filters = input.filters

        val filtered = filterQuests(catalogue.quests, filters)
        val visibleQuests = if (isSearchActive) searchQuests(filtered, input.query) else filtered
        val questItems = visibleQuests
            .map { quest -> quest.toExploreQuestUi(isSaved = quest.id in savedIds, locked = !evaluateQuestAccess(quest)) }
            .let { if (isSearchActive) it else it.take(SUGGESTED_QUEST_CAP) }
            .toPersistentList()

        val questsById = catalogue.quests.associateBy { it.id }
        val allPackItems = catalogue.packs
            .sortedBy { it.sortOrder }
            .map { pack -> pack.toExplorePackUi(locked = !evaluatePackAccess(pack), memberQuests = memberQuestsOf(pack, questsById)) }

        val visiblePackItems = if (isSearchActive) matchingPacks(allPackItems, input.query) else allPackItems
        val featured = if (isSearchActive) persistentListOf() else allPackItems.take(FEATURED_PACK_COUNT).toPersistentList()

        val emptyState = when {
            isSearchActive && questItems.isEmpty() && visiblePackItems.isEmpty() -> ExploreEmptyState.SEARCH
            !isSearchActive && !filters.isEmpty && questItems.isEmpty() -> ExploreEmptyState.FILTER
            else -> null
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                searchQuery = input.query,
                selectedCategory = filters.category?.toUi(),
                activeFilterCount = filters.activeCount,
                featuredPacks = featured,
                packs = visiblePackItems.toPersistentList(),
                quests = questItems,
                access = access,
                isSearchActive = isSearchActive,
                emptyState = emptyState,
                error = null,
            )
        }
    }

    private fun memberQuestsOf(pack: QuestPack, questsById: Map<QuestId, FamilyQuest>): List<FamilyQuest> =
        pack.questIds.mapNotNull { questsById[it] }

    private fun matchingPacks(packs: List<ExplorePackUiModel>, query: String): List<ExplorePackUiModel> {
        val needle = query.trim().lowercase()
        return packs.filter { it.title.lowercase().contains(needle) || it.description.lowercase().contains(needle) }
    }

    private data class ExploreCombinedInput(
        val catalogueResult: DataResult<ExploreCatalogue>,
        val accessResult: DataResult<AccessSnapshot>,
        val savedIdsResult: DataResult<Set<QuestId>>,
        val query: String,
        val filters: ExploreFilters,
    )

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 300L
        const val FEATURED_PACK_COUNT = 3
        const val SUGGESTED_QUEST_CAP = 12
    }
}

package com.togetherly.feature.today.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.datetime.AppClock
import com.togetherly.core.datetime.AppTimeZoneProvider
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.DailyQuestRerollBlocked
import com.togetherly.core.telemetry.DailyQuestRerolled
import com.togetherly.core.telemetry.DailyQuestRevealed
import com.togetherly.core.telemetry.ProductAnalytics
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.completion.repository.CompletionRepository
import com.togetherly.domain.daily.QuestContext
import com.togetherly.domain.daily.usecase.GetOrSelectDailyQuest
import com.togetherly.domain.daily.usecase.RerollDailyQuest
import com.togetherly.domain.daily.usecase.ResolvedDailyQuest
import com.togetherly.domain.daily.usecase.SelectDailyQuestForContext
import com.togetherly.domain.family.repository.FamilyRepository
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.recommendation.durationBandFor
import com.togetherly.domain.saved.repository.SavedQuestRepository
import com.togetherly.domain.saved.usecase.SetQuestSaved
import kotlinx.datetime.LocalDate
import com.togetherly.feature.today.mapper.greetingFor
import com.togetherly.feature.today.mapper.toCardUi
import com.togetherly.feature.today.mapper.toTodayUiText
import com.togetherly.feature.today.model.QuestCardUi
import com.togetherly.feature.today.model.TodayContentState
import com.togetherly.feature.today.model.TodayFilterState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime

private val EMPTY_CONTEXT = QuestContext(null, null, null, null, null)

/**
 * Today's single state machine — [uiState] is the whole snapshot, [onAction] is the only way in,
 * [events] is strictly for one-off effects. No Composable API, `NavController`, or Koin lookup
 * appears here — same boundary [com.togetherly.feature.onboarding.presentation.OnboardingViewModel]
 * maintains.
 *
 * [savedQuestRepository] is queried directly (not just [setQuestSaved]) because Today needs to know
 * whether the *currently resolved* quest is already saved — something none of the daily-selection
 * use cases return.
 *
 * [TodayAction.ScreenStarted] is idempotent: [hasStarted] guards against Compose recomposition (or
 * a re-entered Route) re-triggering the family observer and the initial load a second time.
 */
class TodayViewModel(
    private val getOrSelectDailyQuest: GetOrSelectDailyQuest,
    private val selectDailyQuestForContext: SelectDailyQuestForContext,
    private val rerollDailyQuest: RerollDailyQuest,
    private val setQuestSaved: SetQuestSaved,
    private val savedQuestRepository: SavedQuestRepository,
    private val completionRepository: CompletionRepository,
    private val familyRepository: FamilyRepository,
    private val clock: AppClock,
    private val timeZoneProvider: AppTimeZoneProvider,
    private val analytics: ProductAnalytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TodayUiState(
            greeting = greetingFor(clock.now().toLocalDateTime(timeZoneProvider.current()).time),
            familyName = null,
        ),
    )
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private val _events = Channel<TodayEvent>(Channel.BUFFERED)
    val events: Flow<TodayEvent> = _events.receiveAsFlow()

    private var hasStarted = false
    private var filtersBaseline = TodayFilterState()
    private var isSavingQuest = false

    /** The raw quest behind the currently shown card — [QuestCardUi] only carries localized labels, never the stable enums analytics needs. */
    private var currentQuest: FamilyQuest? = null

    fun onAction(action: TodayAction) {
        when (action) {
            TodayAction.ScreenStarted -> onScreenStarted()
            TodayAction.RetryClicked -> loadDailyQuest()
            TodayAction.RevealClicked -> onReveal()
            TodayAction.FiltersClicked -> onFiltersOpened()
            TodayAction.FiltersDismissed -> onFiltersDismissed()
            is TodayAction.DurationFilterChanged -> updateFilters { it.copy(duration = action.value) }
            is TodayAction.LocationFilterChanged -> updateFilters { it.copy(location = action.value) }
            is TodayAction.EnergyFilterChanged -> updateFilters { it.copy(energy = action.value) }
            is TodayAction.PreparationFilterChanged -> updateFilters { it.copy(preparation = action.value) }
            is TodayAction.CategoryFilterChanged -> updateFilters { it.copy(category = action.value) }
            TodayAction.ClearFiltersClicked -> updateFilters { TodayFilterState() }
            TodayAction.ApplyFiltersClicked -> onApplyFilters()
            TodayAction.RerollClicked -> onRerollClicked()
            TodayAction.RerollConfirmed -> onRerollConfirmed()
            TodayAction.RerollCancelled -> _uiState.update { it.copy(rerollConfirmationVisible = false) }
            TodayAction.SaveClicked -> onSaveClicked()
            TodayAction.StartClicked -> onStartClicked()
            TodayAction.TransientErrorDismissed -> _uiState.update { it.copy(transientError = null) }
        }
    }

    private fun onScreenStarted() {
        if (hasStarted) return
        hasStarted = true
        analytics.screen(AnalyticsScreen.TODAY)

        viewModelScope.launch {
            familyRepository.observeProfile().collect { result ->
                val name = (result as? DataResult.Success)?.value?.displayName?.value
                _uiState.update { it.copy(familyName = name) }
            }
        }

        loadDailyQuest()
    }

    private fun loadDailyQuest() {
        _uiState.update { it.copy(content = TodayContentState.Loading) }
        viewModelScope.launch {
            val result = getOrSelectDailyQuest(context = EMPTY_CONTEXT, timeZone = timeZoneProvider.current())
            applyResolution(result, resetToMystery = true)
        }
    }

    private fun onReveal() {
        val mystery = _uiState.value.content as? TodayContentState.Mystery ?: return
        _uiState.update { it.copy(content = TodayContentState.Revealed(mystery.quest)) }
        currentQuest?.let { quest ->
            analytics.capture(
                DailyQuestRevealed(
                    category = quest.category,
                    durationBucket = durationBandFor(quest.durationMinutes),
                    energyLevel = quest.energy,
                    accessRequired = quest.access,
                ),
            )
        }
    }

    private fun onFiltersOpened() {
        filtersBaseline = _uiState.value.filters
        _uiState.update { it.copy(filtersVisible = true) }
    }

    private fun onFiltersDismissed() {
        _uiState.update { it.copy(filtersVisible = false, filters = filtersBaseline) }
    }

    private inline fun updateFilters(transform: (TodayFilterState) -> TodayFilterState) {
        _uiState.update { it.copy(filters = transform(it.filters)) }
    }

    private fun onApplyFilters() {
        val current = _uiState.value
        if (current.isApplyingFilters) return

        _uiState.update { it.copy(isApplyingFilters = true) }
        viewModelScope.launch {
            val context = current.filters.toQuestContext()
            val result = selectDailyQuestForContext(context = context, timeZone = timeZoneProvider.current())
            when (result) {
                is DataResult.Success -> {
                    val card = result.value.toCardUiWithSavedState()
                    currentQuest = result.value.quest
                    _uiState.update {
                        it.copy(
                            content = TodayContentState.Mystery(card),
                            rerollsRemaining = result.value.rerollAllowance.remaining,
                            isApplyingFilters = false,
                            filtersVisible = false,
                            transientError = null,
                        )
                    }
                }
                is DataResult.Error -> _uiState.update {
                    it.copy(isApplyingFilters = false, transientError = result.error.toTodayUiText())
                }
            }
        }
    }

    /**
     * Requesting a reroll never calls [rerollDailyQuest] directly — confirmation must come first
     * (see [TodayUiState.rerollConfirmationVisible]'s own KDoc), and the free swap is only ever
     * consumed once the user actually confirms ([onRerollConfirmed]). When no allowance remains,
     * there is nothing to confirm — this goes straight to [TodayEvent.RerollLimitReached] instead
     * of opening a dialog for an action that can't succeed.
     */
    private fun onRerollClicked() {
        if (_uiState.value.rerollsRemaining == 0) {
            analytics.capture(DailyQuestRerollBlocked)
            viewModelScope.launch { _events.send(TodayEvent.RerollLimitReached) }
            return
        }
        _uiState.update { it.copy(rerollConfirmationVisible = true) }
    }

    private fun onRerollConfirmed() {
        val current = _uiState.value
        if (current.isRerolling) return

        _uiState.update { it.copy(isRerolling = true, rerollConfirmationVisible = false) }
        viewModelScope.launch {
            val result = rerollDailyQuest(timeZone = timeZoneProvider.current())
            when (result) {
                is DataResult.Success -> {
                    val card = result.value.toCardUiWithSavedState()
                    currentQuest = result.value.quest
                    _uiState.update {
                        it.copy(
                            content = TodayContentState.Mystery(card),
                            rerollsRemaining = result.value.rerollAllowance.remaining,
                            isRerolling = false,
                            transientError = null,
                        )
                    }
                    analytics.capture(
                        DailyQuestRerolled(
                            category = result.value.quest.category,
                            durationBucket = durationBandFor(result.value.quest.durationMinutes),
                            energyLevel = result.value.quest.energy,
                            accessRequired = result.value.quest.access,
                        ),
                    )
                }
                is DataResult.Error -> {
                    _uiState.update { it.copy(isRerolling = false) }
                    if (result.error == AppError.Validation(ValidationError.REROLL_LIMIT_REACHED)) {
                        analytics.capture(DailyQuestRerollBlocked)
                        _events.send(TodayEvent.RerollLimitReached)
                    } else {
                        _uiState.update { it.copy(transientError = result.error.toTodayUiText()) }
                    }
                }
            }
        }
    }

    private fun onSaveClicked() {
        if (isSavingQuest) return
        val quest = _uiState.value.content.currentQuest() ?: return
        val desiredSaved = !quest.isSaved

        isSavingQuest = true
        replaceCurrentQuestCard(quest.copy(isSaved = desiredSaved))
        viewModelScope.launch {
            val result = setQuestSaved(quest.id, desiredSaved)
            isSavingQuest = false
            if (result is DataResult.Error) {
                replaceCurrentQuestCard(quest)
                _uiState.update { it.copy(transientError = result.error.toUiText()) }
            }
        }
    }

    private fun onStartClicked() {
        val quest = _uiState.value.content.currentQuest() ?: return
        viewModelScope.launch { _events.send(TodayEvent.OpenQuestDetail(quest.id)) }
    }

    private suspend fun applyResolution(
        result: DataResult<ResolvedDailyQuest>,
        resetToMystery: Boolean,
    ) {
        when (result) {
            is DataResult.Success -> {
                val card = result.value.toCardUiWithSavedState()
                currentQuest = result.value.quest
                val alreadyCompleted = isCompletedToday(result.value.dailyQuest.questId, result.value.dailyQuest.localDate)
                _uiState.update {
                    it.copy(
                        content = when {
                            alreadyCompleted -> TodayContentState.Completed(card)
                            resetToMystery -> TodayContentState.Mystery(card)
                            else -> it.content
                        },
                        rerollsRemaining = result.value.rerollAllowance.remaining,
                        transientError = null,
                    )
                }
            }
            is DataResult.Error -> _uiState.update {
                it.copy(content = TodayContentState.Error(message = result.error.toUiText(), retryAvailable = true))
            }
        }
    }

    /**
     * Neither [DailyQuest][com.togetherly.domain.daily.DailyQuest] nor
     * [ResolvedDailyQuest] carries a completion linkage, so Today cross-references
     * [completionRepository] itself — matching by [QuestId] *and* the family's local date
     * (mirroring [com.togetherly.domain.journey.JourneySummary]'s own `completedAt`-to-local-date
     * derivation), since the same quest can recur on a later day once its own cooldown elapses and
     * that later selection is not "already completed."
     */
    private suspend fun isCompletedToday(questId: QuestId, localDate: LocalDate): Boolean {
        val completionsResult = completionRepository.getCompletions()
        val completions = (completionsResult as? DataResult.Success)?.value ?: return false
        val timeZone = timeZoneProvider.current()
        return completions.any { completion ->
            completion.questId == questId && completion.completedAt.toLocalDateTime(timeZone).date == localDate
        }
    }

    private suspend fun ResolvedDailyQuest.toCardUiWithSavedState(): QuestCardUi {
        val isSavedResult = savedQuestRepository.isSaved(quest.id)
        val isSaved = (isSavedResult as? DataResult.Success)?.value ?: false
        return quest.toCardUi(isSaved = isSaved)
    }

    private fun replaceCurrentQuestCard(card: QuestCardUi) {
        _uiState.update { current ->
            when (val content = current.content) {
                is TodayContentState.Mystery -> current.copy(content = content.copy(quest = card))
                is TodayContentState.Revealed -> current.copy(content = content.copy(quest = card))
                is TodayContentState.Completed -> current.copy(content = content.copy(quest = card))
                else -> current
            }
        }
    }
}

private fun TodayContentState.currentQuest(): QuestCardUi? = when (this) {
    is TodayContentState.Mystery -> quest
    is TodayContentState.Revealed -> quest
    is TodayContentState.Completed -> quest
    else -> null
}

private fun TodayFilterState.toQuestContext(): QuestContext = QuestContext(
    duration = duration,
    location = location,
    energy = energy,
    preparation = preparation,
    preferredCategory = category,
)

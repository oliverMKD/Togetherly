package com.togetherly.feature.today.preview

import com.togetherly.core.ui.UiText
import com.togetherly.domain.quest.QuestId
import com.togetherly.feature.today.mapper.label
import com.togetherly.feature.today.model.QuestCardUi
import com.togetherly.feature.today.model.QuestCategoryUi
import com.togetherly.feature.today.model.TodayContentState
import com.togetherly.feature.today.model.TodayFilterState
import com.togetherly.feature.today.presentation.TodayUiState
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.today_greeting_morning
import togetherly.shared.generated.resources.today_material_summary

/**
 * A living catalogue of Today's UI states, for the Composable previews Step 8.4 adds — never a
 * substitute for [com.togetherly.feature.today.presentation.TodayViewModelTest], which proves
 * behavior; these only prove appearance. Every fixture is fictional sample data, matching this
 * project's privacy audit requirement that previews/screenshots never carry real data.
 */

private val sampleGreeting = UiText.Resource(Res.string.today_greeting_morning)

fun sampleQuestCard(
    isSaved: Boolean = false,
    hasMaterials: Boolean = true,
) = QuestCardUi(
    id = QuestId("preview-quest"),
    title = "Backyard Scavenger Hunt",
    summary = "Find five hidden treasures together before the timer runs out.",
    category = QuestCategoryUi.DISCOVER,
    durationLabel = com.togetherly.domain.family.DurationBand.TWENTY_MINUTES.label(),
    locationLabel = com.togetherly.domain.quest.QuestLocation.OUTDOOR.label(),
    preparationLabel = com.togetherly.domain.quest.PreparationLevel.SIMPLE_MATERIALS.label(),
    energyLabel = com.togetherly.domain.quest.EnergyLevel.MODERATE.label(),
    materialSummary = if (hasMaterials) UiText.Resource(Res.string.today_material_summary, listOf(3)) else null,
    isSaved = isSaved,
)

fun loadingTodayUiState() = TodayUiState(
    greeting = sampleGreeting,
    familyName = "The Firefly Family",
    content = TodayContentState.Loading,
)

fun mysteryTodayUiState() = TodayUiState(
    greeting = sampleGreeting,
    familyName = "The Firefly Family",
    content = TodayContentState.Mystery(sampleQuestCard()),
    rerollsRemaining = 1,
)

fun revealedTodayUiState(isSaved: Boolean = false, hasMaterials: Boolean = true) = TodayUiState(
    greeting = sampleGreeting,
    familyName = "The Firefly Family",
    content = TodayContentState.Revealed(sampleQuestCard(isSaved = isSaved, hasMaterials = hasMaterials)),
    rerollsRemaining = 1,
)

fun filtersActiveTodayUiState() = revealedTodayUiState().copy(
    filters = TodayFilterState(duration = com.togetherly.domain.family.DurationBand.FIVE_MINUTES),
    filtersVisible = true,
)

fun noActiveFiltersTodayUiState() = revealedTodayUiState().copy(
    filters = TodayFilterState(),
    filtersVisible = true,
)

fun multipleActiveFiltersTodayUiState() = revealedTodayUiState().copy(
    filters = TodayFilterState(
        duration = com.togetherly.domain.family.DurationBand.TEN_MINUTES,
        location = com.togetherly.domain.quest.QuestLocation.OUTDOOR,
        energy = com.togetherly.domain.quest.EnergyLevel.ACTIVE,
    ),
    filtersVisible = true,
)

fun applyingFiltersTodayUiState() = filtersActiveTodayUiState().copy(isApplyingFilters = true)

fun filterNoMatchErrorTodayUiState() = revealedTodayUiState().copy(
    transientError = UiText.Dynamic("Try removing one of your filters."),
)

fun rerollConfirmationTodayUiState() = revealedTodayUiState().copy(rerollConfirmationVisible = true)

fun rerollLoadingTodayUiState() = revealedTodayUiState().copy(isRerolling = true)

fun freeRerollConsumedTodayUiState() = revealedTodayUiState().copy(rerollsRemaining = 0)

fun errorTodayUiState() = TodayUiState(
    greeting = sampleGreeting,
    familyName = null,
    content = TodayContentState.Error(
        message = UiText.Dynamic("Something went wrong. Please try again."),
        retryAvailable = true,
    ),
)

package com.togetherly.feature.explore.presentation

import com.togetherly.domain.explore.QuestAccessFilter
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestLocation

/**
 * Every `*Changed` action carries the *new* value for its own group only — each is single-select
 * (matching [com.togetherly.domain.explore.ExploreFilters]'s own single-valued fields), so there is
 * no separate "cleared" action per group the way [ExploreAction.CategoryCleared] exists on the home
 * screen: [ExploreFiltersScreen] re-dispatches the same `*Changed(null)` when the already-selected
 * chip is tapped again (see that screen's own chip-group helper).
 */
sealed interface ExploreFiltersAction {
    data class DurationChanged(val value: DurationBand?) : ExploreFiltersAction
    data class EnergyChanged(val value: EnergyLevel?) : ExploreFiltersAction
    data class LocationChanged(val value: QuestLocation?) : ExploreFiltersAction
    data class AgeBandChanged(val value: AgeBand?) : ExploreFiltersAction
    data class CategoryChanged(val value: QuestCategory?) : ExploreFiltersAction
    data class AccessChanged(val value: QuestAccessFilter) : ExploreFiltersAction
    data object ClearAllClicked : ExploreFiltersAction
    data object ApplyClicked : ExploreFiltersAction
    data object CancelClicked : ExploreFiltersAction
}

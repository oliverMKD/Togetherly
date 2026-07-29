package com.togetherly.feature.explore.model

import androidx.compose.runtime.Immutable
import com.togetherly.domain.explore.ExploreFilters

/**
 * [draft] is edited freely by every chip tap in [com.togetherly.feature.explore.presentation.ExploreFiltersScreen] —
 * nothing outside this screen (search results, [com.togetherly.feature.explore.presentation.ExploreFilterStore])
 * observes it until [com.togetherly.feature.explore.presentation.ExploreFiltersAction.ApplyClicked]
 * commits it. [draft] starts as a copy of [ExploreFilterStore][com.togetherly.feature.explore.presentation.ExploreFilterStore]'s
 * current committed value (read once when [com.togetherly.feature.explore.presentation.ExploreFiltersViewModel]
 * is constructed) — every other action here only ever mutates this local copy.
 */
@Immutable
data class ExploreFiltersUiState(
    val draft: ExploreFilters,
)

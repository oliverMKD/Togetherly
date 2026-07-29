package com.togetherly.feature.explore.presentation

/**
 * One event, fired by both [ExploreFiltersAction.ApplyClicked] (after committing the draft to
 * [ExploreFilterStore]) and [ExploreFiltersAction.CancelClicked] (without committing anything) —
 * the nav layer treats both the same way (pop back to Explore); only this ViewModel needs to know
 * whether a commit happened.
 */
sealed interface ExploreFiltersEvent {
    data object NavigateBack : ExploreFiltersEvent
}

package com.togetherly.feature.explore.presentation

import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.feature.today.model.QuestCategoryUi

/**
 * [CategoryCleared] has no direct counterpart in the "such as" contract this feature's own task
 * spec sketches — it's the natural pair to [CategorySelected] the same way [SearchCleared] pairs
 * with [SearchChanged], needed so the top-row "All" chip (deselecting whichever category chip is
 * currently active) has an action to dispatch. [QuestClicked]/[PackClicked] fire the same way
 * whether the target is locked or not — [com.togetherly.feature.questdetail.presentation.QuestDetailViewModel]
 * and (future) Pack Details own deciding what a locked target actually shows.
 */
sealed interface ExploreAction {
    data class SearchChanged(val value: String) : ExploreAction
    data object SearchCleared : ExploreAction
    data class CategorySelected(val category: QuestCategoryUi) : ExploreAction
    data object CategoryCleared : ExploreAction
    data class PackClicked(val packId: QuestPackId) : ExploreAction
    data class QuestClicked(val questId: QuestId) : ExploreAction
    data class SaveClicked(val questId: QuestId) : ExploreAction
    data object FiltersClicked : ExploreAction
    data object SavedClicked : ExploreAction
    data object RetryClicked : ExploreAction
}

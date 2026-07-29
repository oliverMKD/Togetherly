package com.togetherly.feature.saved.presentation

import com.togetherly.domain.quest.QuestId

/**
 * [SaveClicked] always means "unsave" here (every quest on this screen already has [com.togetherly.feature.explore.model.ExploreQuestUiModel.isSaved]
 * `true`) — same action name as [com.togetherly.feature.explore.presentation.ExploreAction.SaveClicked]
 * since it's the same toggle, just always starting from the saved side.
 */
sealed interface SavedAction {
    data class QuestClicked(val questId: QuestId) : SavedAction
    data class SaveClicked(val questId: QuestId) : SavedAction
    data object BackClicked : SavedAction
    data object RetryClicked : SavedAction
}

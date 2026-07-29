package com.togetherly.feature.questdetail.presentation

import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.quest.QuestId

/**
 * One-off effects only — never a substitute for a state field. Navigation only ever passes typed
 * IDs (see [com.togetherly.navigation.destination.RootDestination.QuestMode]'s own KDoc) — never
 * an [com.togetherly.domain.completion.ActiveQuestSession] or
 * [com.togetherly.domain.quest.FamilyQuest].
 */
sealed interface QuestDetailEvent {
    data class NavigatedToQuestMode(val completionId: CompletionId) : QuestDetailEvent
    data object NavigateBack : QuestDetailEvent
    data class OpenPaywall(val questId: QuestId) : QuestDetailEvent
}

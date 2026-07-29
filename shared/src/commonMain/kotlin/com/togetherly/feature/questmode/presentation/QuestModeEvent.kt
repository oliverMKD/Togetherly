package com.togetherly.feature.questmode.presentation

import com.togetherly.domain.completion.CompletionId

/**
 * [TimerFinished] triggers platform feedback (Step 9.5) but never creates a completion on its own
 * — see [com.togetherly.domain.questmode.QuestTimerState.Finished]'s own KDoc.
 */
sealed interface QuestModeEvent {
    data object NavigateBack : QuestModeEvent
    data object NavigateToToday : QuestModeEvent
    data class NavigateToCompletion(val completionId: CompletionId) : QuestModeEvent
    data object TimerFinished : QuestModeEvent
}

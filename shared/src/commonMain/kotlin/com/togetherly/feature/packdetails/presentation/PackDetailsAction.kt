package com.togetherly.feature.packdetails.presentation

import com.togetherly.domain.quest.QuestId

/**
 * No dedicated "choose a quest" action — the primary action for a free/unlocked pack dispatches
 * the same [QuestClicked] a tap on any quest card in the list would, targeting the pack's first
 * quest, rather than growing a parallel action for what is functionally identical navigation.
 */
sealed interface PackDetailsAction {
    data object BackClicked : PackDetailsAction
    data class QuestClicked(val questId: QuestId) : PackDetailsAction
    data class SaveClicked(val questId: QuestId) : PackDetailsAction
    data object UnlockClicked : PackDetailsAction
    data object RetryClicked : PackDetailsAction
}

package com.togetherly.feature.questdetail.presentation

/**
 * Every user-initiated intent quest detail can express, dispatched through the single
 * [QuestDetailViewModel.onAction] entry point — same pattern as
 * [com.togetherly.feature.today.presentation.TodayAction].
 */
sealed interface QuestDetailAction {
    data object RetryClicked : QuestDetailAction
    data object BackClicked : QuestDetailAction
    data object SaveClicked : QuestDetailAction
    data object StartClicked : QuestDetailAction
    data object UnlockClicked : QuestDetailAction
    data object ReplaceActiveSessionConfirmed : QuestDetailAction
    data object KeepActiveSessionClicked : QuestDetailAction
}

package com.togetherly.feature.questmode.presentation

sealed interface QuestModeAction {
    data object ScreenStarted : QuestModeAction
    data object RetryClicked : QuestModeAction
    data object BackClicked : QuestModeAction
    data object CloseClicked : QuestModeAction
    data object KeepInProgressClicked : QuestModeAction
    data object AbandonClicked : QuestModeAction
    data object AbandonConfirmed : QuestModeAction
    data object DialogDismissed : QuestModeAction
    data object PhoneDownClicked : QuestModeAction
    data object ExitPhoneDownClicked : QuestModeAction
    data object HintsToggled : QuestModeAction
    data object CompleteClicked : QuestModeAction
    data object ErrorDismissed : QuestModeAction
}

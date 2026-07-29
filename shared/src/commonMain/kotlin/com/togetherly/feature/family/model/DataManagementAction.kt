package com.togetherly.feature.family.model

sealed interface DataManagementAction {
    data object BackClicked : DataManagementAction
    data object DeleteMemoriesClicked : DataManagementAction
    data object ResetQuestHistoryClicked : DataManagementAction
    data object DeleteAllDataClicked : DataManagementAction

    /** Advances from [DataManagementConfirmationStage.DELETE_ALL_STAGE_ONE] to [DataManagementConfirmationStage.DELETE_ALL_STAGE_TWO]. */
    data object DeleteAllDataContinueClicked : DataManagementAction

    /** Dismisses whichever confirmation dialog is currently shown — never performs a deletion. */
    data object ConfirmationDismissed : DataManagementAction

    /** The final, deliberate confirmation for whichever [DataManagementConfirmationStage] is currently active. */
    data object DestructiveActionConfirmed : DataManagementAction

    data object MessageDismissed : DataManagementAction
}

package com.togetherly.feature.family.model

import com.togetherly.core.ui.UiText

data class DataManagementUiState(
    val confirmationStage: DataManagementConfirmationStage = DataManagementConfirmationStage.NONE,
    val isDeletingMemories: Boolean = false,
    val isResettingQuestHistory: Boolean = false,
    val isDeletingAllData: Boolean = false,
    val message: UiText? = null,
) {
    /** Gates every destructive entry point and back navigation — see [com.togetherly.feature.family.presentation.DataManagementViewModel]'s own KDoc. */
    val isBusy: Boolean
        get() = isDeletingMemories || isResettingQuestHistory || isDeletingAllData
}

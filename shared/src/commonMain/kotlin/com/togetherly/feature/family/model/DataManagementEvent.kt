package com.togetherly.feature.family.model

sealed interface DataManagementEvent {
    data object NavigateBack : DataManagementEvent

    /**
     * Fired only after [com.togetherly.domain.localdata.usecase.DeleteAllLocalData] returns
     * success — the Route reacts by clearing the entire back stack and navigating to onboarding.
     * Never fired for [com.togetherly.domain.localdata.usecase.DeleteMemories]/
     * [com.togetherly.domain.localdata.usecase.ResetQuestHistory], which stay on this screen.
     */
    data object LocalDataDeleted : DataManagementEvent
}

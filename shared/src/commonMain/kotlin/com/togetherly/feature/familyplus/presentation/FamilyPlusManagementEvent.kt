package com.togetherly.feature.familyplus.presentation

/** [OpenCustomerCenter] only ever fires when [com.togetherly.feature.familyplus.model.FamilyPlusManagementUiState.customerCenterAvailable] was true at the moment of the tap — see [FamilyPlusManagementViewModel.onAction]. */
sealed interface FamilyPlusManagementEvent {
    data object OpenPaywall : FamilyPlusManagementEvent
    data object OpenCustomerCenter : FamilyPlusManagementEvent
}

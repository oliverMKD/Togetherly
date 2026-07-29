package com.togetherly.feature.familyplus.presentation

sealed interface FamilyPlusManagementAction {
    data object ViewPlansClicked : FamilyPlusManagementAction
    data object RestoreClicked : FamilyPlusManagementAction
    data object ManageSubscriptionClicked : FamilyPlusManagementAction
}

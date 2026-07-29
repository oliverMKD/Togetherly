package com.togetherly.feature.family.model

sealed interface FamilyAction {
    data object EditProfileClicked : FamilyAction
    data object QuestPreferencesClicked : FamilyAction
    data object ReminderClicked : FamilyAction
    data object MemorySettingsClicked : FamilyAction
    data object PrivacyClicked : FamilyAction
    data object LegalClicked : FamilyAction
    data object AboutClicked : FamilyAction
    data object DataManagementClicked : FamilyAction
    data object MessageDismissed : FamilyAction
}

package com.togetherly.feature.family.model

sealed interface FamilyEvent {
    data object OpenProfileEditor : FamilyEvent
    data object OpenQuestPreferences : FamilyEvent
    data object OpenReminder : FamilyEvent
    data object OpenMemorySettings : FamilyEvent
    data object OpenPrivacy : FamilyEvent
    data object OpenLegal : FamilyEvent
    data object OpenAbout : FamilyEvent
    data object OpenDataManagement : FamilyEvent
}

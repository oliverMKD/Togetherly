package com.togetherly.feature.family.model

/** Same shape as [FamilyProfileEditorEvent] — see that type's own KDoc for why [SaveCompleted]/[NavigatedBackWithoutSaving] are kept distinct. */
sealed interface QuestPreferencesEvent {
    data object SaveCompleted : QuestPreferencesEvent
    data object NavigatedBackWithoutSaving : QuestPreferencesEvent
}

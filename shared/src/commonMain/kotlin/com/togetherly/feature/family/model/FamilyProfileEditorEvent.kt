package com.togetherly.feature.family.model

/**
 * One-off effects only — never re-derivable from [FamilyProfileUiState] (see this project's
 * one-time-event convention, e.g. [com.togetherly.feature.onboarding.presentation.OnboardingEvent]).
 * [SaveCompleted] and [NavigatedBackWithoutSaving] both mean "leave the editor"; they're kept
 * distinct so [com.togetherly.feature.family.presentation.FamilyProfileEditorRoute] can react the
 * same way to both (`onSaved`/`onNavigateBack` are both just `popBackStack()` today) without the
 * ViewModel conflating "the user left" with "the user saved."
 */
sealed interface FamilyProfileEditorEvent {
    data object SaveCompleted : FamilyProfileEditorEvent
    data object NavigatedBackWithoutSaving : FamilyProfileEditorEvent
}

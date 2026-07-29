package com.togetherly.feature.onboarding.presentation

/**
 * One-off effects only — never a substitute for a state field. Moving between onboarding steps is
 * an ordinary [com.togetherly.feature.onboarding.model.OnboardingUiState.step] change, not an
 * event, precisely because a state change is safe to re-observe on recomposition while an event
 * must fire exactly once per occurrence.
 */
sealed interface OnboardingEvent {
    data object NavigateBack : OnboardingEvent
    data object FamilyCreated : OnboardingEvent
}

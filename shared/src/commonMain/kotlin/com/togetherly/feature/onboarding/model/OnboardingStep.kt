package com.togetherly.feature.onboarding.model

/**
 * The onboarding flow is one internal state machine on a single navigation destination, not one
 * `NavDestination` per step — see [com.togetherly.feature.onboarding.navigation]'s own KDoc for
 * why. [WELCOME] carries no form data; [PREFERENCES] holds three related choices (duration,
 * location, preparation) together as long as the screen stays readable at large font scale — see
 * that step's own screen KDoc, once built, for whether it was ever split.
 */
enum class OnboardingStep {
    WELCOME,
    FAMILY_NAME,
    AGE_BANDS,
    INTERESTS,
    PREFERENCES,
    REMINDER,
    REVIEW,
}

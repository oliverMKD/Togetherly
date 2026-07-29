package com.togetherly.feature.onboarding.model

/**
 * Every onboarding field that can be individually invalid — keys into
 * [OnboardingUiState.validationErrors]. [com.togetherly.domain.family.LocationPreference] and
 * [com.togetherly.domain.family.PreparationPreference] have no entry here: both always carry a
 * default value in [OnboardingUiState], so neither can ever be "unselected."
 */
enum class OnboardingField {
    FAMILY_NAME,
    AGE_BANDS,
    INTERESTS,
    DURATIONS,
    REMINDER_DAYS,
    REMINDER_TIME,
}

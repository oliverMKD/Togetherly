package com.togetherly.feature.onboarding.navigation

import com.togetherly.feature.onboarding.model.OnboardingStep
import com.togetherly.feature.onboarding.model.OnboardingStep.AGE_BANDS
import com.togetherly.feature.onboarding.model.OnboardingStep.FAMILY_NAME
import com.togetherly.feature.onboarding.model.OnboardingStep.INTERESTS
import com.togetherly.feature.onboarding.model.OnboardingStep.PREFERENCES
import com.togetherly.feature.onboarding.model.OnboardingStep.REMINDER
import com.togetherly.feature.onboarding.model.OnboardingStep.REVIEW
import com.togetherly.feature.onboarding.model.OnboardingStep.WELCOME

/**
 * Onboarding's step order, in one place. This is deliberately *not* a `NavGraph`/`NavController`
 * seam: the whole flow lives on a single navigation destination as one state machine (see
 * [com.togetherly.feature.onboarding.model.OnboardingStep]'s own KDoc), so "moving forward/back" is
 * this package's job, and the app's real navigation graph (`com.togetherly.navigation`) never sees
 * an individual onboarding step. [com.togetherly.feature.onboarding.presentation.OnboardingViewModel]
 * is the only caller.
 */
internal fun OnboardingStep.next(): OnboardingStep? = when (this) {
    WELCOME -> FAMILY_NAME
    FAMILY_NAME -> AGE_BANDS
    AGE_BANDS -> INTERESTS
    INTERESTS -> PREFERENCES
    PREFERENCES -> REMINDER
    REMINDER -> REVIEW
    REVIEW -> null
}

internal fun OnboardingStep.previous(): OnboardingStep? = when (this) {
    WELCOME -> null
    FAMILY_NAME -> WELCOME
    AGE_BANDS -> FAMILY_NAME
    INTERESTS -> AGE_BANDS
    PREFERENCES -> INTERESTS
    REMINDER -> PREFERENCES
    REVIEW -> REMINDER
}

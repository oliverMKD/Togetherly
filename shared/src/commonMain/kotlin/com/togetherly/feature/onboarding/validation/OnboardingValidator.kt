package com.togetherly.feature.onboarding.validation

import com.togetherly.core.ui.UiText
import com.togetherly.domain.family.FamilyDisplayName
import com.togetherly.domain.validation.DomainValidationException
import com.togetherly.domain.validation.DomainValidationReason
import com.togetherly.feature.onboarding.model.OnboardingField
import com.togetherly.feature.onboarding.model.OnboardingStep
import com.togetherly.feature.onboarding.model.OnboardingUiState
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.onboarding_error_age_bands_required
import togetherly.shared.generated.resources.onboarding_error_durations_required
import togetherly.shared.generated.resources.onboarding_error_family_name_invalid
import togetherly.shared.generated.resources.onboarding_error_family_name_too_long
import togetherly.shared.generated.resources.onboarding_error_interests_required
import togetherly.shared.generated.resources.onboarding_error_reminder_days_required
import togetherly.shared.generated.resources.onboarding_error_reminder_time_required

/**
 * A pure function of [OnboardingUiState] to validation errors — no ViewModel, no I/O, nothing
 * platform-shaped. [validateStep] checks only the fields the *current* step collects (so an unrelated
 * later step's still-empty selection never blocks an earlier step's Continue); [validateAll] checks
 * every field and is what actually gates saving, from [OnboardingStep.REVIEW].
 */
object OnboardingValidator {

    fun validateStep(state: OnboardingUiState): PersistentMap<OnboardingField, UiText> = when (state.step) {
        OnboardingStep.WELCOME -> persistentMapOf()
        OnboardingStep.FAMILY_NAME -> errorsOf(OnboardingField.FAMILY_NAME to familyNameError(state))
        OnboardingStep.AGE_BANDS -> errorsOf(OnboardingField.AGE_BANDS to ageBandsError(state))
        OnboardingStep.INTERESTS -> errorsOf(OnboardingField.INTERESTS to interestsError(state))
        OnboardingStep.PREFERENCES -> errorsOf(OnboardingField.DURATIONS to durationsError(state))
        OnboardingStep.REMINDER -> errorsOf(
            OnboardingField.REMINDER_DAYS to reminderDaysError(state),
            OnboardingField.REMINDER_TIME to reminderTimeError(state),
        )
        OnboardingStep.REVIEW -> validateAll(state)
    }

    fun validateAll(state: OnboardingUiState): PersistentMap<OnboardingField, UiText> = errorsOf(
        OnboardingField.FAMILY_NAME to familyNameError(state),
        OnboardingField.AGE_BANDS to ageBandsError(state),
        OnboardingField.INTERESTS to interestsError(state),
        OnboardingField.DURATIONS to durationsError(state),
        OnboardingField.REMINDER_DAYS to reminderDaysError(state),
        OnboardingField.REMINDER_TIME to reminderTimeError(state),
    )

    private fun errorsOf(vararg entries: Pair<OnboardingField, UiText?>): PersistentMap<OnboardingField, UiText> =
        entries.mapNotNull { (field, error) -> error?.let { field to it } }.toMap().toPersistentMap()

    /** Trims before validating: a trailing space from ordinary typing must never block Continue. */
    private fun familyNameError(state: OnboardingUiState): UiText? = validateFamilyDisplayName(state.familyName)

    private fun ageBandsError(state: OnboardingUiState): UiText? =
        if (state.selectedAgeBands.isEmpty()) UiText.Resource(Res.string.onboarding_error_age_bands_required) else null

    private fun interestsError(state: OnboardingUiState): UiText? =
        if (state.selectedInterests.isEmpty()) UiText.Resource(Res.string.onboarding_error_interests_required) else null

    private fun durationsError(state: OnboardingUiState): UiText? =
        if (state.selectedDurations.isEmpty()) UiText.Resource(Res.string.onboarding_error_durations_required) else null

    /** A disabled reminder ignores whatever stale day/time selection is still sitting in state. */
    private fun reminderDaysError(state: OnboardingUiState): UiText? =
        if (state.reminderEnabled && state.reminderDays.isEmpty()) {
            UiText.Resource(Res.string.onboarding_error_reminder_days_required)
        } else {
            null
        }

    private fun reminderTimeError(state: OnboardingUiState): UiText? =
        if (state.reminderEnabled && state.reminderTime == null) {
            UiText.Resource(Res.string.onboarding_error_reminder_time_required)
        } else {
            null
        }
}

/**
 * The shared family-display-name rule — a blank/whitespace-only name is always valid (the field is
 * optional; [com.togetherly.domain.family.FamilyDisplayName] is `null` in that case), a non-blank
 * one must satisfy [com.togetherly.domain.family.FamilyDisplayName]'s own constructor invariants.
 * Public and top-level (not a member of [OnboardingValidator]) so [com.togetherly.feature.family.presentation.FamilyProfileValidator]
 * (Step 13.2's profile editor) reuses this exact rule rather than re-implementing it — see that
 * object's own KDoc.
 */
fun validateFamilyDisplayName(rawName: String): UiText? {
    val trimmed = rawName.trim()
    if (trimmed.isEmpty()) return null
    return try {
        FamilyDisplayName(trimmed)
        null
    } catch (e: DomainValidationException) {
        e.reason.toFamilyNameUiText()
    }
}

private fun DomainValidationReason.toFamilyNameUiText(): UiText = when (this) {
    DomainValidationReason.VALUE_TOO_LONG -> UiText.Resource(Res.string.onboarding_error_family_name_too_long)
    else -> UiText.Resource(Res.string.onboarding_error_family_name_invalid)
}

package com.togetherly.feature.family.presentation

import com.togetherly.core.ui.UiText
import com.togetherly.feature.family.model.FamilyProfileField
import com.togetherly.feature.family.model.FamilyProfileUiState
import com.togetherly.feature.onboarding.validation.validateFamilyDisplayName
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.onboarding_error_age_bands_required
import togetherly.shared.generated.resources.onboarding_error_durations_required

/**
 * A pure function of [FamilyProfileUiState] to validation errors — same shape as
 * [com.togetherly.feature.onboarding.validation.OnboardingValidator]. Reuses onboarding's own rules
 * rather than re-implementing them: [validateFamilyDisplayName] is the exact function
 * [com.togetherly.feature.onboarding.validation.OnboardingValidator] itself calls, and the
 * "must select at least one" age-band/duration errors reuse onboarding's own string resources
 * rather than adding near-duplicate copy under a new key.
 *
 * Age bands and durations are required (non-empty) here for the same reason onboarding requires
 * them: an empty set breaks quest-recommendation matching. There is no "impossible age-range
 * combination" to reject beyond that — [com.togetherly.domain.family.AgeBand] has no invariants
 * between its values.
 */
object FamilyProfileValidator {

    fun validate(state: FamilyProfileUiState): PersistentMap<FamilyProfileField, UiText> = errorsOf(
        FamilyProfileField.FAMILY_NAME to validateFamilyDisplayName(state.familyName),
        FamilyProfileField.AGE_BANDS to ageBandsError(state),
        FamilyProfileField.DURATIONS to durationsError(state),
    )

    private fun errorsOf(vararg entries: Pair<FamilyProfileField, UiText?>): PersistentMap<FamilyProfileField, UiText> =
        entries.mapNotNull { (field, error) -> error?.let { field to it } }.toMap().toPersistentMap()

    private fun ageBandsError(state: FamilyProfileUiState): UiText? =
        if (state.selectedAgeBands.isEmpty()) UiText.Resource(Res.string.onboarding_error_age_bands_required) else null

    private fun durationsError(state: FamilyProfileUiState): UiText? =
        if (state.selectedDurations.isEmpty()) UiText.Resource(Res.string.onboarding_error_durations_required) else null
}

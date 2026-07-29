package com.togetherly.feature.onboarding.validation

import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.FamilyDisplayName
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.feature.onboarding.model.OnboardingField
import com.togetherly.feature.onboarding.model.OnboardingStep
import com.togetherly.feature.onboarding.model.OnboardingUiState
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Direct unit coverage of the pure [OnboardingValidator] — [com.togetherly.feature.onboarding.presentation.OnboardingViewModelTest]
 * exercises most of these same rules indirectly (through `ContinueClicked`/`CreateFamilyClicked`),
 * but the family-name-length rule specifically was never reachable that way (every ViewModel test
 * uses a short name or skips the field entirely), so it's tested here instead.
 */
class OnboardingValidatorTest {

    private fun validState() = OnboardingUiState(
        step = OnboardingStep.REVIEW,
        selectedAgeBands = persistentSetOf(AgeBand.AGE_6_TO_8),
        selectedInterests = persistentSetOf(QuestCategory.CREATE),
        selectedDurations = persistentSetOf(DurationBand.TEN_MINUTES),
    )

    @Test
    fun blankFamilyNameIsValid() {
        val errors = OnboardingValidator.validateAll(validState().copy(familyName = ""))

        assertFalse(errors.containsKey(OnboardingField.FAMILY_NAME))
    }

    @Test
    fun familyNameWithinTheLimitIsValid() {
        val errors = OnboardingValidator.validateAll(validState().copy(familyName = "Team Firefly"))

        assertFalse(errors.containsKey(OnboardingField.FAMILY_NAME))
    }

    @Test
    fun familyNameOverTheLimitIsInvalid() {
        val tooLong = "A".repeat(FamilyDisplayName.MAX_LENGTH + 1)

        val errors = OnboardingValidator.validateAll(validState().copy(familyName = tooLong))

        assertTrue(errors.containsKey(OnboardingField.FAMILY_NAME))
    }

    @Test
    fun familyNameWithSurroundingWhitespaceIsValidBecauseItsTrimmedFirst() {
        val errors = OnboardingValidator.validateAll(validState().copy(familyName = "  Team Firefly  "))

        assertFalse(errors.containsKey(OnboardingField.FAMILY_NAME))
    }

    @Test
    fun emptyAgeBandsIsInvalid() {
        val errors = OnboardingValidator.validateAll(validState().copy(selectedAgeBands = persistentSetOf()))

        assertTrue(errors.containsKey(OnboardingField.AGE_BANDS))
    }

    @Test
    fun emptyInterestsIsInvalid() {
        val errors = OnboardingValidator.validateAll(validState().copy(selectedInterests = persistentSetOf()))

        assertTrue(errors.containsKey(OnboardingField.INTERESTS))
    }

    @Test
    fun emptyDurationsIsInvalid() {
        val errors = OnboardingValidator.validateAll(validState().copy(selectedDurations = persistentSetOf()))

        assertTrue(errors.containsKey(OnboardingField.DURATIONS))
    }

    @Test
    fun reminderDisabledIsAlwaysValidRegardlessOfStaleDaysOrTime() {
        val staleState = validState().copy(
            reminderEnabled = false,
            reminderDays = persistentSetOf(),
            reminderTime = null,
        )

        val errors = OnboardingValidator.validateAll(staleState)

        assertFalse(errors.containsKey(OnboardingField.REMINDER_DAYS))
        assertFalse(errors.containsKey(OnboardingField.REMINDER_TIME))
    }

    @Test
    fun reminderEnabledWithoutDaysIsInvalid() {
        val state = validState().copy(reminderEnabled = true, reminderTime = LocalTime(18, 0))

        val errors = OnboardingValidator.validateAll(state)

        assertTrue(errors.containsKey(OnboardingField.REMINDER_DAYS))
    }

    @Test
    fun reminderEnabledWithoutTimeIsInvalid() {
        val state = validState().copy(reminderEnabled = true, reminderDays = persistentSetOf(DayOfWeek.MONDAY))

        val errors = OnboardingValidator.validateAll(state)

        assertTrue(errors.containsKey(OnboardingField.REMINDER_TIME))
    }

    @Test
    fun reminderEnabledWithDaysAndTimeIsValid() {
        val state = validState().copy(
            reminderEnabled = true,
            reminderDays = persistentSetOf(DayOfWeek.MONDAY),
            reminderTime = LocalTime(18, 0),
        )

        val errors = OnboardingValidator.validateAll(state)

        assertFalse(errors.containsKey(OnboardingField.REMINDER_DAYS))
        assertFalse(errors.containsKey(OnboardingField.REMINDER_TIME))
    }

    @Test
    fun validateStepOnlyChecksTheCurrentStepsFields() {
        // At AGE_BANDS, interests/durations are still empty — validateStep must not flag them.
        val state = OnboardingUiState(step = OnboardingStep.AGE_BANDS)

        val errors = OnboardingValidator.validateStep(state)

        assertTrue(errors.containsKey(OnboardingField.AGE_BANDS))
        assertFalse(errors.containsKey(OnboardingField.INTERESTS))
        assertFalse(errors.containsKey(OnboardingField.DURATIONS))
    }

    @Test
    fun fullyValidStateHasNoErrors() {
        assertTrue(OnboardingValidator.validateAll(validState()).isEmpty())
    }
}

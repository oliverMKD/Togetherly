package com.togetherly.feature.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.core.ui.UiText
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.feature.onboarding.model.OnboardingField
import com.togetherly.feature.onboarding.model.OnboardingStep
import com.togetherly.feature.onboarding.model.OnboardingUiState
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

/**
 * A living catalogue of every onboarding step and the cross-cutting states (validation error,
 * saving, save failure) called for in this step's own spec — never a substitute for the UI tests,
 * which prove behavior; these only prove appearance. Every fixture below is fictional sample data
 * (a placeholder family name, made-up selections), matching this project's privacy audit
 * requirement that previews/screenshots never carry real data.
 */

private val sampleAgeBands = persistentSetOf(AgeBand.AGE_6_TO_8, AgeBand.AGE_9_TO_11)
private val sampleInterests = persistentSetOf(QuestCategory.CREATE, QuestCategory.SILLY, QuestCategory.DISCOVER)
private val sampleDurations = persistentSetOf(DurationBand.TEN_MINUTES, DurationBand.TWENTY_MINUTES)
private val sampleReminderDays = persistentSetOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

private fun reviewState() = OnboardingUiState(
    step = OnboardingStep.REVIEW,
    familyName = "Team Firefly",
    selectedAgeBands = sampleAgeBands,
    selectedInterests = sampleInterests,
    selectedDurations = sampleDurations,
    locationPreference = LocationPreference.OUTDOOR,
    preparationPreference = PreparationPreference.SIMPLE_MATERIALS,
    reminderEnabled = true,
    reminderDays = sampleReminderDays,
    reminderTime = LocalTime(18, 30),
)

@Composable
private fun OnboardingPreview(state: OnboardingUiState, darkTheme: Boolean = false) {
    TogetherlyTheme(darkTheme = darkTheme) {
        OnboardingScreen(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun WelcomeStepPreview() {
    OnboardingPreview(OnboardingUiState(step = OnboardingStep.WELCOME))
}

@Preview
@Composable
private fun FamilyNameStepPreview() {
    OnboardingPreview(OnboardingUiState(step = OnboardingStep.FAMILY_NAME, familyName = "Team Firefly"))
}

@Preview
@Composable
private fun AgeBandsStepPreview() {
    OnboardingPreview(OnboardingUiState(step = OnboardingStep.AGE_BANDS, selectedAgeBands = sampleAgeBands))
}

@Preview
@Composable
private fun InterestsStepPreview() {
    OnboardingPreview(OnboardingUiState(step = OnboardingStep.INTERESTS, selectedInterests = sampleInterests))
}

@Preview
@Composable
private fun PreferencesStepPreview() {
    OnboardingPreview(
        OnboardingUiState(
            step = OnboardingStep.PREFERENCES,
            selectedDurations = sampleDurations,
            locationPreference = LocationPreference.OUTDOOR,
            preparationPreference = PreparationPreference.SIMPLE_MATERIALS,
        ),
    )
}

@Preview
@Composable
private fun ReminderStepPreview() {
    OnboardingPreview(
        OnboardingUiState(
            step = OnboardingStep.REMINDER,
            reminderEnabled = true,
            reminderDays = sampleReminderDays,
            reminderTime = LocalTime(18, 30),
        ),
    )
}

@Preview
@Composable
private fun ReviewStepPreview() {
    OnboardingPreview(reviewState())
}

// -- Cross-cutting states -------------------------------------------------------------------

@Preview
@Composable
private fun ValidationErrorPreview() {
    OnboardingPreview(
        OnboardingUiState(
            step = OnboardingStep.AGE_BANDS,
            validationErrors = persistentMapOf(
                OnboardingField.AGE_BANDS to UiText.Dynamic("Choose at least one age group."),
            ),
        ),
    )
}

@Preview
@Composable
private fun SavingPreview() {
    OnboardingPreview(reviewState().copy(isSaving = true))
}

@Preview
@Composable
private fun SaveFailurePreview() {
    OnboardingPreview(
        reviewState().copy(saveError = UiText.Dynamic("Something went wrong. Please try again.")),
    )
}

// -- Theme / accessibility / device size ----------------------------------------------------

@Preview
@Composable
private fun WelcomeStepDarkPreview() {
    OnboardingPreview(OnboardingUiState(step = OnboardingStep.WELCOME), darkTheme = true)
}

@Preview
@Composable
private fun ReviewStepDarkPreview() {
    OnboardingPreview(reviewState(), darkTheme = true)
}

@Preview(fontScale = 2f)
@Composable
private fun PreferencesStepLargeFontPreview() {
    OnboardingPreview(
        OnboardingUiState(
            step = OnboardingStep.PREFERENCES,
            selectedDurations = sampleDurations,
            locationPreference = LocationPreference.OUTDOOR,
            preparationPreference = PreparationPreference.SIMPLE_MATERIALS,
        ),
    )
}

@Preview(widthDp = 320)
@Composable
private fun ReviewStepNarrowPhonePreview() {
    OnboardingPreview(reviewState())
}

@Preview(widthDp = 840, heightDp = 1000)
@Composable
private fun ReviewStepWideDevicePreview() {
    OnboardingPreview(reviewState())
}

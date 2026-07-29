package com.togetherly.feature.onboarding.presentation

import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.quest.QuestCategory
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

/**
 * Every user-initiated intent onboarding can express, dispatched through the single
 * [OnboardingViewModel.onAction] entry point — a screen never calls a more specific ViewModel
 * method directly.
 */
sealed interface OnboardingAction {
    data object ContinueClicked : OnboardingAction
    data object BackClicked : OnboardingAction
    data object SkipNameClicked : OnboardingAction
    data class FamilyNameChanged(val value: String) : OnboardingAction
    data class AgeBandToggled(val value: AgeBand) : OnboardingAction
    data class InterestToggled(val value: QuestCategory) : OnboardingAction
    data class DurationToggled(val value: DurationBand) : OnboardingAction
    data class LocationSelected(val value: LocationPreference) : OnboardingAction
    data class PreparationSelected(val value: PreparationPreference) : OnboardingAction
    data class ReminderEnabledChanged(val enabled: Boolean) : OnboardingAction
    data class ReminderDayToggled(val day: DayOfWeek) : OnboardingAction
    data class ReminderTimeChanged(val time: LocalTime) : OnboardingAction
    data object CreateFamilyClicked : OnboardingAction
    data object RetrySaveClicked : OnboardingAction
}

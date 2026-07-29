package com.togetherly.feature.today.presentation

import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.PreparationLevel
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestLocation

/**
 * Every user-initiated intent Today can express, dispatched through the single
 * [TodayViewModel.onAction] entry point — a screen never calls a more specific ViewModel method
 * directly (same rule as [com.togetherly.feature.onboarding.presentation.OnboardingAction]).
 */
sealed interface TodayAction {
    data object ScreenStarted : TodayAction
    data object RetryClicked : TodayAction
    data object RevealClicked : TodayAction
    data object FiltersClicked : TodayAction
    data object FiltersDismissed : TodayAction
    data class DurationFilterChanged(val value: DurationBand?) : TodayAction
    data class LocationFilterChanged(val value: QuestLocation?) : TodayAction
    data class EnergyFilterChanged(val value: EnergyLevel?) : TodayAction
    data class PreparationFilterChanged(val value: PreparationLevel?) : TodayAction
    data class CategoryFilterChanged(val value: QuestCategory?) : TodayAction
    data object ClearFiltersClicked : TodayAction
    data object ApplyFiltersClicked : TodayAction
    data object RerollClicked : TodayAction
    data object RerollConfirmed : TodayAction
    data object RerollCancelled : TodayAction
    data object SaveClicked : TodayAction
    data object StartClicked : TodayAction
    data object TransientErrorDismissed : TodayAction
}

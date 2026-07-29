package com.togetherly.feature.family.model

import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.quest.EnergyLevel

sealed interface QuestPreferencesAction {
    data class DurationToggled(val value: DurationBand) : QuestPreferencesAction
    data class EnergyToggled(val value: EnergyLevel) : QuestPreferencesAction
    data class LocationPreferenceChanged(val value: LocationPreference) : QuestPreferencesAction
    data class PreparationPreferenceChanged(val value: PreparationPreference) : QuestPreferencesAction
    data object ResetToDefaultsClicked : QuestPreferencesAction
    data object SaveClicked : QuestPreferencesAction
    data object BackClicked : QuestPreferencesAction
    data object DiscardConfirmed : QuestPreferencesAction
    data object DismissDiscardDialog : QuestPreferencesAction
}

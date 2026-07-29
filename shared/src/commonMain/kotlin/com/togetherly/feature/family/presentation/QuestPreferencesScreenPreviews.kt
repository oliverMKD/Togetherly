package com.togetherly.feature.family.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.core.ui.UiText
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.feature.family.model.QuestPreferencesField
import com.togetherly.feature.family.model.QuestPreferencesUiState
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

private val LOADED_STATE = QuestPreferencesUiState(
    isLoading = false,
    selectedDurations = persistentSetOf(DurationBand.TEN_MINUTES, DurationBand.TWENTY_MINUTES),
    selectedEnergyLevels = persistentSetOf(EnergyLevel.CALM, EnergyLevel.MODERATE),
    locationPreference = LocationPreference.BOTH,
    preparationPreference = PreparationPreference.SIMPLE_MATERIALS,
)

@Composable
private fun QuestPreferencesPreview(state: QuestPreferencesUiState) {
    TogetherlyTheme {
        QuestPreferencesScreen(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun QuestPreferencesDefaultPreview() {
    QuestPreferencesPreview(LOADED_STATE)
}

@Preview
@Composable
private fun QuestPreferencesLoadingPreview() {
    QuestPreferencesPreview(QuestPreferencesUiState(isLoading = true))
}

@Preview
@Composable
private fun QuestPreferencesValidationErrorsPreview() {
    QuestPreferencesPreview(
        LOADED_STATE.copy(
            selectedDurations = persistentSetOf(),
            selectedEnergyLevels = persistentSetOf(),
            validationErrors = persistentMapOf(
                QuestPreferencesField.DURATIONS to UiText.Dynamic("Choose at least one duration."),
                QuestPreferencesField.ENERGY to UiText.Dynamic("Choose at least one energy level."),
            ),
        ),
    )
}

@Preview
@Composable
private fun QuestPreferencesUnsavedChangesPreview() {
    QuestPreferencesPreview(LOADED_STATE.copy(hasUnsavedChanges = true, showDiscardDialog = true))
}

@Preview
@Composable
private fun QuestPreferencesSavingPreview() {
    QuestPreferencesPreview(LOADED_STATE.copy(isSaving = true))
}

@Preview
@Composable
private fun QuestPreferencesErrorPreview() {
    QuestPreferencesPreview(LOADED_STATE.copy(error = UiText.Dynamic("Something went wrong. Please try again.")))
}

@Preview(fontScale = 2f)
@Composable
private fun QuestPreferencesLargeTextPreview() {
    QuestPreferencesPreview(LOADED_STATE)
}

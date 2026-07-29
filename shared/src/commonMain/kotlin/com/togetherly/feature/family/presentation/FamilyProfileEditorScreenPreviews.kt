package com.togetherly.feature.family.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.core.ui.UiText
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import com.togetherly.feature.family.model.FamilyProfileField
import com.togetherly.feature.family.model.FamilyProfileUiState
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

private val LOADED_STATE = FamilyProfileUiState(
    isLoading = false,
    familyName = "Team Firefly",
    selectedAgeBands = persistentSetOf(AgeBand.AGE_6_TO_8, AgeBand.AGE_9_TO_11),
    selectedDurations = persistentSetOf(DurationBand.TEN_MINUTES, DurationBand.TWENTY_MINUTES),
    locationPreference = LocationPreference.BOTH,
)

@Composable
private fun FamilyProfileEditorPreview(state: FamilyProfileUiState) {
    TogetherlyTheme {
        FamilyProfileEditorScreen(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun FamilyProfileEditorDefaultPreview() {
    FamilyProfileEditorPreview(LOADED_STATE)
}

@Preview
@Composable
private fun FamilyProfileEditorValidationErrorsPreview() {
    FamilyProfileEditorPreview(
        LOADED_STATE.copy(
            selectedAgeBands = persistentSetOf(),
            selectedDurations = persistentSetOf(),
            validationErrors = persistentMapOf(
                FamilyProfileField.AGE_BANDS to UiText.Dynamic("Choose at least one age group."),
                FamilyProfileField.DURATIONS to UiText.Dynamic("Choose at least one duration."),
            ),
        ),
    )
}

@Preview
@Composable
private fun FamilyProfileEditorUnsavedChangesPreview() {
    FamilyProfileEditorPreview(LOADED_STATE.copy(familyName = "Team Firefly Adventures", hasUnsavedChanges = true, showDiscardDialog = true))
}

@Preview
@Composable
private fun FamilyProfileEditorSavingPreview() {
    FamilyProfileEditorPreview(LOADED_STATE.copy(isSaving = true))
}

@Preview
@Composable
private fun FamilyProfileEditorErrorPreview() {
    FamilyProfileEditorPreview(LOADED_STATE.copy(error = UiText.Dynamic("Something went wrong. Please try again.")))
}

@Preview
@Composable
private fun FamilyProfileEditorLongFamilyNamePreview() {
    FamilyProfileEditorPreview(LOADED_STATE.copy(familyName = "The Extraordinarily Adventurous Wonderful Family of Firefly Lane"))
}

@Preview(fontScale = 2f)
@Composable
private fun FamilyProfileEditorLargeTextPreview() {
    FamilyProfileEditorPreview(LOADED_STATE)
}

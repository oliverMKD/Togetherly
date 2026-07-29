package com.togetherly.feature.family.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.feature.family.model.FamilyUiState
import com.togetherly.feature.familyplus.model.FamilyPlusManagementUiState
import com.togetherly.feature.familyplus.presentation.FamilyPlusManagementContent
import kotlinx.collections.immutable.persistentSetOf

private val LOADED_STATE = FamilyUiState(
    isLoading = false,
    familyName = "Team Firefly",
    ageBands = persistentSetOf(AgeBand.AGE_6_TO_8, AgeBand.AGE_9_TO_11),
    preferredDurations = persistentSetOf(DurationBand.TEN_MINUTES, DurationBand.TWENTY_MINUTES),
    locationPreference = LocationPreference.BOTH,
)

@Composable
private fun FamilyPreview(state: FamilyUiState, familyPlusState: FamilyPlusManagementUiState) {
    TogetherlyTheme {
        FamilyScreen(
            state = state,
            onAction = {},
            familyPlusSection = { FamilyPlusManagementContent(state = familyPlusState, onAction = {}) },
        )
    }
}

@Preview
@Composable
private fun FamilyRootFreeFamilyPreview() {
    FamilyPreview(LOADED_STATE, FamilyPlusManagementUiState(isLoading = false, access = FamilyAccess.free()))
}

@Preview
@Composable
private fun FamilyRootFamilyPlusPreview() {
    FamilyPreview(
        LOADED_STATE,
        FamilyPlusManagementUiState(isLoading = false, access = FamilyAccess.lifetime(), customerCenterAvailable = true),
    )
}

@Preview
@Composable
private fun FamilyRootLoadingPreview() {
    FamilyPreview(FamilyUiState(isLoading = true), FamilyPlusManagementUiState(isLoading = true))
}

package com.togetherly.feature.explore.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.feature.explore.model.ExploreUiState
import com.togetherly.feature.explore.preview.emptyCategoryResultsExploreUiState
import com.togetherly.feature.explore.preview.emptySearchResultsExploreUiState
import com.togetherly.feature.explore.preview.errorExploreUiState
import com.togetherly.feature.explore.preview.familyPlusExploreUiState
import com.togetherly.feature.explore.preview.freeFamilyExploreUiState
import com.togetherly.feature.explore.preview.loadingExploreUiState
import com.togetherly.feature.explore.preview.lockedPremiumCardsExploreUiState
import com.togetherly.feature.explore.preview.longTextExploreUiState
import com.togetherly.feature.explore.preview.searchResultsExploreUiState

/**
 * A living catalogue of Explore's rendered states — never a substitute for
 * [ExploreViewModelTest] (behavior) or the Compose UI tests (semantics/interaction); these only
 * prove appearance.
 */

@Composable
private fun ExplorePreview(state: ExploreUiState, darkTheme: Boolean = false) {
    TogetherlyTheme(darkTheme = darkTheme) {
        ExploreScreen(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun ExploreLoadingPreview() {
    ExplorePreview(loadingExploreUiState())
}

@Preview
@Composable
private fun ExploreFreeFamilyPreview() {
    ExplorePreview(freeFamilyExploreUiState())
}

@Preview
@Composable
private fun ExploreFamilyPlusPreview() {
    ExplorePreview(familyPlusExploreUiState())
}

@Preview
@Composable
private fun ExploreSearchResultsPreview() {
    ExplorePreview(searchResultsExploreUiState())
}

@Preview
@Composable
private fun ExploreEmptySearchResultsPreview() {
    ExplorePreview(emptySearchResultsExploreUiState())
}

@Preview
@Composable
private fun ExploreEmptyCategoryResultsPreview() {
    ExplorePreview(emptyCategoryResultsExploreUiState())
}

@Preview
@Composable
private fun ExploreErrorPreview() {
    ExplorePreview(errorExploreUiState())
}

@Preview
@Composable
private fun ExploreLongTextPreview() {
    ExplorePreview(longTextExploreUiState())
}

@Preview(fontScale = 2f)
@Composable
private fun ExploreLargeFontPreview() {
    ExplorePreview(freeFamilyExploreUiState())
}

@Preview
@Composable
private fun ExploreLockedPremiumCardsPreview() {
    ExplorePreview(lockedPremiumCardsExploreUiState())
}

@Preview
@Composable
private fun ExploreFreeFamilyDarkPreview() {
    ExplorePreview(freeFamilyExploreUiState(), darkTheme = true)
}

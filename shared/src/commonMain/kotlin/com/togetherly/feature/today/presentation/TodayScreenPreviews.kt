package com.togetherly.feature.today.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.feature.today.preview.applyingFiltersTodayUiState
import com.togetherly.feature.today.preview.errorTodayUiState
import com.togetherly.feature.today.preview.filterNoMatchErrorTodayUiState
import com.togetherly.feature.today.preview.freeRerollConsumedTodayUiState
import com.togetherly.feature.today.preview.loadingTodayUiState
import com.togetherly.feature.today.preview.multipleActiveFiltersTodayUiState
import com.togetherly.feature.today.preview.mysteryTodayUiState
import com.togetherly.feature.today.preview.noActiveFiltersTodayUiState
import com.togetherly.feature.today.preview.revealedTodayUiState
import com.togetherly.feature.today.preview.rerollConfirmationTodayUiState
import com.togetherly.feature.today.preview.rerollLoadingTodayUiState

/**
 * A living catalogue of Today's rendered states — never a substitute for
 * [com.togetherly.feature.today.presentation.TodayViewModelTest] (behavior) or the Compose UI tests
 * (semantics/interaction); these only prove appearance. Filters/reroll UI is intentionally absent
 * (Step 8.5).
 */

@Composable
private fun TodayPreview(state: TodayUiState, darkTheme: Boolean = false) {
    TogetherlyTheme(darkTheme = darkTheme) {
        TodayScreen(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun TodayLoadingPreview() {
    TodayPreview(loadingTodayUiState())
}

@Preview
@Composable
private fun TodayMysteryPreview() {
    TodayPreview(mysteryTodayUiState())
}

@Preview
@Composable
private fun TodayRevealedFreeQuestPreview() {
    TodayPreview(revealedTodayUiState())
}

@Preview
@Composable
private fun TodayRevealedSavedQuestPreview() {
    TodayPreview(revealedTodayUiState(isSaved = true))
}

@Preview
@Composable
private fun TodayRevealedWithMaterialsPreview() {
    TodayPreview(revealedTodayUiState(hasMaterials = true))
}

@Preview
@Composable
private fun TodayRevealedWithoutMaterialsPreview() {
    TodayPreview(revealedTodayUiState(hasMaterials = false))
}

@Preview
@Composable
private fun TodayErrorPreview() {
    TodayPreview(errorTodayUiState())
}

@Preview
@Composable
private fun TodayRevealedDarkPreview() {
    TodayPreview(revealedTodayUiState(), darkTheme = true)
}

@Preview(fontScale = 2f)
@Composable
private fun TodayRevealedLargeFontPreview() {
    TodayPreview(revealedTodayUiState())
}

@Preview(fontScale = 2f)
@Composable
private fun TodayMysteryLargeFontPreview() {
    TodayPreview(mysteryTodayUiState())
}

/** Reduced motion only affects the Mystery→Revealed transition itself, not a static snapshot — this preview exists to document that the reveal crossfade honors [com.togetherly.designsystem.theme.togetherlyReduceMotion] (see [TodayQuestReveal]), not to show a visibly different static frame. */
@Preview
@Composable
private fun TodayRevealedReducedMotionPreview() {
    TogetherlyTheme(reduceMotion = true) {
        TodayScreen(state = revealedTodayUiState(), onAction = {})
    }
}

@Preview
@Composable
private fun TodayFiltersNoneActivePreview() {
    TodayPreview(noActiveFiltersTodayUiState())
}

@Preview
@Composable
private fun TodayFiltersMultipleActivePreview() {
    TodayPreview(multipleActiveFiltersTodayUiState())
}

@Preview
@Composable
private fun TodayFiltersApplyingPreview() {
    TodayPreview(applyingFiltersTodayUiState())
}

@Preview
@Composable
private fun TodayFilterNoMatchErrorPreview() {
    TodayPreview(filterNoMatchErrorTodayUiState())
}

@Preview
@Composable
private fun TodayRerollConfirmationPreview() {
    TodayPreview(rerollConfirmationTodayUiState())
}

@Preview
@Composable
private fun TodayRerollLoadingPreview() {
    TodayPreview(rerollLoadingTodayUiState())
}

@Preview
@Composable
private fun TodayFreeRerollConsumedPreview() {
    TodayPreview(freeRerollConsumedTodayUiState())
}

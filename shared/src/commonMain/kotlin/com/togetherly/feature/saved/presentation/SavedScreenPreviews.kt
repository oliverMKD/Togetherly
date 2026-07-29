package com.togetherly.feature.saved.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.core.ui.UiText
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.feature.explore.preview.sampleExploreQuest
import com.togetherly.feature.saved.model.SavedUiState
import kotlinx.collections.immutable.persistentListOf

@Composable
private fun SavedPreview(state: SavedUiState) {
    TogetherlyTheme {
        SavedScreen(state = state, onAction = {}, onNavigateBack = {})
    }
}

@Preview
@Composable
private fun SavedLoadingPreview() {
    SavedPreview(SavedUiState.initial())
}

@Preview
@Composable
private fun SavedEmptyPreview() {
    SavedPreview(SavedUiState.initial().copy(isLoading = false))
}

@Preview
@Composable
private fun SavedWithQuestsPreview() {
    SavedPreview(
        SavedUiState.initial().copy(
            isLoading = false,
            quests = persistentListOf(
                sampleExploreQuest(id = "quest-1", title = "Backyard Scavenger Hunt", isSaved = true),
                sampleExploreQuest(id = "quest-2", title = "Draw a Shared Imaginary Creature", isSaved = true, isPremium = true, locked = true),
            ),
        ),
    )
}

@Preview
@Composable
private fun SavedErrorPreview() {
    SavedPreview(SavedUiState.initial().copy(isLoading = false, error = UiText.Dynamic("Something went wrong. Please try again.")))
}

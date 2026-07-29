package com.togetherly.feature.packdetails.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.core.ui.UiText
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.feature.explore.preview.sampleExplorePack
import com.togetherly.feature.explore.preview.sampleExploreQuest
import com.togetherly.feature.packdetails.model.ContentAccessState
import com.togetherly.feature.packdetails.model.PackDetailsUiState
import kotlinx.collections.immutable.persistentListOf

@Composable
private fun PackDetailsPreview(state: PackDetailsUiState) {
    TogetherlyTheme {
        PackDetailsScreen(state = state, onAction = {}, onNavigateBack = {})
    }
}

@Preview
@Composable
private fun PackDetailsLoadingPreview() {
    PackDetailsPreview(PackDetailsUiState.initial())
}

@Preview
@Composable
private fun PackDetailsFreePreview() {
    PackDetailsPreview(
        PackDetailsUiState.initial().copy(
            isLoading = false,
            pack = sampleExplorePack(title = "Quick Wins"),
            quests = persistentListOf(
                sampleExploreQuest(id = "quest-1", title = "Backyard Scavenger Hunt"),
                sampleExploreQuest(id = "quest-2", title = "Three Good Things"),
            ),
            accessState = ContentAccessState.FREE,
        ),
    )
}

@Preview
@Composable
private fun PackDetailsLockedPreview() {
    PackDetailsPreview(
        PackDetailsUiState.initial().copy(
            isLoading = false,
            pack = sampleExplorePack(title = "Creative Sparks", isPremium = true, locked = true),
            quests = persistentListOf(
                sampleExploreQuest(id = "quest-1", title = "Draw a Shared Imaginary Creature", isPremium = true, locked = true),
                sampleExploreQuest(id = "quest-2", title = "Gratitude Paper Chain", isPremium = true, locked = true),
            ),
            accessState = ContentAccessState.LOCKED,
        ),
    )
}

@Preview
@Composable
private fun PackDetailsUnlockedPreview() {
    PackDetailsPreview(
        PackDetailsUiState.initial().copy(
            isLoading = false,
            pack = sampleExplorePack(title = "Creative Sparks", isPremium = true, locked = false),
            quests = persistentListOf(
                sampleExploreQuest(id = "quest-1", title = "Draw a Shared Imaginary Creature", isPremium = true, locked = false),
                sampleExploreQuest(id = "quest-2", title = "Gratitude Paper Chain", isPremium = true, locked = false, isSaved = true),
            ),
            accessState = ContentAccessState.UNLOCKED,
        ),
    )
}

@Preview
@Composable
private fun PackDetailsNotFoundPreview() {
    PackDetailsPreview(
        PackDetailsUiState.initial().copy(isLoading = false, error = UiText.Dynamic("Something went wrong. Please try again.")),
    )
}

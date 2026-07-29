package com.togetherly.feature.questdetail.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.core.ui.UiText
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.PreparationLevel
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestLocation
import com.togetherly.feature.questdetail.model.QuestDetailUi
import com.togetherly.feature.questdetail.model.QuestDetailUiState
import com.togetherly.feature.today.mapper.label
import com.togetherly.feature.today.model.QuestCategoryUi
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.onboarding_age_band_6_to_8
import togetherly.shared.generated.resources.onboarding_age_band_9_to_11

/**
 * A living catalogue of Quest Detail's states — never a substitute for
 * [QuestDetailViewModelTest] (behavior) or the Compose UI tests (semantics/interaction).
 */

private fun sampleDetailUi(
    withMaterials: Boolean = true,
    withHints: Boolean = true,
    withSafetyNote: Boolean = true,
    locked: Boolean = false,
    isPremium: Boolean = false,
) = QuestDetailUi(
    id = QuestId("preview-quest"),
    title = "Backyard Scavenger Hunt",
    summary = "Find five hidden treasures together before the timer runs out.",
    category = QuestCategoryUi.DISCOVER,
    durationLabel = DurationBand.TWENTY_MINUTES.label(),
    locationLabel = QuestLocation.OUTDOOR.label(),
    preparationLabel = PreparationLevel.SIMPLE_MATERIALS.label(),
    energyLabel = EnergyLevel.MODERATE.label(),
    ageBandLabels = listOf(UiText.Resource(Res.string.onboarding_age_band_6_to_8), UiText.Resource(Res.string.onboarding_age_band_9_to_11)),
    isPremium = isPremium,
    packTitle = if (isPremium) "Creative Sparks" else "Quick Wins",
    instructions = if (locked) {
        emptyList()
    } else {
        listOf(
            "Hide five small objects in the yard.",
            "Give clues to find each one.",
            "Celebrate when every object is found.",
        )
    },
    materials = if (withMaterials) listOf("Small toys", "A basket") else emptyList(),
    hints = if (withHints && !locked) listOf("Try the garden.", "Look under things.") else emptyList(),
    safetyNote = if (withSafetyNote) "Adult supervision required near the street." else null,
)

@Composable
private fun QuestDetailPreview(state: QuestDetailUiState, darkTheme: Boolean = false) {
    TogetherlyTheme(darkTheme = darkTheme) {
        QuestDetailScreen(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun QuestDetailLoadingPreview() {
    QuestDetailPreview(QuestDetailUiState.Loading)
}

@Preview
@Composable
private fun QuestDetailContentPreview() {
    QuestDetailPreview(QuestDetailUiState.Content(quest = sampleDetailUi(), isSaved = false, isStarting = false))
}

@Preview
@Composable
private fun QuestDetailSavedPreview() {
    QuestDetailPreview(QuestDetailUiState.Content(quest = sampleDetailUi(), isSaved = true, isStarting = false))
}

@Preview
@Composable
private fun QuestDetailWithoutOptionalSectionsPreview() {
    QuestDetailPreview(
        QuestDetailUiState.Content(
            quest = sampleDetailUi(withMaterials = false, withHints = false, withSafetyNote = false),
            isSaved = false,
            isStarting = false,
        ),
    )
}

@Preview
@Composable
private fun QuestDetailStartingPreview() {
    QuestDetailPreview(QuestDetailUiState.Content(quest = sampleDetailUi(), isSaved = false, isStarting = true))
}

@Preview
@Composable
private fun QuestDetailUnlockedPremiumPreview() {
    QuestDetailPreview(
        QuestDetailUiState.Content(quest = sampleDetailUi(isPremium = true), isSaved = false, isStarting = false, locked = false),
    )
}

@Preview
@Composable
private fun QuestDetailLockedPremiumPreviewPreview() {
    QuestDetailPreview(
        QuestDetailUiState.Content(
            quest = sampleDetailUi(isPremium = true, locked = true),
            isSaved = false,
            isStarting = false,
            locked = true,
        ),
    )
}

@Preview
@Composable
private fun QuestDetailConflictPreview() {
    QuestDetailPreview(
        QuestDetailUiState.Content(quest = sampleDetailUi(), isSaved = false, isStarting = false, activeSessionConflict = true),
    )
}

@Preview
@Composable
private fun QuestDetailErrorPreview() {
    QuestDetailPreview(QuestDetailUiState.Error(UiText.Dynamic("Something went wrong. Please try again.")))
}

@Preview
@Composable
private fun QuestDetailDarkPreview() {
    QuestDetailPreview(QuestDetailUiState.Content(quest = sampleDetailUi(), isSaved = false, isStarting = false), darkTheme = true)
}

@Preview(fontScale = 2f)
@Composable
private fun QuestDetailLargeFontPreview() {
    QuestDetailPreview(QuestDetailUiState.Content(quest = sampleDetailUi(), isSaved = false, isStarting = false))
}

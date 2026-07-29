package com.togetherly.feature.explore.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.domain.explore.ExploreFilters
import com.togetherly.domain.explore.QuestAccessFilter
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestLocation
import com.togetherly.feature.explore.model.ExploreFiltersUiState

@Composable
private fun ExploreFiltersPreview(draft: ExploreFilters) {
    TogetherlyTheme {
        ExploreFiltersScreen(state = ExploreFiltersUiState(draft), onAction = {}, onNavigateBack = {})
    }
}

@Preview
@Composable
private fun ExploreFiltersNoneActivePreview() {
    ExploreFiltersPreview(ExploreFilters())
}

@Preview
@Composable
private fun ExploreFiltersMultipleActivePreview() {
    ExploreFiltersPreview(
        ExploreFilters(
            duration = DurationBand.TEN_MINUTES,
            energy = EnergyLevel.ACTIVE,
            location = QuestLocation.OUTDOOR,
            ageBand = AgeBand.AGE_6_TO_8,
            category = QuestCategory.MOVE,
            access = QuestAccessFilter.PREMIUM,
        ),
    )
}

package com.togetherly.feature.packdetails.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.core.ui.UiText
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.feature.explore.model.ExplorePackUiModel
import com.togetherly.feature.explore.model.ExploreQuestUiModel
import com.togetherly.feature.packdetails.model.ContentAccessState
import com.togetherly.feature.packdetails.model.PackDetailsUiState
import com.togetherly.feature.today.model.QuestCategoryUi
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
internal class PackDetailsScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun backButtonUsesAnAccessibleLabel() = runComposeUiTest {
        val actions = mutableListOf<PackDetailsAction>()
        setContent {
            TogetherlyTheme {
                PackDetailsScreen(
                    state = sampleState(),
                    onAction = actions::add,
                    onNavigateBack = {},
                )
            }
        }

        onNodeWithContentDescription("Back").performClick()

        assertEquals(listOf<PackDetailsAction>(PackDetailsAction.BackClicked), actions)
    }

    private fun sampleState(): PackDetailsUiState = PackDetailsUiState(
        isLoading = false,
        pack = ExplorePackUiModel(
            id = QuestPackId("pack-1"),
            title = "Quick Wins",
            description = "Short activities for busy days.",
            questCount = 2,
            durationLabel = UiText.Dynamic("10-20 minutes"),
            isPremium = false,
            locked = false,
            theme = QuestCategoryUi.DISCOVER,
        ),
        quests = persistentListOf(
            ExploreQuestUiModel(
                id = QuestId("quest-1"),
                title = "Backyard Scavenger Hunt",
                summary = "Find five hidden treasures together.",
                category = QuestCategoryUi.DISCOVER,
                durationLabel = UiText.Dynamic("20 minutes"),
                energyLabel = UiText.Dynamic("Moderate"),
                locationLabel = UiText.Dynamic("Outdoors"),
                isSaved = false,
                isPremium = false,
                locked = false,
            ),
        ),
        accessState = ContentAccessState.FREE,
        error = null,
    )
}

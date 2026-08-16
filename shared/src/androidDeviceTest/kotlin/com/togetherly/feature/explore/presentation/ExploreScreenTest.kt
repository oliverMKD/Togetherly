package com.togetherly.feature.explore.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.feature.explore.preview.emptySearchResultsExploreUiState
import com.togetherly.feature.explore.preview.errorExploreUiState
import com.togetherly.feature.explore.preview.freeFamilyExploreUiState
import com.togetherly.feature.explore.preview.lockedPremiumCardsExploreUiState
import com.togetherly.feature.today.model.QuestCategoryUi
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
internal class ExploreScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun typingInSearchEmitsSearchChanged() = runComposeUiTest {
        val actions = mutableListOf<ExploreAction>()
        setContent {
            TogetherlyTheme { ExploreScreen(state = freeFamilyExploreUiState(), onAction = actions::add) }
        }

        onNodeWithText("Search quests and packs").performTextInput("draw")

        assertEquals(listOf<ExploreAction>(ExploreAction.SearchChanged("draw")), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectingACategoryChipEmitsCategorySelected() = runComposeUiTest {
        val actions = mutableListOf<ExploreAction>()
        setContent {
            TogetherlyTheme { ExploreScreen(state = freeFamilyExploreUiState(), onAction = actions::add) }
        }

        onNodeWithText("Talk").performClick()

        assertEquals(listOf<ExploreAction>(ExploreAction.CategorySelected(QuestCategoryUi.TALK)), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun tappingTheAllChipEmitsCategoryCleared() = runComposeUiTest {
        val actions = mutableListOf<ExploreAction>()
        setContent {
            TogetherlyTheme { ExploreScreen(state = freeFamilyExploreUiState(), onAction = actions::add) }
        }

        onNodeWithText("All").performClick()

        assertEquals(listOf<ExploreAction>(ExploreAction.CategoryCleared), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun savingAQuestEmitsSaveClicked() = runComposeUiTest {
        val actions = mutableListOf<ExploreAction>()
        setContent {
            TogetherlyTheme { ExploreScreen(state = freeFamilyExploreUiState(), onAction = actions::add) }
        }

        onNodeWithTag(ExploreContentListTestTag).performScrollToNode(hasContentDescription("Saved. Tap to remove from saved quests."))
        onNodeWithContentDescription("Saved. Tap to remove from saved quests.").performClick()

        assertEquals(listOf<ExploreAction>(ExploreAction.SaveClicked(QuestId("quest-3"))), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun openingAPackEmitsPackClicked() = runComposeUiTest {
        val actions = mutableListOf<ExploreAction>()
        setContent {
            TogetherlyTheme { ExploreScreen(state = freeFamilyExploreUiState(), onAction = actions::add) }
        }

        // "Everyday Together" only appears in the "All packs" section — "Quick Wins" is
        // deliberately in both featuredPacks and packs (see the fixture's own KDoc-adjacent
        // comment), so it renders as two separate cards and can't be targeted by text alone.
        onNodeWithTag(ExploreContentListTestTag).performScrollToNode(hasText("Everyday Together"))
        onNodeWithText("Everyday Together").performClick()

        assertEquals(listOf<ExploreAction>(ExploreAction.PackClicked(QuestPackId("everyday-together"))), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun openingAFreeQuestEmitsQuestClicked() = runComposeUiTest {
        val actions = mutableListOf<ExploreAction>()
        setContent {
            TogetherlyTheme { ExploreScreen(state = freeFamilyExploreUiState(), onAction = actions::add) }
        }

        onNodeWithTag(ExploreContentListTestTag).performScrollToNode(hasText("Backyard Scavenger Hunt"))
        onNodeWithText("Backyard Scavenger Hunt").performClick()

        assertEquals(listOf<ExploreAction>(ExploreAction.QuestClicked(QuestId("quest-1"))), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun openingALockedPremiumQuestStillEmitsQuestClicked() = runComposeUiTest {
        val actions = mutableListOf<ExploreAction>()
        setContent {
            TogetherlyTheme { ExploreScreen(state = freeFamilyExploreUiState(), onAction = actions::add) }
        }

        onNodeWithTag(ExploreContentListTestTag).performScrollToNode(hasText("Draw a Shared Imaginary Creature"))
        onNodeWithText("Draw a Shared Imaginary Creature").performClick()

        assertEquals(listOf<ExploreAction>(ExploreAction.QuestClicked(QuestId("quest-2"))), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun filtersActionEmitsFiltersClicked() = runComposeUiTest {
        val actions = mutableListOf<ExploreAction>()
        setContent {
            TogetherlyTheme { ExploreScreen(state = freeFamilyExploreUiState(), onAction = actions::add) }
        }

        onNodeWithText("Filters").performClick()

        assertEquals(listOf<ExploreAction>(ExploreAction.FiltersClicked), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun lockedPremiumContentIsAnnouncedWithoutLookingDisabled() = runComposeUiTest {
        setContent {
            TogetherlyTheme { ExploreScreen(state = lockedPremiumCardsExploreUiState(), onAction = {}) }
        }

        onNodeWithTag(ExploreContentListTestTag).performScrollToNode(hasContentDescription("Family Plus quest, locked for your family. Tap to preview."))
        onNodeWithContentDescription("Family Plus quest, locked for your family. Tap to preview.").assertExists()
        // This fixture has several locked cards, each with its own "Family Plus" badge — just
        // confirming the real, visible text is what's announcing the premium state is the point.
        onAllNodesWithText("Family Plus").onFirst().assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun emptySearchResultsShowWarmCopy() = runComposeUiTest {
        setContent {
            TogetherlyTheme { ExploreScreen(state = emptySearchResultsExploreUiState(), onAction = {}) }
        }

        onNodeWithText("No quests found").assertIsDisplayed()
        onNodeWithText("Try another word or clear your filters.").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun errorStateShowsMessageAndRetry() = runComposeUiTest {
        val actions = mutableListOf<ExploreAction>()
        setContent {
            TogetherlyTheme { ExploreScreen(state = errorExploreUiState(), onAction = actions::add) }
        }

        onNodeWithText("Something went wrong. Please try again.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()

        assertEquals(listOf<ExploreAction>(ExploreAction.RetryClicked), actions)
    }
}

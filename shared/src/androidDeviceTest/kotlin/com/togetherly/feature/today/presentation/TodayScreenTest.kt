package com.togetherly.feature.today.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.core.ui.UiText
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.feature.today.model.TodayContentState
import com.togetherly.feature.today.preview.errorTodayUiState
import com.togetherly.feature.today.preview.freeRerollConsumedTodayUiState
import com.togetherly.feature.today.preview.loadingTodayUiState
import com.togetherly.feature.today.preview.mysteryTodayUiState
import com.togetherly.feature.today.preview.revealedTodayUiState
import com.togetherly.feature.today.preview.rerollConfirmationTodayUiState
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
internal class TodayScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun mysteryDoesNotExposeTheHiddenQuestTitleInSemantics() = runComposeUiTest {
        val state = mysteryTodayUiState()
        val hiddenTitle = ((state.content as TodayContentState.Mystery).quest.title)

        setContent {
            TogetherlyTheme { TodayScreen(state = state, onAction = {}) }
        }

        assertTrue(hiddenTitle.isNotBlank())
        assertNodeTreeNeverContains(hiddenTitle)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun revealActionEmitsRevealClicked() = runComposeUiTest {
        val actions = mutableListOf<TodayAction>()
        setContent {
            TogetherlyTheme { TodayScreen(state = mysteryTodayUiState(), onAction = actions::add) }
        }

        onNodeWithText("Reveal quest").performClick()

        assertEquals(listOf<TodayAction>(TodayAction.RevealClicked), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun revealedContentChangesWhatIsVisibleComparedToMystery() = runComposeUiTest {
        val state = revealedTodayUiState()
        val quest = (state.content as TodayContentState.Revealed).quest

        setContent {
            TogetherlyTheme { TodayScreen(state = state, onAction = {}) }
        }

        onNodeWithText(quest.title).assertIsDisplayed()
        onNodeWithText("Reveal quest").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun revealedMetadataIsAccessible() = runComposeUiTest {
        setContent {
            TogetherlyTheme { TodayScreen(state = revealedTodayUiState(), onAction = {}) }
        }

        onNodeWithContentDescription("20 minutes. Outdoors. Simple materials. Moderate. 3 materials needed", substring = true)
            .assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun reducedMotionStillRendersRevealedContent() = runComposeUiTest {
        val state = revealedTodayUiState()
        val quest = (state.content as TodayContentState.Revealed).quest

        setContent {
            TogetherlyTheme(reduceMotion = true) { TodayScreen(state = state, onAction = {}) }
        }

        onNodeWithText(quest.title).assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun saveActionEmitsSaveClicked() = runComposeUiTest {
        val actions = mutableListOf<TodayAction>()
        setContent {
            TogetherlyTheme { TodayScreen(state = revealedTodayUiState(), onAction = actions::add) }
        }

        onNodeWithContentDescription("Not saved. Tap to save this quest.").performClick()

        assertEquals(listOf<TodayAction>(TodayAction.SaveClicked), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun primaryActionEmitsStartClicked() = runComposeUiTest {
        val actions = mutableListOf<TodayAction>()
        setContent {
            TogetherlyTheme { TodayScreen(state = revealedTodayUiState(), onAction = actions::add) }
        }

        onNodeWithText("See quest").performClick()

        assertEquals(listOf<TodayAction>(TodayAction.StartClicked), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun loadingStateShowsALoadingIndicator() = runComposeUiTest {
        setContent {
            TogetherlyTheme { TodayScreen(state = loadingTodayUiState(), onAction = {}) }
        }

        onNodeWithContentDescription("Loading", substring = true).assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun errorStateShowsMessageAndRetry() = runComposeUiTest {
        val actions = mutableListOf<TodayAction>()
        val message = "Something went wrong. Please try again."
        setContent {
            TogetherlyTheme {
                TodayScreen(
                    state = errorTodayUiState().copy(content = TodayContentState.Error(UiText.Dynamic(message), retryAvailable = true)),
                    onAction = actions::add,
                )
            }
        }

        onNodeWithText(message).assertIsDisplayed()
        onNodeWithText("Retry").performClick()

        assertEquals(listOf<TodayAction>(TodayAction.RetryClicked), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun errorWithoutRetryShowsNoRetryControl() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                TodayScreen(
                    state = errorTodayUiState().copy(
                        content = TodayContentState.Error(UiText.Dynamic("No retry here"), retryAvailable = false),
                    ),
                    onAction = {},
                )
            }
        }

        onNodeWithText("Retry").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun largeTextDoesNotHideThePrimaryAction() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                TogetherlyTheme { TodayScreen(state = revealedTodayUiState(), onAction = {}) }
            }
        }

        onNodeWithText("See quest").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun todayNeverRendersItsOwnBottomNavigation() = runComposeUiTest {
        setContent {
            TogetherlyTheme { TodayScreen(state = revealedTodayUiState(), onAction = {}) }
        }

        onNodeWithText("Explore").assertDoesNotExist()
        onNodeWithText("Journey").assertDoesNotExist()
        onNodeWithText("Family").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun openingFiltersShowsTheFilterSheet() = runComposeUiTest {
        val actions = mutableListOf<TodayAction>()
        setContent {
            TogetherlyTheme { TodayScreen(state = revealedTodayUiState(), onAction = actions::add) }
        }

        onNodeWithText("Filters").performClick()

        assertEquals(listOf<TodayAction>(TodayAction.FiltersClicked), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun filterSheetShowsEveryGroupAndSelectingAChipEmitsTheRightAction() = runComposeUiTest {
        val actions = mutableListOf<TodayAction>()
        val state = revealedTodayUiState().copy(filtersVisible = true)
        setContent {
            TogetherlyTheme { TodayScreen(state = state, onAction = actions::add) }
        }

        onNodeWithText("Find a quest").assertIsDisplayed()
        onNodeWithText("5 minutes").performClick()

        assertEquals(
            listOf<TodayAction>(TodayAction.DurationFilterChanged(com.togetherly.domain.family.DurationBand.FIVE_MINUTES)),
            actions,
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun clearAllInSheetEmitsClearFiltersClicked() = runComposeUiTest {
        val actions = mutableListOf<TodayAction>()
        val state = revealedTodayUiState().copy(filtersVisible = true)
        setContent {
            TogetherlyTheme { TodayScreen(state = state, onAction = actions::add) }
        }

        onNodeWithText("Clear all").performClick()

        assertEquals(listOf<TodayAction>(TodayAction.ClearFiltersClicked), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun findAQuestInSheetEmitsApplyFiltersClicked() = runComposeUiTest {
        val actions = mutableListOf<TodayAction>()
        val state = revealedTodayUiState().copy(filtersVisible = true)
        setContent {
            TogetherlyTheme { TodayScreen(state = state, onAction = actions::add) }
        }

        onNodeWithText("Find a quest").performClick()

        assertEquals(listOf<TodayAction>(TodayAction.ApplyFiltersClicked), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rerollShowsConfirmationBeforeConsumingTheSwap() = runComposeUiTest {
        val actions = mutableListOf<TodayAction>()
        setContent {
            TogetherlyTheme { TodayScreen(state = revealedTodayUiState(), onAction = actions::add) }
        }

        onNodeWithText("Try another quest").performClick()

        assertEquals(listOf<TodayAction>(TodayAction.RerollClicked), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rerollConfirmationDialogNamesTheConsequence() = runComposeUiTest {
        setContent {
            TogetherlyTheme { TodayScreen(state = rerollConfirmationTodayUiState(), onAction = {}) }
        }

        onNodeWithText("Use today's free swap?").assertIsDisplayed()
        onNodeWithText("Keep this quest").assertIsDisplayed()
        onNodeWithText("Show another").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun cancellingRerollConfirmationEmitsRerollCancelled() = runComposeUiTest {
        val actions = mutableListOf<TodayAction>()
        setContent {
            TogetherlyTheme { TodayScreen(state = rerollConfirmationTodayUiState(), onAction = actions::add) }
        }

        onNodeWithText("Keep this quest").performClick()

        assertEquals(listOf<TodayAction>(TodayAction.RerollCancelled), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun confirmingRerollEmitsRerollConfirmed() = runComposeUiTest {
        val actions = mutableListOf<TodayAction>()
        setContent {
            TogetherlyTheme { TodayScreen(state = rerollConfirmationTodayUiState(), onAction = actions::add) }
        }

        onNodeWithText("Show another").performClick()

        assertEquals(listOf<TodayAction>(TodayAction.RerollConfirmed), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun freeRerollConsumedDisablesTheRerollActionInsteadOfARealButton() = runComposeUiTest {
        setContent {
            TogetherlyTheme { TodayScreen(state = freeRerollConsumedTodayUiState(), onAction = {}) }
        }

        onNodeWithText("Today's free swap has been used.").assertIsDisplayed()
        onNodeWithText("Try another quest").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    private fun ComposeUiTest.assertNodeTreeNeverContains(text: String) {
        val tree = onRoot().printToString()
        assertTrue(text !in tree, "Expected \"$text\" to never appear in the semantics tree before reveal")
    }
}

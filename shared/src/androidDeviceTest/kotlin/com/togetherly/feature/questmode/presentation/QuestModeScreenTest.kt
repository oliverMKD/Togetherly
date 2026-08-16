package com.togetherly.feature.questmode.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.feature.questmode.preview.abandonConfirmationQuestModeUiState
import com.togetherly.feature.questmode.preview.exitConfirmationQuestModeUiState
import com.togetherly.feature.questmode.preview.finishedTimerQuestModeUiState
import com.togetherly.feature.questmode.preview.runningTimerQuestModeUiState
import com.togetherly.feature.questmode.preview.untimedQuestModeUiState
import com.togetherly.feature.questmode.preview.withHintsQuestModeUiState
import com.togetherly.feature.questmode.preview.withSafetyNoteQuestModeUiState
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
internal class QuestModeScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun bottomNavigationIsAbsent() = runComposeUiTest {
        setContent {
            TogetherlyTheme { QuestModeScreen(state = runningTimerQuestModeUiState(), onAction = {}) }
        }

        onNodeWithText("Explore").assertDoesNotExist()
        onNodeWithText("Journey").assertDoesNotExist()
        onNodeWithText("Family").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun instructionsAreOrdered() = runComposeUiTest {
        setContent {
            TogetherlyTheme { QuestModeScreen(state = untimedQuestModeUiState(), onAction = {}) }
        }

        onNodeWithText("1.", substring = true).assertIsDisplayed()
        onNodeWithText("Hide five small objects in the yard.").assertIsDisplayed()
        onNodeWithText("3.", substring = true).assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun untimedQuestHidesTheTimer() = runComposeUiTest {
        setContent {
            TogetherlyTheme { QuestModeScreen(state = untimedQuestModeUiState(), onAction = {}) }
        }

        onNodeWithText("Time's up").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun runningTimerIsVisible() = runComposeUiTest {
        setContent {
            TogetherlyTheme { QuestModeScreen(state = runningTimerQuestModeUiState(), onAction = {}) }
        }

        onNodeWithText("9:05").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun finishedTimerShowsTimesUpCopy() = runComposeUiTest {
        setContent {
            TogetherlyTheme { QuestModeScreen(state = finishedTimerQuestModeUiState(), onAction = {}) }
        }

        onNodeWithText("Time's up").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun hintsExpandAndCollapse() = runComposeUiTest {
        val actions = mutableListOf<QuestModeAction>()
        setContent {
            TogetherlyTheme { QuestModeScreen(state = withHintsQuestModeUiState(), onAction = actions::add) }
        }

        // QuestModeHints renders each hint as "•  $hint", the same bullet-prefixed format
        // QuestDetailScreen uses for materials/hints — the bare hint text alone never matches.
        onNodeWithText("•  Try the garden.").assertIsDisplayed()
        onNodeWithText("Need an idea?").performClick()

        assertEquals(listOf<QuestModeAction>(QuestModeAction.HintsToggled), actions)
    }

    /**
     * Split into two tests rather than one calling `setContent` twice — this API only supports
     * setting content once per [runComposeUiTest]; a second call throws "has already set content".
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun safetyNoteIsAbsentWithoutOne() = runComposeUiTest {
        setContent {
            TogetherlyTheme { QuestModeScreen(state = untimedQuestModeUiState(), onAction = {}) }
        }
        onNodeWithText("Safety note").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun safetyNoteIsShownWhenPresent() = runComposeUiTest {
        setContent {
            TogetherlyTheme { QuestModeScreen(state = withSafetyNoteQuestModeUiState(), onAction = {}) }
        }
        onNodeWithText("Safety note").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun phoneDownActionEmitsPhoneDownClicked() = runComposeUiTest {
        val actions = mutableListOf<QuestModeAction>()
        setContent {
            TogetherlyTheme { QuestModeScreen(state = untimedQuestModeUiState(), onAction = actions::add) }
        }

        onNodeWithText("Put the phone down").performClick()

        assertEquals(listOf<QuestModeAction>(QuestModeAction.PhoneDownClicked), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun completeActionEmitsCompleteClicked() = runComposeUiTest {
        val actions = mutableListOf<QuestModeAction>()
        setContent {
            TogetherlyTheme { QuestModeScreen(state = untimedQuestModeUiState(), onAction = actions::add) }
        }

        onNodeWithText("We did it").performClick()

        assertEquals(listOf<QuestModeAction>(QuestModeAction.CompleteClicked), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun exitConfirmationShowsAllThreeActions() = runComposeUiTest {
        setContent {
            TogetherlyTheme { QuestModeScreen(state = exitConfirmationQuestModeUiState(), onAction = {}) }
        }

        onNodeWithText("Keep this quest in progress?").assertIsDisplayed()
        onNodeWithText("Keep for later").assertIsDisplayed()
        onNodeWithText("Continue quest").assertIsDisplayed()
        onNodeWithText("Abandon quest").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun abandonConfirmationRequiresASecondExplicitChoice() = runComposeUiTest {
        setContent {
            TogetherlyTheme { QuestModeScreen(state = abandonConfirmationQuestModeUiState(), onAction = {}) }
        }

        onNodeWithText("Abandon this quest?").assertIsDisplayed()
        onNodeWithText("Cancel").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun largeTextKeepsTheCompleteActionReachable() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                TogetherlyTheme { QuestModeScreen(state = untimedQuestModeUiState(), onAction = {}) }
            }
        }

        onNodeWithText("We did it").assertExists()
    }
}

package com.togetherly.feature.questdetail.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

private fun sampleDetail() = QuestDetailUi(
    id = QuestId("quest-1"),
    title = "Backyard Scavenger Hunt",
    summary = "Find five hidden treasures together.",
    category = QuestCategoryUi.DISCOVER,
    durationLabel = DurationBand.TWENTY_MINUTES.label(),
    locationLabel = QuestLocation.OUTDOOR.label(),
    preparationLabel = PreparationLevel.SIMPLE_MATERIALS.label(),
    energyLabel = EnergyLevel.MODERATE.label(),
    ageBandLabels = emptyList(),
    isPremium = false,
    packTitle = null,
    instructions = listOf("Hide five objects.", "Give clues."),
    materials = listOf("Small toys"),
    hints = listOf("Try the garden."),
    safetyNote = "Adult supervision required.",
)

@RunWith(AndroidJUnit4::class)
internal class QuestDetailScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun instructionsAreShownAsANumberedList() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                QuestDetailScreen(
                    state = QuestDetailUiState.Content(quest = sampleDetail(), isSaved = false, isStarting = false),
                    onAction = {},
                )
            }
        }

        onNodeWithText("1.  Hide five objects.").assertIsDisplayed()
        onNodeWithText("2.  Give clues.").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun optionalSectionsAreHiddenWhenEmpty() = runComposeUiTest {
        val quest = sampleDetail().copy(materials = emptyList(), hints = emptyList(), safetyNote = null)
        setContent {
            TogetherlyTheme {
                QuestDetailScreen(state = QuestDetailUiState.Content(quest = quest, isSaved = false, isStarting = false), onAction = {})
            }
        }

        onNodeWithText("What you'll need").assertDoesNotExist()
        onNodeWithText("Hints").assertDoesNotExist()
        onNodeWithText("Safety note").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun backActionEmitsBackClicked() = runComposeUiTest {
        val actions = mutableListOf<QuestDetailAction>()
        setContent {
            TogetherlyTheme {
                QuestDetailScreen(
                    state = QuestDetailUiState.Content(quest = sampleDetail(), isSaved = false, isStarting = false),
                    onAction = actions::add,
                )
            }
        }

        onNodeWithContentDescription("Back").performClick()

        assertEquals(listOf<QuestDetailAction>(QuestDetailAction.BackClicked), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun startActionEmitsStartClicked() = runComposeUiTest {
        val actions = mutableListOf<QuestDetailAction>()
        setContent {
            TogetherlyTheme {
                QuestDetailScreen(
                    state = QuestDetailUiState.Content(quest = sampleDetail(), isSaved = false, isStarting = false),
                    onAction = actions::add,
                )
            }
        }

        onNodeWithText("Start quest").performClick()

        assertEquals(listOf<QuestDetailAction>(QuestDetailAction.StartClicked), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun saveActionEmitsSaveClicked() = runComposeUiTest {
        val actions = mutableListOf<QuestDetailAction>()
        setContent {
            TogetherlyTheme {
                QuestDetailScreen(
                    state = QuestDetailUiState.Content(quest = sampleDetail(), isSaved = false, isStarting = false),
                    onAction = actions::add,
                )
            }
        }

        onNodeWithContentDescription("Not saved. Tap to save this quest.").performClick()

        assertEquals(listOf<QuestDetailAction>(QuestDetailAction.SaveClicked), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun conflictDialogShowsBothActions() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                QuestDetailScreen(
                    state = QuestDetailUiState.Content(quest = sampleDetail(), isSaved = false, isStarting = false, activeSessionConflict = true),
                    onAction = {},
                )
            }
        }

        onNodeWithText("Another quest is still in progress").assertIsDisplayed()
        onNodeWithText("Replace and start this quest").assertIsDisplayed()
        onNodeWithText("Cancel").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun replaceActionEmitsReplaceActiveSessionConfirmed() = runComposeUiTest {
        val actions = mutableListOf<QuestDetailAction>()
        setContent {
            TogetherlyTheme {
                QuestDetailScreen(
                    state = QuestDetailUiState.Content(quest = sampleDetail(), isSaved = false, isStarting = false, activeSessionConflict = true),
                    onAction = actions::add,
                )
            }
        }

        onNodeWithText("Replace and start this quest").performClick()

        assertEquals(listOf<QuestDetailAction>(QuestDetailAction.ReplaceActiveSessionConfirmed), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun cancelActionEmitsKeepActiveSessionClicked() = runComposeUiTest {
        val actions = mutableListOf<QuestDetailAction>()
        setContent {
            TogetherlyTheme {
                QuestDetailScreen(
                    state = QuestDetailUiState.Content(quest = sampleDetail(), isSaved = false, isStarting = false, activeSessionConflict = true),
                    onAction = actions::add,
                )
            }
        }

        onNodeWithText("Cancel").performClick()

        assertEquals(listOf<QuestDetailAction>(QuestDetailAction.KeepActiveSessionClicked), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun errorStateShowsMessageAndRetry() = runComposeUiTest {
        val actions = mutableListOf<QuestDetailAction>()
        setContent {
            TogetherlyTheme {
                QuestDetailScreen(state = QuestDetailUiState.Error(UiText.Dynamic("Not found.")), onAction = actions::add)
            }
        }

        onNodeWithText("Not found.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()

        assertEquals(listOf<QuestDetailAction>(QuestDetailAction.RetryClicked), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun lockedPremiumPreviewShowsTheFamilyPlusExplanationAndUnlockAction() = runComposeUiTest {
        val quest = sampleDetail().copy(isPremium = true, instructions = emptyList(), hints = emptyList())
        setContent {
            TogetherlyTheme {
                QuestDetailScreen(
                    state = QuestDetailUiState.Content(quest = quest, isSaved = false, isStarting = false, locked = true),
                    onAction = {},
                )
            }
        }

        onNodeWithText("This quest is part of Family Plus.").assertIsDisplayed()
        onNodeWithText("Unlock the full activity and more ways to spend meaningful time together.").assertIsDisplayed()
        onNodeWithText("Unlock with Family Plus").assertIsDisplayed()
        onNodeWithText("Start quest").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun lockedPremiumPreviewNeverRendersInstructions() = runComposeUiTest {
        val quest = sampleDetail().copy(isPremium = true, instructions = emptyList(), hints = emptyList())
        setContent {
            TogetherlyTheme {
                QuestDetailScreen(
                    state = QuestDetailUiState.Content(quest = quest, isSaved = false, isStarting = false, locked = true),
                    onAction = {},
                )
            }
        }

        onNodeWithText("Instructions").assertDoesNotExist()
        onNodeWithText("1.  Hide five objects.").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun unlockActionEmitsUnlockClicked() = runComposeUiTest {
        val actions = mutableListOf<QuestDetailAction>()
        val quest = sampleDetail().copy(isPremium = true, instructions = emptyList(), hints = emptyList())
        setContent {
            TogetherlyTheme {
                QuestDetailScreen(
                    state = QuestDetailUiState.Content(quest = quest, isSaved = false, isStarting = false, locked = true),
                    onAction = actions::add,
                )
            }
        }

        onNodeWithText("Unlock with Family Plus").performClick()

        assertEquals(listOf<QuestDetailAction>(QuestDetailAction.UnlockClicked), actions)
    }
}

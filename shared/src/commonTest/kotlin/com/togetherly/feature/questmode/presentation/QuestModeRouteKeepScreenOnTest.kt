package com.togetherly.feature.questmode.presentation

import com.togetherly.core.ui.UiText
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.quest.QuestId
import com.togetherly.feature.questmode.model.InstructionStepUi
import com.togetherly.feature.questmode.model.QuestModeContentUi
import com.togetherly.feature.questmode.model.QuestTimerUi
import com.togetherly.feature.today.model.QuestCategoryUi
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun contentUi(timer: QuestTimerUi, keepScreenOnRequested: Boolean) = QuestModeContentUi(
    completionId = CompletionId("completion-1"),
    questId = QuestId("quest-1"),
    title = "Title",
    category = QuestCategoryUi.DISCOVER,
    instructions = persistentListOf(InstructionStepUi(1, "Step")),
    hints = persistentListOf(),
    safetyNote = null,
    timer = timer,
    phoneDownSupported = true,
    keepScreenOnRequested = keepScreenOnRequested,
)

/**
 * [shouldKeepScreenOn] is a plain function specifically so these cases don't need a Compose UI
 * test environment — see that function's own KDoc.
 */
class QuestModeRouteKeepScreenOnTest {

    @Test
    fun enabledWhenTimedRunningAndNotPhoneDown() {
        val state = QuestModeUiState.Content(
            quest = contentUi(timer = QuestTimerUi.Running("9:00", 0f), keepScreenOnRequested = true),
        )

        assertTrue(shouldKeepScreenOn(state))
    }

    @Test
    fun disabledWhenPhoneDownIsActive() {
        val state = QuestModeUiState.Content(
            quest = contentUi(timer = QuestTimerUi.Running("9:00", 0f), keepScreenOnRequested = true),
            phoneDown = true,
        )

        assertFalse(shouldKeepScreenOn(state))
    }

    @Test
    fun disabledForAnUntimedQuest() {
        val state = QuestModeUiState.Content(
            quest = contentUi(timer = QuestTimerUi.Hidden, keepScreenOnRequested = false),
        )

        assertFalse(shouldKeepScreenOn(state))
    }

    @Test
    fun disabledOnceTheTimerHasFinished() {
        val state = QuestModeUiState.Content(
            quest = contentUi(timer = QuestTimerUi.Finished, keepScreenOnRequested = true),
        )

        assertFalse(shouldKeepScreenOn(state))
    }

    @Test
    fun disabledWhenTheQuestDidNotRequestIt() {
        val state = QuestModeUiState.Content(
            quest = contentUi(timer = QuestTimerUi.Running("9:00", 0f), keepScreenOnRequested = false),
        )

        assertFalse(shouldKeepScreenOn(state))
    }

    @Test
    fun disabledWhileLoading() {
        assertFalse(shouldKeepScreenOn(QuestModeUiState.Loading))
    }

    @Test
    fun disabledOnError() {
        assertFalse(shouldKeepScreenOn(QuestModeUiState.Error(UiText.Dynamic("error"), canRetry = true, canClose = true)))
    }
}

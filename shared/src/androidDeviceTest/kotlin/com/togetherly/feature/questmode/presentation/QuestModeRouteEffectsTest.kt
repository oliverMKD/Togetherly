package com.togetherly.feature.questmode.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.core.feedback.QuestFeedbackController
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.domain.completion.CompletionId
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
internal class QuestModeRouteEffectsTest {

    private class RecordingFeedbackController : QuestFeedbackController {
        var timerFinishedCalls = 0
        var questCompletedCalls = 0

        override fun timerFinished() {
            timerFinishedCalls++
        }

        override fun questCompleted() {
            questCompletedCalls++
        }
    }

    @Test
    fun terminalEventsAreNotRepeatedAfterTheRouteIsRecreated() = runComposeUiTest {
        val events = MutableSharedFlow<QuestModeEvent>(extraBufferCapacity = 4)
        val feedbackController = RecordingFeedbackController()
        var timerFinishedCalls = 0
        var completionNavigations = 0
        var routeVisible by mutableStateOf(true)

        @Composable
        fun content() {
            val saveableStateHolder = rememberSaveableStateHolder()
            if (routeVisible) {
                saveableStateHolder.SaveableStateProvider("completion-1") {
                    TogetherlyTheme {
                        QuestModeRouteEffects(
                            completionId = CompletionId("completion-1"),
                            events = events,
                            feedbackController = feedbackController,
                            onNavigateBack = {},
                            onNavigateToToday = {},
                            onNavigateToCompletion = { completionNavigations++ },
                            onTimerFinished = { timerFinishedCalls++ },
                        )
                    }
                }
            }
        }

        setContent { content() }
        events.tryEmit(QuestModeEvent.TimerFinished)
        waitForIdle()
        assertEquals(1, feedbackController.timerFinishedCalls)
        assertEquals(1, timerFinishedCalls)

        events.tryEmit(QuestModeEvent.NavigateToCompletion(CompletionId("completion-1")))
        waitForIdle()
        assertEquals(1, feedbackController.questCompletedCalls)
        assertEquals(1, completionNavigations)

        runOnIdle { routeVisible = false }
        waitForIdle()
        runOnIdle { routeVisible = true }
        waitForIdle()

        events.tryEmit(QuestModeEvent.TimerFinished)
        waitForIdle()
        assertEquals(1, feedbackController.timerFinishedCalls)
        assertEquals(1, timerFinishedCalls)

        events.tryEmit(QuestModeEvent.NavigateToCompletion(CompletionId("completion-1")))
        waitForIdle()
        assertEquals(1, feedbackController.questCompletedCalls)
        assertEquals(1, completionNavigations)
    }
}

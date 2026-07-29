package com.togetherly.feature.questmode.presentation

import app.cash.turbine.test
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.FakeProductAnalytics
import com.togetherly.core.telemetry.QuestAbandoned
import com.togetherly.core.telemetry.QuestCompleted
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.repository.FakeQuestSessionTransaction
import com.togetherly.domain.completion.usecase.CompleteQuest
import com.togetherly.domain.completion.usecase.ResolveCompletionTransition
import com.togetherly.domain.completion.validActiveQuestSession
import com.togetherly.domain.purchase.repository.FakeCustomerAttributesRepository
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestTimer
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.validFamilyQuest
import com.togetherly.domain.questmode.DefaultQuestCountdownEngine
import com.togetherly.domain.questmode.QuestTimerPolicy
import com.togetherly.domain.questmode.usecase.AbandonQuest
import com.togetherly.domain.questmode.usecase.LoadQuestMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")

@OptIn(ExperimentalCoroutinesApi::class)
class QuestModeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private inner class Fixture(
        val completionId: CompletionId,
        val completionRepository: FakeCompletionRepository,
        val questRepository: FakeQuestRepository,
        val clock: TestAppClock,
        val analytics: FakeProductAnalytics = FakeProductAnalytics().apply { setCollectionEnabled(true) },
        val customerAttributesRepository: FakeCustomerAttributesRepository = FakeCustomerAttributesRepository(),
    ) {
        val transaction = FakeQuestSessionTransaction(completionRepository)
        val timerPolicy = QuestTimerPolicy()
        val loadQuestMode = LoadQuestMode(completionRepository, questRepository, timerPolicy, clock)
        val countdownEngine = DefaultQuestCountdownEngine(clock, timerPolicy, TestAppDispatchers(testDispatcher))
        val completeQuest = CompleteQuest(completionRepository, transaction, clock)
        val abandonQuest = AbandonQuest(completionRepository)
        val resolveCompletionTransition = ResolveCompletionTransition(completionRepository)
        val viewModel = QuestModeViewModel(
            completionId, loadQuestMode, countdownEngine, completeQuest, abandonQuest, resolveCompletionTransition, analytics, customerAttributesRepository,
        )
    }

    private suspend fun fixture(
        withTimer: kotlin.time.Duration? = null,
        questId: QuestId = QuestId("quest-1"),
        completionId: CompletionId = CompletionId("completion-1"),
    ): Fixture {
        val quest = validFamilyQuest(
            id = questId,
            timer = withTimer?.let { QuestTimer(duration = it, keepScreenOn = false) },
        )
        val session = validActiveQuestSession(completionId = completionId, questId = questId, startedAt = NOW)
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(session) }
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }
        return Fixture(completionId, completionRepository, questRepository, TestAppClock(NOW))
    }

    @Test
    fun loadIsIdempotent() = runTest {
        val fixture = fixture()

        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        val afterFirst = fixture.viewModel.uiState.value

        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(afterFirst, fixture.viewModel.uiState.value)
    }

    @Test
    fun validContentLoadsSuccessfully() = runTest {
        val fixture = fixture()

        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value as QuestModeUiState.Content
        assertEquals(QuestId("quest-1"), content.quest.questId)
    }

    @Test
    fun missingSessionShowsCloseableError() = runTest {
        val completionRepository = FakeCompletionRepository()
        val fixture = Fixture(CompletionId("completion-1"), completionRepository, FakeQuestRepository(), TestAppClock(NOW))

        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val error = fixture.viewModel.uiState.value as QuestModeUiState.Error
        assertTrue(error.canClose)
        assertFalse(error.canRetry)
    }

    @Test
    fun sessionMismatchShowsCloseableError() = runTest {
        val session = validActiveQuestSession(completionId = CompletionId("completion-current"))
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(session) }
        val fixture = Fixture(CompletionId("completion-stale"), completionRepository, FakeQuestRepository(), TestAppClock(NOW))

        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val error = fixture.viewModel.uiState.value as QuestModeUiState.Error
        assertTrue(error.canClose)
        assertFalse(error.canRetry)
    }

    @Test
    fun missingQuestShowsCloseableError() = runTest {
        val session = validActiveQuestSession(completionId = CompletionId("completion-1"), questId = QuestId("quest-1"))
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(session) }
        val fixture = Fixture(CompletionId("completion-1"), completionRepository, FakeQuestRepository(), TestAppClock(NOW))

        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val error = fixture.viewModel.uiState.value as QuestModeUiState.Error
        assertTrue(error.canClose)
        assertFalse(error.canRetry)
    }

    @Test
    fun repositoryFailureAllowsRetry() = runTest {
        val completionRepository = FakeCompletionRepository()
        completionRepository.setNextError(AppError.Storage(StorageError.READ_FAILED))
        val fixture = Fixture(CompletionId("completion-1"), completionRepository, FakeQuestRepository(), TestAppClock(NOW))

        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val error = fixture.viewModel.uiState.value as QuestModeUiState.Error
        assertTrue(error.canRetry)
    }

    @Test
    fun timerUpdatesPreserveOtherContentState() = runTest {
        // A short timer, deliberately driven all the way to Finished before the test ends: the
        // countdown job is still a live `viewModelScope` coroutine otherwise, and `runTest`'s own
        // implicit end-of-test drain waits for every launched coroutine to finish — an
        // undrained/never-completing countdown loop would hang the test itself, not just this
        // one assertion.
        val fixture = fixture(withTimer = 3.seconds)
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.runCurrent()
        fixture.viewModel.onAction(QuestModeAction.HintsToggled)
        fixture.viewModel.onAction(QuestModeAction.PhoneDownClicked)

        fixture.clock.advanceTo(NOW + 1.seconds)
        testDispatcher.scheduler.advanceTimeBy(1.seconds)
        testDispatcher.scheduler.runCurrent()

        val content = fixture.viewModel.uiState.value as QuestModeUiState.Content
        assertTrue(content.hintsExpanded)
        assertTrue(content.phoneDown)

        fixture.clock.advanceTo(NOW + 3.seconds)
        testDispatcher.scheduler.advanceTimeBy(2.seconds)
        testDispatcher.scheduler.runCurrent()
    }

    @Test
    fun timerFinishedEventFiresExactlyOnce() = runTest {
        val fixture = fixture(withTimer = 2.seconds)

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
            testDispatcher.scheduler.runCurrent()

            fixture.clock.advanceTo(NOW + 2.seconds)
            testDispatcher.scheduler.advanceTimeBy(1.seconds)
            testDispatcher.scheduler.runCurrent()

            assertEquals(QuestModeEvent.TimerFinished, awaitItem())

            fixture.clock.advanceTo(NOW + 5.seconds)
            testDispatcher.scheduler.advanceTimeBy(5.seconds)
            testDispatcher.scheduler.runCurrent()

            expectNoEvents()
        }
    }

    @Test
    fun timerFinishingDoesNotCompleteTheQuest() = runTest {
        val fixture = fixture(withTimer = 2.seconds)
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.runCurrent()

        fixture.clock.advanceTo(NOW + 2.seconds)
        testDispatcher.scheduler.advanceTimeBy(1.seconds)
        testDispatcher.scheduler.runCurrent()

        val activeSessionResult = fixture.completionRepository.getActiveSession()
        assertTrue((activeSessionResult as DataResult.Success).value != null)
    }

    @Test
    fun phoneDownTogglesOnAndOff() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(QuestModeAction.PhoneDownClicked)
        assertTrue((fixture.viewModel.uiState.value as QuestModeUiState.Content).phoneDown)

        fixture.viewModel.onAction(QuestModeAction.ExitPhoneDownClicked)
        assertFalse((fixture.viewModel.uiState.value as QuestModeUiState.Content).phoneDown)
    }

    @Test
    fun hintsToggleOnAndOff() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(QuestModeAction.HintsToggled)
        assertTrue((fixture.viewModel.uiState.value as QuestModeUiState.Content).hintsExpanded)

        fixture.viewModel.onAction(QuestModeAction.HintsToggled)
        assertFalse((fixture.viewModel.uiState.value as QuestModeUiState.Content).hintsExpanded)
    }

    @Test
    fun backShowsExitConfirmation() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(QuestModeAction.BackClicked)

        assertTrue((fixture.viewModel.uiState.value as QuestModeUiState.Content).showExitConfirmation)
    }

    @Test
    fun keepInProgressRetainsSessionAndNavigatesBack() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(QuestModeAction.BackClicked)

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(QuestModeAction.KeepInProgressClicked)
            assertEquals(QuestModeEvent.NavigateBack, awaitItem())
        }
        val activeSessionResult = fixture.completionRepository.getActiveSession()
        assertTrue((activeSessionResult as DataResult.Success).value != null)
    }

    @Test
    fun dialogDismissedHidesBothConfirmationsWithoutTouchingTheSession() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(QuestModeAction.BackClicked)
        fixture.viewModel.onAction(QuestModeAction.AbandonClicked)

        fixture.viewModel.onAction(QuestModeAction.DialogDismissed)

        val content = fixture.viewModel.uiState.value as QuestModeUiState.Content
        assertFalse(content.showExitConfirmation)
        assertFalse(content.showAbandonConfirmation)
        val activeSessionResult = fixture.completionRepository.getActiveSession()
        assertTrue((activeSessionResult as DataResult.Success).value != null)
    }

    @Test
    fun closeClickedNavigatesBackWithoutTouchingTheSession() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(QuestModeAction.CloseClicked)
            assertEquals(QuestModeEvent.NavigateBack, awaitItem())
        }
        val activeSessionResult = fixture.completionRepository.getActiveSession()
        assertTrue((activeSessionResult as DataResult.Success).value != null)
    }

    @Test
    fun abandonRequiresASecondConfirmation() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(QuestModeAction.BackClicked)

        fixture.viewModel.onAction(QuestModeAction.AbandonClicked)

        val content = fixture.viewModel.uiState.value as QuestModeUiState.Content
        assertFalse(content.showExitConfirmation)
        assertTrue(content.showAbandonConfirmation)
    }

    @Test
    fun abandonSuccessNavigatesToToday() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(QuestModeAction.BackClicked)
        fixture.viewModel.onAction(QuestModeAction.AbandonClicked)

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(QuestModeAction.AbandonConfirmed)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(QuestModeEvent.NavigateToToday, awaitItem())
        }
        val activeSessionResult = fixture.completionRepository.getActiveSession()
        assertEquals(null, (activeSessionResult as DataResult.Success).value)
    }

    @Test
    fun abandonFailureKeepsTheSession() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(QuestModeAction.BackClicked)
        fixture.viewModel.onAction(QuestModeAction.AbandonClicked)
        fixture.completionRepository.setNextError(AppError.Storage(StorageError.WRITE_FAILED))

        fixture.viewModel.onAction(QuestModeAction.AbandonConfirmed)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value as QuestModeUiState.Content
        assertFalse(content.showAbandonConfirmation)
        val activeSessionResult = fixture.completionRepository.getActiveSession()
        assertTrue((activeSessionResult as DataResult.Success).value != null)
    }

    @Test
    fun completeLoadingPreventsDuplicateAction() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(QuestModeAction.CompleteClicked)
        fixture.viewModel.onAction(QuestModeAction.CompleteClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val completionsResult = fixture.completionRepository.getCompletions()
        val completions = (completionsResult as DataResult.Success).value
        assertEquals(1, completions.size)
    }

    @Test
    fun completeSuccessEmitsOneNavigationEvent() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(QuestModeAction.CompleteClicked)
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(awaitItem() is QuestModeEvent.NavigateToCompletion)
        }
    }

    @Test
    fun timedCompletionBeforeDeadlineSucceeds() = runTest {
        // Short timer, deliberately driven all the way to Finished before the test ends — see
        // timerUpdatesPreserveOtherContentState's own KDoc for why an undrained countdown job
        // would otherwise hang runTest's implicit end-of-test drain.
        val fixture = fixture(withTimer = 10.seconds)
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.runCurrent()

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(QuestModeAction.CompleteClicked)
            testDispatcher.scheduler.runCurrent()
            assertTrue(awaitItem() is QuestModeEvent.NavigateToCompletion)
        }
        val completions = (fixture.completionRepository.getCompletions() as DataResult.Success).value
        assertEquals(1, completions.size)

        fixture.clock.advanceTo(NOW + 10.seconds)
        testDispatcher.scheduler.advanceTimeBy(10.seconds)
        testDispatcher.scheduler.runCurrent()
    }

    @Test
    fun timedCompletionAfterDeadlineStillSucceeds() = runTest {
        val fixture = fixture(withTimer = 2.seconds)

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
            testDispatcher.scheduler.runCurrent()

            fixture.clock.advanceTo(NOW + 5.seconds)
            testDispatcher.scheduler.advanceTimeBy(3.seconds)
            testDispatcher.scheduler.runCurrent()
            assertEquals(QuestModeEvent.TimerFinished, awaitItem())

            fixture.viewModel.onAction(QuestModeAction.CompleteClicked)
            testDispatcher.scheduler.runCurrent()
            assertTrue(awaitItem() is QuestModeEvent.NavigateToCompletion)
        }
        val completions = (fixture.completionRepository.getCompletions() as DataResult.Success).value
        assertEquals(1, completions.size)
    }

    @Test
    fun completeFailureStaysOpen() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.completionRepository.setNextError(AppError.Storage(StorageError.WRITE_FAILED))

        fixture.viewModel.onAction(QuestModeAction.CompleteClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value as QuestModeUiState.Content
        assertFalse(content.isCompleting)
        assertTrue(content.error != null)
    }

    @Test
    fun staleRouteAfterCompletionNavigatesToCompletionWithoutDuplicating() = runTest {
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(
                com.togetherly.domain.completion.validQuestCompletion(
                    id = CompletionId("completion-1"),
                    questId = QuestId("quest-1"),
                ),
            )
        }
        val fixture = Fixture(CompletionId("completion-1"), completionRepository, FakeQuestRepository(), TestAppClock(NOW))

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(QuestModeEvent.NavigateToCompletion(CompletionId("completion-1")), awaitItem())
        }
        val completions = (completionRepository.getCompletions() as DataResult.Success).value
        assertEquals(1, completions.size)
    }

    @Test
    fun completeFailureEmitsNoNavigationEvent() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.completionRepository.setNextError(AppError.Storage(StorageError.WRITE_FAILED))

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(QuestModeAction.CompleteClicked)
            testDispatcher.scheduler.advanceUntilIdle()
            expectNoEvents()
        }
    }

    /**
     * Simulates process death: a *second, independently constructed* [Fixture] — a fresh
     * [QuestModeViewModel] instance with a fresh [DefaultQuestCountdownEngine], as a real process
     * restart would produce — reads the *same* persisted [completionRepository]/[questRepository]
     * state a real database would have retained, with [clock] now reporting a later wall-clock time
     * than when the (destroyed) first instance started the timer. Recovery must come from
     * `startedAt + timer.duration` alone, never from any in-memory countdown state.
     */
    @Test
    fun processDeathRecreatesViewModelAndRecoversRunningTimerFromPersistedStart() = runTest {
        val questId = QuestId("quest-1")
        val completionId = CompletionId("completion-1")
        val quest = validFamilyQuest(id = questId, timer = QuestTimer(duration = 10.minutes, keepScreenOn = false))
        val session = validActiveQuestSession(completionId = completionId, questId = questId, startedAt = NOW)
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(session) }
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }

        val recreatedClock = TestAppClock(NOW + 4.minutes)
        val recreated = Fixture(completionId, completionRepository, questRepository, recreatedClock)

        recreated.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.runCurrent()

        val content = recreated.viewModel.uiState.value as QuestModeUiState.Content
        val timer = content.quest.timer as com.togetherly.feature.questmode.model.QuestTimerUi.Running
        assertTrue(timer.progress in 0.39f..0.41f)

        recreatedClock.advanceTo(NOW + 10.minutes)
        testDispatcher.scheduler.advanceTimeBy(6.minutes)
        testDispatcher.scheduler.runCurrent()
    }

    @Test
    fun processDeathAfterDeadlineElapsedShowsFinishedOnRecovery() = runTest {
        val questId = QuestId("quest-1")
        val completionId = CompletionId("completion-1")
        val quest = validFamilyQuest(id = questId, timer = QuestTimer(duration = 2.minutes, keepScreenOn = false))
        val session = validActiveQuestSession(completionId = completionId, questId = questId, startedAt = NOW)
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(session) }
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }

        // Deadline elapsed entirely while the process was dead — no live coroutine ever ran.
        val recreatedClock = TestAppClock(NOW + 10.minutes)
        val recreated = Fixture(completionId, completionRepository, questRepository, recreatedClock)

        recreated.viewModel.events.test {
            recreated.viewModel.onAction(QuestModeAction.ScreenStarted)
            testDispatcher.scheduler.runCurrent()
            assertEquals(QuestModeEvent.TimerFinished, awaitItem())
        }
        val content = recreated.viewModel.uiState.value as QuestModeUiState.Content
        assertEquals(com.togetherly.feature.questmode.model.QuestTimerUi.Finished, content.quest.timer)
    }

    @Test
    fun processDeathThenCompleteDoesNotDuplicateCompletion() = runTest {
        val questId = QuestId("quest-1")
        val completionId = CompletionId("completion-1")
        val quest = validFamilyQuest(id = questId)
        val session = validActiveQuestSession(completionId = completionId, questId = questId, startedAt = NOW)
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(session) }
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }

        val recreated = Fixture(completionId, completionRepository, questRepository, TestAppClock(NOW + 1.minutes))
        recreated.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        recreated.viewModel.onAction(QuestModeAction.CompleteClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val completions = (completionRepository.getCompletions() as DataResult.Success).value
        assertEquals(1, completions.size)
        assertEquals(null, (completionRepository.getActiveSession() as DataResult.Success).value)
    }

    @Test
    fun rawErrorsAreNeverExposedDirectly() = runTest {
        val completionRepository = FakeCompletionRepository()
        completionRepository.setNextError(AppError.Storage(StorageError.READ_FAILED))
        val fixture = Fixture(CompletionId("completion-1"), completionRepository, FakeQuestRepository(), TestAppClock(NOW))

        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val error = fixture.viewModel.uiState.value as QuestModeUiState.Error
        assertEquals(AppError.Storage(StorageError.READ_FAILED).toUiText(), error.message)
    }

    // -- Analytics --------------------------------------------------------------------------

    @Test
    fun screenStartedCapturesTheQuestModeScreenExactlyOnce() = runTest {
        val fixture = fixture()

        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(AnalyticsScreen.QUEST_MODE), fixture.analytics.screensViewed)
    }

    @Test
    fun completingWithoutATimerCapturesUsedTimerFalse() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(QuestModeAction.CompleteClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = fixture.analytics.capturedEvents.single() as QuestCompleted
        assertEquals(QuestId("quest-1"), event.questId)
        assertFalse(event.usedTimer)
        assertFalse(event.usedPhoneDown)
    }

    @Test
    fun completingAfterTheTimerFinishesCapturesUsedTimerTrue() = runTest {
        val fixture = fixture(withTimer = 2.seconds)
        fixture.viewModel.events.test {
            fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
            testDispatcher.scheduler.runCurrent()
            fixture.clock.advanceTo(NOW + 2.seconds)
            testDispatcher.scheduler.advanceTimeBy(1.seconds)
            testDispatcher.scheduler.runCurrent()
            assertEquals(QuestModeEvent.TimerFinished, awaitItem())

            fixture.viewModel.onAction(QuestModeAction.CompleteClicked)
            testDispatcher.scheduler.runCurrent()
            awaitItem()
        }

        val event = fixture.analytics.capturedEvents.single() as QuestCompleted
        assertTrue(event.usedTimer)
    }

    @Test
    fun completingWhilePhoneDownCapturesUsedPhoneDownTrue() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(QuestModeAction.PhoneDownClicked)

        fixture.viewModel.onAction(QuestModeAction.CompleteClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = fixture.analytics.capturedEvents.single() as QuestCompleted
        assertTrue(event.usedPhoneDown)
    }

    @Test
    fun failedCompletionCapturesNoCompletedEvent() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.completionRepository.setNextError(AppError.Storage(StorageError.WRITE_FAILED))

        fixture.viewModel.onAction(QuestModeAction.CompleteClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fixture.analytics.capturedEvents.none { it is QuestCompleted })
    }

    @Test
    fun duplicateCompleteClicksNeverCaptureTwice() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(QuestModeAction.CompleteClicked)
        fixture.viewModel.onAction(QuestModeAction.CompleteClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fixture.analytics.capturedEvents.count { it is QuestCompleted })
    }

    @Test
    fun successfulAbandonCapturesQuestAbandonedWithThePhoneDownState() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(QuestModeAction.PhoneDownClicked)
        fixture.viewModel.onAction(QuestModeAction.BackClicked)
        fixture.viewModel.onAction(QuestModeAction.AbandonClicked)

        fixture.viewModel.onAction(QuestModeAction.AbandonConfirmed)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = fixture.analytics.capturedEvents.single() as QuestAbandoned
        assertEquals(QuestId("quest-1"), event.questId)
        assertTrue(event.usedPhoneDown)
    }

    @Test
    fun failedAbandonCapturesNoAbandonedEvent() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(QuestModeAction.BackClicked)
        fixture.viewModel.onAction(QuestModeAction.AbandonClicked)
        fixture.completionRepository.setNextError(AppError.Storage(StorageError.WRITE_FAILED))

        fixture.viewModel.onAction(QuestModeAction.AbandonConfirmed)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fixture.analytics.capturedEvents.none { it is QuestAbandoned })
    }

    @Test
    fun keepingInProgressNeverCapturesAbandoned() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(QuestModeAction.BackClicked)

        fixture.viewModel.onAction(QuestModeAction.KeepInProgressClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fixture.analytics.capturedEvents.none { it is QuestAbandoned })
    }

    @Test
    fun noEventsAreCapturedWithoutConsent() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val session = validActiveQuestSession(completionId = CompletionId("completion-1"), questId = QuestId("quest-1"), startedAt = NOW)
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(session) }
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }
        val analytics = FakeProductAnalytics()
        val fixture = Fixture(CompletionId("completion-1"), completionRepository, questRepository, TestAppClock(NOW), analytics)

        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(QuestModeAction.CompleteClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(analytics.screensViewed.isEmpty())
        assertTrue(analytics.capturedEvents.isEmpty())
    }

    // -- RevenueCat customer attributes ------------------------------------------------------

    @Test
    fun successfulCompletionMarksFirstQuestCompleted() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(QuestModeAction.CompleteClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fixture.customerAttributesRepository.markFirstQuestCompletedCallCount)
    }

    @Test
    fun failedCompletionNeverMarksFirstQuestCompleted() = runTest {
        val fixture = fixture()
        fixture.viewModel.onAction(QuestModeAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.completionRepository.setNextError(AppError.Storage(StorageError.WRITE_FAILED))

        fixture.viewModel.onAction(QuestModeAction.CompleteClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, fixture.customerAttributesRepository.markFirstQuestCompletedCallCount)
    }
}

package com.togetherly.domain.questmode

import app.cash.turbine.test
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.domain.completion.validActiveQuestSession
import com.togetherly.domain.quest.QuestTimer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val STARTED_AT = Instant.parse("2026-06-15T08:00:00Z")

private fun session() = validActiveQuestSession(startedAt = STARTED_AT)
private fun timer(duration: kotlin.time.Duration) = QuestTimer(duration = duration, keepScreenOn = false)

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultQuestCountdownEngineTest {

    private fun TestAppClock.toDispatchers(scheduler: kotlinx.coroutines.test.TestCoroutineScheduler) =
        TestAppDispatchers(StandardTestDispatcher(scheduler))

    @Test
    fun immediateFirstEmission() = runTest {
        val clock = TestAppClock(STARTED_AT)
        val engine = DefaultQuestCountdownEngine(clock, QuestTimerPolicy(), clock.toDispatchers(testScheduler))

        engine.observe(session(), timer(10.minutes)).test {
            val first = awaitItem()
            assertTrue(first is QuestTimerState.Running)
            assertEquals(10.minutes, (first as QuestTimerState.Running).remaining)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun runningStateUpdatesAsClockAdvances() = runTest {
        val clock = TestAppClock(STARTED_AT)
        val engine = DefaultQuestCountdownEngine(clock, QuestTimerPolicy(), clock.toDispatchers(testScheduler))

        engine.observe(session(), timer(10.minutes)).test {
            awaitItem()

            clock.advanceTo(STARTED_AT + 1.seconds)
            testScheduler.advanceTimeBy(1.seconds)
            testScheduler.runCurrent()

            val second = awaitItem() as QuestTimerState.Running
            assertEquals(10.minutes - 1.seconds, second.remaining)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun exactDeadlineEmitsFinishedAndCompletes() = runTest {
        val clock = TestAppClock(STARTED_AT)
        val engine = DefaultQuestCountdownEngine(clock, QuestTimerPolicy(), clock.toDispatchers(testScheduler))

        engine.observe(session(), timer(2.seconds)).test {
            awaitItem()

            clock.advanceTo(STARTED_AT + 2.seconds)
            testScheduler.advanceTimeBy(1.seconds)
            testScheduler.runCurrent()

            val finished = awaitItem()
            assertTrue(finished is QuestTimerState.Finished)
            awaitComplete()
        }
    }

    @Test
    fun delayedTickRecalculatesFromTheRealDeadlineNotByDecrementing() = runTest {
        val clock = TestAppClock(STARTED_AT)
        val engine = DefaultQuestCountdownEngine(clock, QuestTimerPolicy(), clock.toDispatchers(testScheduler))

        engine.observe(session(), timer(10.minutes)).test {
            awaitItem()

            // Simulates the app being backgrounded for far longer than one tick interval.
            clock.advanceTo(STARTED_AT + 5.minutes)
            testScheduler.advanceTimeBy(1.seconds)
            testScheduler.runCurrent()

            val afterJump = awaitItem() as QuestTimerState.Running
            assertEquals(5.minutes, afterJump.remaining)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun backwardClockJumpIsClampedToFullDuration() = runTest {
        val clock = TestAppClock(STARTED_AT + 1.minutes)
        val engine = DefaultQuestCountdownEngine(clock, QuestTimerPolicy(), clock.toDispatchers(testScheduler))

        engine.observe(session(), timer(10.minutes)).test {
            awaitItem()

            clock.advanceTo(STARTED_AT - 1.minutes)
            testScheduler.advanceTimeBy(1.seconds)
            testScheduler.runCurrent()

            val afterBackwardJump = awaitItem() as QuestTimerState.Running
            assertEquals(10.minutes, afterBackwardJump.remaining)
            assertEquals(0f, afterBackwardJump.progress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cancellationStopsTheTickLoop() = runTest {
        val clock = TestAppClock(STARTED_AT)
        val engine = DefaultQuestCountdownEngine(clock, QuestTimerPolicy(), clock.toDispatchers(testScheduler))
        var emissions = 0

        val job = launch {
            engine.observe(session(), timer(10.minutes)).collect { emissions++ }
        }
        testScheduler.advanceTimeBy(1.seconds)
        testScheduler.runCurrent()
        val countBeforeCancel = emissions
        job.cancel()

        testScheduler.advanceTimeBy(5.seconds)
        testScheduler.runCurrent()

        assertEquals(countBeforeCancel, emissions)
    }

    @Test
    fun multipleCollectorsAreIndependent() = runTest {
        val clock = TestAppClock(STARTED_AT)
        val engine = DefaultQuestCountdownEngine(clock, QuestTimerPolicy(), clock.toDispatchers(testScheduler))
        val flow = engine.observe(session(), timer(10.minutes))

        flow.test {
            assertTrue(awaitItem() is QuestTimerState.Running)
            cancelAndIgnoreRemainingEvents()
        }
        flow.test {
            assertTrue(awaitItem() is QuestTimerState.Running)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

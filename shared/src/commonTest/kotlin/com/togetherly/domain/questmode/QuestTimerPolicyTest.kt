package com.togetherly.domain.questmode

import com.togetherly.domain.completion.validActiveQuestSession
import com.togetherly.domain.quest.QuestTimer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val STARTED_AT = Instant.parse("2026-06-15T08:00:00Z")

private fun timer(duration: kotlin.time.Duration = 10.minutes) = QuestTimer(duration = duration, keepScreenOn = false)
private fun session() = validActiveQuestSession(startedAt = STARTED_AT)

class QuestTimerPolicyTest {

    private val policy = QuestTimerPolicy()

    @Test
    fun noTimerProducesNotRequired() {
        val result = policy.resolve(session(), null, now = STARTED_AT)

        assertEquals(QuestTimerState.NotRequired, result)
    }

    @Test
    fun runningTimerAtStart() {
        val result = policy.resolve(session(), timer(10.minutes), now = STARTED_AT)

        val running = result as QuestTimerState.Running
        assertEquals(10.minutes, running.remaining)
        assertEquals(0f, running.progress)
    }

    @Test
    fun runningTimerHalfway() {
        val result = policy.resolve(session(), timer(10.minutes), now = STARTED_AT + 5.minutes)

        val running = result as QuestTimerState.Running
        assertEquals(5.minutes, running.remaining)
        assertEquals(0.5f, running.progress)
    }

    @Test
    fun finishedTimerAtExactDeadline() {
        val result = policy.resolve(session(), timer(10.minutes), now = STARTED_AT + 10.minutes)

        val finished = result as QuestTimerState.Finished
        assertEquals(STARTED_AT, finished.startedAt)
        assertEquals(STARTED_AT + 10.minutes, finished.finishedAt)
    }

    @Test
    fun finishedTimerAfterDeadline() {
        val result = policy.resolve(session(), timer(10.minutes), now = STARTED_AT + 20.minutes)

        assertTrue(result is QuestTimerState.Finished)
    }

    @Test
    fun clockEarlierThanStartReportsFullDurationRemaining() {
        val result = policy.resolve(session(), timer(10.minutes), now = STARTED_AT - 5.minutes)

        val running = result as QuestTimerState.Running
        assertEquals(10.minutes, running.remaining)
        assertEquals(0f, running.progress)
    }

    @Test
    fun progressIsClampedAndNeverExceedsValidRange() {
        val justBeforeDeadline = policy.resolve(session(), timer(10.minutes), now = STARTED_AT + 599.seconds)

        val running = justBeforeDeadline as QuestTimerState.Running
        assertTrue(running.progress in 0f..1f)
        assertTrue(running.progress > 0.9f)
    }
}

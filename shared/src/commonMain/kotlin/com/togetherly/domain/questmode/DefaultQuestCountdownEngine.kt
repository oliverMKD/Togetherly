package com.togetherly.domain.questmode

import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.datetime.AppClock
import com.togetherly.domain.completion.ActiveQuestSession
import com.togetherly.domain.quest.QuestTimer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.time.Duration.Companion.seconds

/**
 * Every emission is a full, independent recalculation from [clock].[AppClock.now] through
 * [timerPolicy] — never `remaining -= 1.seconds` — so a delayed tick (coroutine scheduling,
 * device load, the app having been backgrounded and resumed, a process recreated mid-countdown)
 * never accumulates drift: whatever the real elapsed wall-clock time turns out to be, the next
 * emission reflects it exactly, the same way [com.togetherly.domain.questmode.usecase.LoadQuestMode]
 * resolves state fresh on a cold start. [TICK_INTERVAL] governs the *emission* cadence, not the
 * math — it exists only to avoid emitting on every recomposition-driving frame.
 *
 * Runs on [dispatchers]' `default` dispatcher via [flowOn] specifically so tests can substitute a
 * [kotlinx.coroutines.test.TestDispatcher] there and drive the whole tick loop through virtual
 * time (`advanceTimeBy`/`advanceUntilIdle`) instead of waiting on real one-second [delay] calls.
 * The [Flow] itself is cold and does no scope management of its own — cancelling the collector
 * (see [QuestCountdownEngine]'s own KDoc) is what stops the loop; nothing here ever reaches for
 * `GlobalScope` or a platform timer (`Handler`, `NSTimer`).
 */
internal class DefaultQuestCountdownEngine(
    private val clock: AppClock,
    private val timerPolicy: QuestTimerPolicy,
    private val dispatchers: AppDispatchers,
) : QuestCountdownEngine {

    override fun observe(
        session: ActiveQuestSession,
        timer: QuestTimer,
    ): Flow<QuestTimerState> = flow {
        while (true) {
            val state = timerPolicy.resolve(session, timer, clock.now())
            emit(state)
            if (state is QuestTimerState.Finished) break
            delay(TICK_INTERVAL)
        }
    }.flowOn(dispatchers.default)

    private companion object {
        val TICK_INTERVAL = 1.seconds
    }
}

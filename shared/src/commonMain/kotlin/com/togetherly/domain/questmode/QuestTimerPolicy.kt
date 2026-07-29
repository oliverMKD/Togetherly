package com.togetherly.domain.questmode

import com.togetherly.domain.completion.ActiveQuestSession
import com.togetherly.domain.quest.QuestTimer
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Pure and synchronous — never reads the system clock itself; [now] is always supplied by the
 * caller (see [com.togetherly.domain.questmode.usecase.LoadQuestMode] and Step 9.2's countdown
 * engine, the only two callers). The timer's deadline is derived wall-clock data —
 * `activeSession.startedAt + questTimer.duration` — never a value stored or ticked down anywhere;
 * see `docs/quest-mode.md`'s own "timer deadline decision" section for why that's what lets the
 * timer survive backgrounding and process death with no background service and no periodic writes.
 */
class QuestTimerPolicy {

    fun resolve(
        session: ActiveQuestSession,
        questTimer: QuestTimer?,
        now: Instant,
    ): QuestTimerState {
        if (questTimer == null) return QuestTimerState.NotRequired

        val startedAt = session.startedAt
        val endsAt = startedAt + questTimer.duration

        if (now >= endsAt) {
            return QuestTimerState.Finished(startedAt = startedAt, finishedAt = endsAt)
        }

        // A clock earlier than startedAt (clock skew, a resumed process reading a slightly stale
        // clock) is treated as "no time has elapsed yet" — full duration remaining, 0f progress —
        // rather than a negative elapsed/over-100%-remaining value.
        val elapsed = (now - startedAt).coerceAtLeast(Duration.ZERO)
        val remaining = (endsAt - now).coerceAtMost(questTimer.duration)
        val progress = (elapsed / questTimer.duration).toFloat().coerceIn(0f, 1f)

        return QuestTimerState.Running(
            startedAt = startedAt,
            endsAt = endsAt,
            remaining = remaining,
            progress = progress,
        )
    }
}

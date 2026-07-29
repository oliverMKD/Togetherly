package com.togetherly.domain.questmode

import com.togetherly.domain.validation.requireConsistentState
import com.togetherly.domain.validation.requirePositive
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * A pure snapshot of where a quest's optional countdown stands at one instant — never a ticking
 * object itself (see [QuestTimerPolicy], the lifecycle-safe countdown engine of Step 9.2, for how
 * repeated snapshots are produced over time).
 *
 * [Finished] does not imply the quest is completed, and reaching it never creates a
 * [com.togetherly.domain.completion.QuestCompletion] on its own — the family still explicitly
 * chooses "We did it" (Step 9.3's `CompleteClicked`). A timer and a completion are two separate
 * concerns that only happen to interact through the UI.
 */
sealed interface QuestTimerState {

    /** The quest has no [com.togetherly.domain.quest.QuestTimer] configured — nothing to show. */
    data object NotRequired : QuestTimerState

    /**
     * [remaining] must be strictly positive — the instant it would reach zero, [resolve][QuestTimerPolicy.resolve]
     * returns [Finished] instead, so a [Running] with non-positive remaining time is a contradiction
     * this type refuses to represent. [progress] is always in `0f..1f`, derived as `elapsed /
     * totalDuration` — never a raw ratio that could exceed that range or become `NaN`.
     */
    data class Running(
        val startedAt: Instant,
        val endsAt: Instant,
        val remaining: Duration,
        val progress: Float,
    ) : QuestTimerState {
        init {
            requirePositive(remaining)
            requireConsistentState(progress in 0f..1f)
        }
    }

    data class Finished(
        val startedAt: Instant,
        val finishedAt: Instant,
    ) : QuestTimerState
}

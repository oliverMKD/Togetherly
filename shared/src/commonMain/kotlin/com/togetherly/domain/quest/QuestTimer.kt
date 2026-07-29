package com.togetherly.domain.quest

import com.togetherly.domain.validation.requireAtMost
import com.togetherly.domain.validation.requirePositive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * keepScreenOn is a product instruction for the timed activity, not an Android window flag —
 * platform code decides how to honor it.
 */
data class QuestTimer(
    val duration: Duration,
    val keepScreenOn: Boolean,
) {
    init {
        requirePositive(duration)
        requireAtMost(duration, MAX_DURATION)
    }

    companion object {
        val MAX_DURATION: Duration = 2.hours
    }
}

package com.togetherly.designsystem.motion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TogetherlyMotionTest {

    @Test
    fun everyDurationIsNonNegative() {
        val motion = TogetherlyMotion()
        listOf(motion.instant, motion.fast, motion.standard, motion.slow, motion.reveal, motion.celebration)
            .forEach { duration -> assertTrue(duration >= 0, "Duration $duration must be non-negative") }
    }

    @Test
    fun durationsIncreaseMonotonicallyFromInstantToCelebration() {
        val motion = TogetherlyMotion()
        val durations = listOf(motion.instant, motion.fast, motion.standard, motion.slow, motion.reveal, motion.celebration)
        durations.zipWithNext().forEach { (shorter, longer) ->
            assertTrue(shorter < longer, "Expected $shorter < $longer")
        }
    }

    @Test
    fun instantIsZero() {
        assertEquals(0, TogetherlyMotion().instant)
    }

    @Test
    fun reducedMotionCollapsesAnyDurationToInstant() {
        assertEquals(0, resolveMotionDuration(TogetherlyMotion().celebration, reduceMotion = true))
    }

    @Test
    fun motionIsUnaffectedWhenReducedMotionIsOff() {
        val celebration = TogetherlyMotion().celebration
        assertEquals(celebration, resolveMotionDuration(celebration, reduceMotion = false))
    }
}

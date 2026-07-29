package com.togetherly.designsystem.spacing

import kotlin.test.Test
import kotlin.test.assertTrue

class TogetherlySpacingTest {

    @Test
    fun everyStepIsNonNegative() {
        val spacing = TogetherlySpacing()
        listOf(spacing.none, spacing.xxs, spacing.xs, spacing.s, spacing.m, spacing.l, spacing.xl, spacing.xxl)
            .forEach { step -> assertTrue(step.value >= 0f, "Spacing step $step must be non-negative") }
    }

    @Test
    fun everyStepIsAMultipleOfFourDp() {
        val spacing = TogetherlySpacing()
        listOf(spacing.xxs, spacing.xs, spacing.s, spacing.m, spacing.l, spacing.xl, spacing.xxl)
            .forEach { step -> assertTrue(step.value % 4f == 0f, "Spacing step $step must sit on the 4dp rhythm") }
    }

    @Test
    fun stepsIncreaseMonotonically() {
        val spacing = TogetherlySpacing()
        val steps = listOf(spacing.none, spacing.xxs, spacing.xs, spacing.s, spacing.m, spacing.l, spacing.xl, spacing.xxl)
        steps.zipWithNext().forEach { (smaller, larger) ->
            assertTrue(smaller < larger, "Expected $smaller < $larger")
        }
    }
}

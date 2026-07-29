package com.togetherly.domain.daily

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RerollAllowanceTest {

    @Test
    fun unlimitedHasNullRemainingAndAlwaysCanReroll() {
        val allowance = RerollAllowance(used = 100, maximum = null)

        assertEquals(null, allowance.remaining)
        assertTrue(allowance.canReroll)
    }

    @Test
    fun exhaustedFiniteAllowanceCannotReroll() {
        val allowance = RerollAllowance(used = 1, maximum = 1)

        assertEquals(0, allowance.remaining)
        assertFalse(allowance.canReroll)
    }

    @Test
    fun remainingNeverGoesNegativeIfUsedExceedsMaximum() {
        val allowance = RerollAllowance(used = 5, maximum = 1)

        assertEquals(0, allowance.remaining)
        assertFalse(allowance.canReroll)
    }
}

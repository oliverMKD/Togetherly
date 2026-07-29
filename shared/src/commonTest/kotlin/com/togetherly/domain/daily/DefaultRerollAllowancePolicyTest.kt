package com.togetherly.domain.daily

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultRerollAllowancePolicyTest {

    private val policy = DefaultRerollAllowancePolicy()

    @Test
    fun freshSelectionAllowsOneFreeReroll() {
        val allowance = policy.allowance(selectionIndex = 0, hasFamilyPlus = false)

        assertEquals(RerollAllowance(used = 0, maximum = 1), allowance)
        assertTrue(allowance.canReroll)
        assertEquals(1, allowance.remaining)
    }

    @Test
    fun oneRerollAlreadyUsedRejectsAnother() {
        val allowance = policy.allowance(selectionIndex = 1, hasFamilyPlus = false)

        assertFalse(allowance.canReroll)
        assertEquals(0, allowance.remaining)
    }

    @Test
    fun familyPlusIsUnlimitedRegardlessOfSelectionIndex() {
        val allowance = policy.allowance(selectionIndex = 5, hasFamilyPlus = true)

        assertEquals(RerollAllowance(used = 5, maximum = null), allowance)
        assertTrue(allowance.canReroll)
        assertEquals(null, allowance.remaining)
    }

    @Test
    fun newLocalDayResetsAllowanceBecauseItsFreshSelectionIndexIsZero() {
        val previousDayAllowance = policy.allowance(selectionIndex = 1, hasFamilyPlus = false)
        val newDayAllowance = policy.allowance(selectionIndex = 0, hasFamilyPlus = false)

        assertFalse(previousDayAllowance.canReroll)
        assertTrue(newDayAllowance.canReroll)
    }
}

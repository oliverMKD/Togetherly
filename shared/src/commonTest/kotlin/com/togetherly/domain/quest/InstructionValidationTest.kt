package com.togetherly.domain.quest

import com.togetherly.domain.validation.DomainValidationException
import com.togetherly.domain.validation.DomainValidationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InstructionValidationTest {

    @Test
    fun positiveOrderIsAccepted() {
        val step = InstructionStep(order = 1, text = InstructionText("Do the thing"))

        assertEquals(1, step.order)
    }

    @Test
    fun zeroOrderIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            InstructionStep(order = 0, text = InstructionText("Do the thing"))
        }
        assertEquals(DomainValidationReason.NON_POSITIVE_VALUE, exception.reason)
    }

    @Test
    fun duplicateOrderIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            validFamilyQuest(
                instructions = listOf(
                    InstructionStep(1, InstructionText("Step one")),
                    InstructionStep(1, InstructionText("Step two")),
                ),
            )
        }
        assertEquals(DomainValidationReason.DUPLICATE_VALUE, exception.reason)
    }

    @Test
    fun missingOrderGapIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            validFamilyQuest(
                instructions = listOf(
                    InstructionStep(1, InstructionText("Step one")),
                    InstructionStep(3, InstructionText("Step three")),
                ),
            )
        }
        assertEquals(DomainValidationReason.INVALID_ORDER, exception.reason)
    }

    @Test
    fun orderingNotStartingAtOneIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            validFamilyQuest(
                instructions = listOf(
                    InstructionStep(2, InstructionText("Step two")),
                    InstructionStep(3, InstructionText("Step three")),
                ),
            )
        }
        assertEquals(DomainValidationReason.INVALID_ORDER, exception.reason)
    }
}

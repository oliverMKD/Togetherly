package com.togetherly.domain.validation

import com.togetherly.domain.common.ReminderId
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.purchase.ProductId
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPackId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DomainIdentifierValidationTest {

    private val identifierConstructors: List<(String) -> Any> = listOf(
        { value -> FamilyId(value) },
        { value -> QuestId(value) },
        { value -> QuestPackId(value) },
        { value -> CompletionId(value) },
        { value -> MemoryMediaId(value) },
        { value -> ReminderId(value) },
        { value -> EntitlementId(value) },
        { value -> ProductId(value) },
    )

    @Test
    fun validIdentifierIsAccepted() {
        for (construct in identifierConstructors) {
            construct("a-valid-id-123")
        }
    }

    @Test
    fun blankIdentifierIsRejected() {
        for (construct in identifierConstructors) {
            val exception = assertFailsWith<DomainValidationException> { construct("") }
            assertEquals(DomainValidationReason.BLANK_VALUE, exception.reason)
        }
    }

    @Test
    fun whitespaceOnlyIdentifierIsRejected() {
        for (construct in identifierConstructors) {
            val exception = assertFailsWith<DomainValidationException> { construct("   ") }
            assertEquals(DomainValidationReason.BLANK_VALUE, exception.reason)
        }
    }

    @Test
    fun leadingWhitespaceIsRejected() {
        for (construct in identifierConstructors) {
            val exception = assertFailsWith<DomainValidationException> { construct(" leading") }
            assertEquals(DomainValidationReason.SURROUNDING_WHITESPACE, exception.reason)
        }
    }

    @Test
    fun trailingWhitespaceIsRejected() {
        for (construct in identifierConstructors) {
            val exception = assertFailsWith<DomainValidationException> { construct("trailing ") }
            assertEquals(DomainValidationReason.SURROUNDING_WHITESPACE, exception.reason)
        }
    }

    @Test
    fun excessivelyLongIdentifierIsRejected() {
        val tooLong = "a".repeat(MAX_DOMAIN_ID_LENGTH + 1)
        for (construct in identifierConstructors) {
            val exception = assertFailsWith<DomainValidationException> { construct(tooLong) }
            assertEquals(DomainValidationReason.VALUE_TOO_LONG, exception.reason)
        }
    }
}

package com.togetherly.domain.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DomainCollectionValidationTest {

    @Test
    fun requireNotEmptyRejectsEmptyCollection() {
        val exception = assertFailsWith<DomainValidationException> { requireNotEmpty(emptyList<Int>()) }
        assertEquals(DomainValidationReason.EMPTY_COLLECTION, exception.reason)
    }

    @Test
    fun requireNotEmptyAcceptsNonEmptyCollection() {
        requireNotEmpty(listOf(1))
    }

    @Test
    fun requireUniqueValuesRejectsDuplicates() {
        val exception = assertFailsWith<DomainValidationException> { requireUniqueValues(listOf(1, 2, 1)) }
        assertEquals(DomainValidationReason.DUPLICATE_VALUE, exception.reason)
    }

    @Test
    fun requireUniqueValuesAcceptsDistinctValues() {
        requireUniqueValues(listOf(1, 2, 3))
    }

    @Test
    fun requirePositiveRejectsZeroAndNegative() {
        for (value in listOf(0, -1)) {
            val exception = assertFailsWith<DomainValidationException> { requirePositive(value) }
            assertEquals(DomainValidationReason.NON_POSITIVE_VALUE, exception.reason)
        }
    }

    @Test
    fun requirePositiveAcceptsPositiveValue() {
        requirePositive(1)
    }

    @Test
    fun requireNonNegativeRejectsNegative() {
        val exception = assertFailsWith<DomainValidationException> { requireNonNegative(-1) }
        assertEquals(DomainValidationReason.NON_POSITIVE_VALUE, exception.reason)
    }

    @Test
    fun requireNonNegativeAcceptsZeroAndPositive() {
        requireNonNegative(0)
        requireNonNegative(1)
    }
}

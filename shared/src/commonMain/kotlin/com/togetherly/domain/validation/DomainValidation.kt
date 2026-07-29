package com.togetherly.domain.validation

import kotlin.time.Duration

/**
 * Comfortably fits generated UUIDs (36 chars) and third-party store/product identifiers
 * (Google Play and App Store IDs stay well under 100 chars) while still rejecting garbage input.
 */
internal const val MAX_DOMAIN_ID_LENGTH = 128

internal fun requireNotBlank(value: String) {
    if (value.isBlank()) throw DomainValidationException(DomainValidationReason.BLANK_VALUE)
}

internal fun requireNoSurroundingWhitespace(value: String) {
    if (value != value.trim()) throw DomainValidationException(DomainValidationReason.SURROUNDING_WHITESPACE)
}

internal fun requireMaxLength(value: String, maxLength: Int) {
    if (value.length > maxLength) throw DomainValidationException(DomainValidationReason.VALUE_TOO_LONG)
}

internal fun requireValidDomainId(value: String) {
    requireNotBlank(value)
    requireNoSurroundingWhitespace(value)
    requireMaxLength(value, MAX_DOMAIN_ID_LENGTH)
}

internal fun requireValidDomainText(value: String, maxLength: Int) {
    requireNotBlank(value)
    requireNoSurroundingWhitespace(value)
    requireMaxLength(value, maxLength)
}

internal fun requireNotEmpty(collection: Collection<*>) {
    if (collection.isEmpty()) throw DomainValidationException(DomainValidationReason.EMPTY_COLLECTION)
}

internal fun requireUniqueValues(values: List<*>) {
    if (values.toSet().size != values.size) throw DomainValidationException(DomainValidationReason.DUPLICATE_VALUE)
}

internal fun requirePositive(value: Int) {
    if (value <= 0) throw DomainValidationException(DomainValidationReason.NON_POSITIVE_VALUE)
}

internal fun requireNonNegative(value: Int) {
    if (value < 0) throw DomainValidationException(DomainValidationReason.NON_POSITIVE_VALUE)
}

internal fun requirePositive(value: Duration) {
    if (value <= Duration.ZERO) throw DomainValidationException(DomainValidationReason.NON_POSITIVE_VALUE)
}

internal fun requireAtMost(value: Int, maximum: Int) {
    if (value > maximum) throw DomainValidationException(DomainValidationReason.VALUE_TOO_LONG)
}

internal fun requireAtMost(value: Duration, maximum: Duration) {
    if (value > maximum) throw DomainValidationException(DomainValidationReason.VALUE_TOO_LONG)
}

internal fun requireConsistentState(condition: Boolean) {
    if (!condition) throw DomainValidationException(DomainValidationReason.CONTRADICTORY_STATE)
}

internal fun requireInRange(value: Float, range: ClosedFloatingPointRange<Float>) {
    if (value !in range) throw DomainValidationException(DomainValidationReason.OUT_OF_RANGE)
}

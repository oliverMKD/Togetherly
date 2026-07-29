package com.togetherly.domain.validation

enum class DomainValidationReason {
    BLANK_VALUE,
    SURROUNDING_WHITESPACE,
    VALUE_TOO_LONG,
    NON_POSITIVE_VALUE,
    EMPTY_COLLECTION,
    DUPLICATE_VALUE,
    INVALID_ORDER,
    CONTRADICTORY_STATE,
    OUT_OF_RANGE,
}

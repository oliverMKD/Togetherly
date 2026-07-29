package com.togetherly.domain.validation

class DomainValidationException(
    val reason: DomainValidationReason,
) : IllegalArgumentException(reason.name)

package com.togetherly.content.mapper

import com.togetherly.domain.validation.DomainValidationException

internal inline fun <T> ContentMappingResult<T>.getOrElse(
    onFailure: (ContentMappingIssue) -> Nothing,
): T = when (this) {
    is ContentMappingResult.Success -> value
    is ContentMappingResult.Failure -> onFailure(issue)
}

/**
 * Constructs a domain value, converting a [DomainValidationException] into a typed mapping
 * issue at [path] instead of letting it escape uncontrolled.
 */
internal inline fun <T> mapCatching(
    path: String,
    rawValue: String?,
    block: () -> T,
): ContentMappingResult<T> = try {
    ContentMappingResult.Success(block())
} catch (e: DomainValidationException) {
    ContentMappingResult.Failure(ContentMappingIssue(path, ContentMappingIssueCode.DOMAIN_VALIDATION_FAILED, rawValue))
}

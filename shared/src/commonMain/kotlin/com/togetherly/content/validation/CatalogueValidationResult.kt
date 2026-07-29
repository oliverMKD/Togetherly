package com.togetherly.content.validation

internal sealed interface CatalogueValidationResult {

    /**
     * No blocking [CatalogueIssueSeverity.ERROR] issues were found. [warnings] may still be
     * non-empty — content-quality warnings never invalidate an otherwise correct catalogue.
     */
    data class Valid(
        val warnings: List<CatalogueValidationIssue> = emptyList(),
    ) : CatalogueValidationResult

    /** [issues] contains every issue found in the pass, both errors and warnings mixed together. */
    data class Invalid(
        val issues: List<CatalogueValidationIssue>,
    ) : CatalogueValidationResult
}

internal enum class CatalogueIssueSeverity {
    ERROR,
    WARNING,
}

/**
 * A small, reused set of codes — mirroring [com.togetherly.domain.validation.DomainValidationReason]
 * and [com.togetherly.content.mapper.ContentMappingIssueCode] — rather than one code per exact
 * rule. [CatalogueValidationIssue.path] already identifies which specific field or rule fired.
 */
internal enum class CatalogueIssueCode {
    UNSUPPORTED_SCHEMA_VERSION,
    NON_POSITIVE_VALUE,
    UNSUPPORTED_LOCALE,
    EMPTY_COLLECTION,
    DUPLICATE_VALUE,
    BLANK_VALUE,
    INVALID_ORDER,
    UNSUPPORTED_ENUM_VALUE,
    VALUE_TOO_LONG,
    INVALID_ACCESS_COMBINATION,
    REFERENCE_NOT_FOUND,
    REFERENCE_MISMATCH,
    CONTRADICTORY_STATE,
    NAMING_CONVENTION_VIOLATION,
    SUSPICIOUS_CONTENT,
}

/**
 * [details] carries only the single offending raw field value for diagnostics — never entire
 * quest content, never a stack trace.
 */
internal data class CatalogueValidationIssue(
    val severity: CatalogueIssueSeverity,
    val path: String,
    val code: CatalogueIssueCode,
    val details: String? = null,
)

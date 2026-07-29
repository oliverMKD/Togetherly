package com.togetherly.content.mapper

/**
 * A separate, purpose-built result type rather than reusing [com.togetherly.core.result.DataResult]/
 * [com.togetherly.core.error.AppError]: those model coarse-grained, user/app-facing runtime
 * errors, not the precise JSON-path diagnostics content authors need (e.g. `quests[2].ageBands[1]`).
 */
internal sealed interface ContentMappingResult<out T> {

    data class Success<T>(
        val value: T,
    ) : ContentMappingResult<T>

    data class Failure(
        val issue: ContentMappingIssue,
    ) : ContentMappingResult<Nothing>
}

internal enum class ContentMappingIssueCode {
    UNKNOWN_ENUM_VALUE,
    INVALID_ACCESS_COMBINATION,
    DOMAIN_VALIDATION_FAILED,
}

/**
 * [value] retains only the raw offending field value for diagnostics — never family memory
 * content or a stack trace.
 */
internal data class ContentMappingIssue(
    val path: String,
    val code: ContentMappingIssueCode,
    val value: String? = null,
)

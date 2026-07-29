package com.togetherly.content.validation

/**
 * Carries the full validation report as [issues] so it stays available for tests and internal
 * diagnostics via [com.togetherly.core.error.AppError.Content.cause] — the message itself stays
 * generic since [AppError] causes must never surface as user-facing text.
 */
internal class CatalogueValidationException(
    val issues: List<CatalogueValidationIssue>,
) : Exception("Quest catalogue failed validation with ${issues.size} issue(s)")

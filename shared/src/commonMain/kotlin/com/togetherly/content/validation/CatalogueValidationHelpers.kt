package com.togetherly.content.validation

internal fun errorIssue(path: String, code: CatalogueIssueCode, details: String? = null) =
    CatalogueValidationIssue(CatalogueIssueSeverity.ERROR, path, code, details)

internal fun warningIssue(path: String, code: CatalogueIssueCode, details: String? = null) =
    CatalogueValidationIssue(CatalogueIssueSeverity.WARNING, path, code, details)

internal fun requireNonBlank(value: String, path: String, issues: MutableList<CatalogueValidationIssue>) {
    if (value.isBlank()) issues += errorIssue(path, CatalogueIssueCode.BLANK_VALUE, value)
}

internal fun requireMaxLength(
    value: String,
    maxLength: Int,
    path: String,
    issues: MutableList<CatalogueValidationIssue>,
) {
    if (value.length > maxLength) issues += errorIssue(path, CatalogueIssueCode.VALUE_TOO_LONG, value)
}

/** Preserves first-seen order, matching Kotlin's default [LinkedHashSet]-backed `mutableSetOf`. */
internal fun <T> findDuplicates(values: List<T>): Set<T> {
    val seen = mutableSetOf<T>()
    val duplicates = mutableSetOf<T>()
    for (value in values) {
        if (!seen.add(value)) duplicates += value
    }
    return duplicates
}

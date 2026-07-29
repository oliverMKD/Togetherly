package com.togetherly.content.mapper

/**
 * Carries the offending [issue] so it stays available for tests and internal diagnostics via
 * [com.togetherly.core.error.AppError.Content.cause] — the message itself stays generic since
 * [com.togetherly.core.error.AppError] causes must never surface as user-facing text.
 */
internal class ContentMappingException(
    val issue: ContentMappingIssue,
) : Exception("Quest catalogue mapping failed at ${issue.path}: ${issue.code}")

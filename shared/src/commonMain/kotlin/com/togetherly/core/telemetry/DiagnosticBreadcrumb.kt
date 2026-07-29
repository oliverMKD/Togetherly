package com.togetherly.core.telemetry

/**
 * A single "what happened right before this" trail entry for a future crash/exception report —
 * [message] must be a short, developer-facing technical description, never user-facing copy, a
 * note, a search query, or anything else this app's own [com.togetherly.core.logging.AppLogger]
 * KDoc already forbids logging. Same text-safety validation as [DiagnosticContext].
 */
data class DiagnosticBreadcrumb(
    val message: String,
    val category: String? = null,
)

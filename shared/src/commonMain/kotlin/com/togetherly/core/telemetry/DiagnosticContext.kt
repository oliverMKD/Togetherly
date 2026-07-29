package com.togetherly.core.telemetry

/**
 * Freeform key/value tags attached to a handled-exception report — never family content. Every
 * value is still run through [TelemetryPrivacyValidator]'s general text-safety checks (length,
 * email-like, URL, file-path, newline-heavy) by [DebugOperationalDiagnostics]/any future real
 * provider before being attached, the same defense-in-depth [ProductAnalytics] properties get —
 * there is no per-key allowlist here the way [AnalyticsEvent] has per-event property allowlists,
 * since diagnostic context keys are inherently freeform (a stack trace's originating feature, a
 * screen name, a retry count) rather than a fixed, reviewable vocabulary.
 */
data class DiagnosticContext(
    val tags: Map<String, String> = emptyMap(),
)

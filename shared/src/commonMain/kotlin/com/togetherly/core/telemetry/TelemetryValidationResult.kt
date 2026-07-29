package com.togetherly.core.telemetry

/** [Rejected.reason] is a short, developer-facing technical description only — never the rejected value itself (see [TelemetryPrivacyValidator]'s own KDoc). */
sealed interface TelemetryValidationResult {
    data class Accepted(val sanitizedProperties: Map<String, AnalyticsValue>) : TelemetryValidationResult
    data class Rejected(val reason: String) : TelemetryValidationResult
}

package com.togetherly.core.telemetry

private const val MAX_TEXT_LENGTH = 200

/**
 * Checked against a lowercased property key before the per-event allowlist even runs — defense in
 * depth against a future schema being misconfigured with one of these by mistake. Not exhaustive
 * by design; the allowlist in [TelemetryEventRegistry] is the real defense (see this class's own
 * KDoc: "prefer allowlisting over trying to detect every possible type of personal data").
 */
private val FORBIDDEN_PROPERTY_KEYS = setOf(
    "email", "phone", "name", "family_name", "parent_name", "child_name",
    "birth_date", "birthdate", "age", "note", "notes", "photo", "photos",
    "voice", "audio", "recording", "receipt", "purchase_token", "token",
    "api_key", "apikey", "secret", "location", "address", "path", "file",
    "search_query", "query", "reaction_text",
)

/**
 * The central allowlist gate every [AnalyticsEvent] and every diagnostic free-text value passes
 * through before a provider ever sees it — the last line of defense, not the only one (every event
 * property is already type-restricted to [AnalyticsValue] at compile time, and every event's own
 * shape is fixed by its data class constructor).
 *
 * Allowlisting is the primary strategy: an event must be [TelemetryEventRegistry]-registered by
 * name, and every one of its properties must be individually registered for that specific event —
 * this class does not try to pattern-match every possible way personal data could appear (that's
 * an unwinnable arms race); it trusts the allowlist and only adds a small number of cheap,
 * high-confidence heuristic checks ([isSafeText]) as defense in depth for the one case an allowlist
 * alone can't catch: a *value* that looks unsafe even though its *key* was legitimately registered
 * (e.g. a future bug that accidentally interpolates a family's own text into an otherwise-safe
 * property).
 *
 * Never logs a rejected value anywhere — [TelemetryValidationResult.Rejected.reason] is always a
 * short, fixed, technical description (`"unsafe property value"`, never the value itself). Callers
 * (e.g. [DebugProductAnalytics]) must only ever log that reason string.
 */
class TelemetryPrivacyValidator(
    private val schemas: Map<String, TelemetryEventSchema>,
) {
    fun validateEvent(event: AnalyticsEvent): TelemetryValidationResult {
        val schema = schemas[event.name]
            ?: return TelemetryValidationResult.Rejected("unregistered event name")

        val sanitized = mutableMapOf<String, AnalyticsValue>()
        for ((key, value) in event.properties()) {
            if (key.isBlank()) {
                return TelemetryValidationResult.Rejected("blank property key")
            }
            if (key.lowercase() in FORBIDDEN_PROPERTY_KEYS) {
                return TelemetryValidationResult.Rejected("forbidden property key")
            }
            if (key !in schema.allowedProperties) {
                return TelemetryValidationResult.Rejected("unregistered property for event")
            }
            if (!isSafeValue(value)) {
                return TelemetryValidationResult.Rejected("unsafe property value")
            }
            sanitized[key] = value
        }
        return TelemetryValidationResult.Accepted(sanitized)
    }

    /**
     * Reusable free-text safety check for [DiagnosticBreadcrumb.message]/[DiagnosticContext.tags]
     * values, which have no per-key allowlist the way event properties do (see those types' own
     * KDoc) — length, email-like, URL-like, file-path-like, and newline-heavy are all rejected.
     */
    fun isSafeText(text: String): Boolean {
        if (text.isBlank()) return false
        if (text.length > MAX_TEXT_LENGTH) return false
        if (looksLikeEmail(text)) return false
        if (looksLikeUrl(text)) return false
        if (looksLikeFilePath(text)) return false
        if (text.contains('\n')) return false
        return true
    }

    /** Exhaustive over [AnalyticsValue]'s closed sealed hierarchy — "rejects unknown property types" is enforced by the type system here, not a runtime check: there is no code path that could construct a fifth variant. */
    private fun isSafeValue(value: AnalyticsValue): Boolean = when (value) {
        is AnalyticsValue.Text -> isSafeText(value.value)
        is AnalyticsValue.Number -> true
        is AnalyticsValue.Decimal -> true
        is AnalyticsValue.BooleanValue -> true
    }

    companion object {
        fun default(): TelemetryPrivacyValidator = TelemetryPrivacyValidator(TelemetryEventRegistry.schemas)
    }
}

private fun looksLikeEmail(text: String): Boolean {
    val atIndex = text.indexOf('@')
    if (atIndex <= 0 || atIndex == text.lastIndex) return false
    return text.substring(atIndex + 1).contains('.')
}

private fun looksLikeUrl(text: String): Boolean {
    val lower = text.lowercase()
    return lower.contains("://") || lower.startsWith("www.")
}

private fun looksLikeFilePath(text: String): Boolean =
    text.contains('/') || text.contains('\\')

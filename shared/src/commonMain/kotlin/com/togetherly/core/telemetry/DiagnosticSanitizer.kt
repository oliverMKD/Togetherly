package com.togetherly.core.telemetry

private const val EXCEPTION_TYPE_TAG = "exception_type"
private val FILE_PATH_PATTERN = Regex("""\S*[/\\]\S+""")
private val URL_QUERY_PATTERN = Regex("""\?\S*""")

/**
 * Prepares a caught [Throwable] and its [DiagnosticContext] for a real diagnostics provider
 * (Sentry) before either ever reaches [OperationalDiagnostics.captureHandledException]'s own
 * implementation — the diagnostics-side counterpart to [TelemetryPrivacyValidator].
 *
 * **The exception class is always the primary identity**: [sanitize] always adds an
 * [EXCEPTION_TYPE_TAG] tag from [Throwable]'s own `::class.simpleName`, regardless of whether the
 * exception's own message is safe to show.
 *
 * **[DiagnosticContext.tags] and breadcrumb messages are what this class can actually sanitize** —
 * each is stripped of file-path-like tokens and URL query parameters ([stripUnsafeSubstrings]),
 * then validated with [TelemetryPrivacyValidator.isSafeText]; a tag that's still unsafe afterward
 * is dropped entirely (never redacted in place — a tag's whole value is its content, so a
 * placeholder string carries no useful signal once the real value is gone).
 *
 * **The [Throwable] instance passed to a real provider is never reconstructed or wrapped.**
 * Kotlin's own common `Throwable` has no portable stack-trace API (`stackTrace` is JVM-only) — a
 * wrapper exception constructed inside this sanitizer would only ever have a stack trace pointing
 * at *this sanitizer's own call site*, never the real one, which would make every report useless
 * for debugging ("do not weaken stack traces so much that reports become useless," this class's own
 * governing spec). The real, technically safe stack trace can only survive if the original
 * [Throwable] object reaches the provider unmodified. This means **the exception's own `.message`
 * is not rewritten before reaching a real provider** — every capture-boundary call site
 * (`docs/sentry-setup.md`'s own "Capture boundaries" list) is required to only ever pass a
 * deliberately-authored, already-safe message (e.g. `IllegalStateException("Catalogue JSON parse
 * failed")`), never blindly forward a raw lower-level exception whose own message might embed a
 * file path, URL, or other unsafe text. This is a disclosed, deliberate limitation, not an
 * oversight — see `docs/sentry-setup.md` for the full reasoning.
 */
class DiagnosticSanitizer(
    private val validator: TelemetryPrivacyValidator = TelemetryPrivacyValidator.default(),
) {
    fun sanitizeTags(throwable: Throwable, context: DiagnosticContext): Map<String, String> {
        val exceptionType = throwable::class.simpleName ?: "UnknownThrowable"
        val sanitizedTags = context.tags
            .filterKeys { it.isNotBlank() && it != EXCEPTION_TYPE_TAG }
            .mapNotNull { (key, value) -> sanitizeText(value)?.let { key to it } }
            .toMap()
        return sanitizedTags + (EXCEPTION_TYPE_TAG to exceptionType)
    }

    /** `null` means the breadcrumb must be dropped entirely — never sent with an empty/placeholder message. */
    fun sanitizeBreadcrumbMessage(message: String): String? = sanitizeText(message)

    private fun sanitizeText(text: String): String? {
        val stripped = stripUnsafeSubstrings(text)
        return stripped.takeIf { validator.isSafeText(it) }
    }
}

/** Strips URL query strings first (a query string is itself often the risky part of an otherwise-safe-looking URL), then any remaining file-path-like or URL-like token wholesale. */
private fun stripUnsafeSubstrings(text: String): String =
    text.replace(URL_QUERY_PATTERN, "").replace(FILE_PATH_PATTERN, "<path>")

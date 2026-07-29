package com.togetherly.data.telemetry

/**
 * The one narrow boundary any Sentry SDK type may cross — [SentryOperationalDiagnostics] depends
 * on this interface only, never on `io.sentry.kotlin.multiplatform.Sentry` (a global `object`)
 * directly, so tests can substitute a fake instead of depending on real global SDK state. No
 * Sentry type appears in this interface's own signature either (every parameter is a primitive or
 * a plain [Throwable]), so nothing above [SentryOperationalDiagnostics] ever needs to know Sentry
 * exists — the same narrow-adapter shape [PostHogSdkAdapter] already establishes for PostHog.
 *
 * [tags] passed to [captureException] are applied only to that one event (Sentry's own
 * per-event-scope-callback overload — never a persistent mutation of the global scope), so a tag
 * from one report can never leak onto an unrelated later report.
 */
internal interface SentrySdkAdapter {
    fun setup(dsn: String, release: String, environment: String, debug: Boolean)
    fun captureException(throwable: Throwable, tags: Map<String, String>)
    fun addBreadcrumb(message: String, category: String?)
    fun clearScope()
    fun close()
}

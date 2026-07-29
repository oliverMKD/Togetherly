package com.togetherly.data.telemetry

/**
 * Supplies this platform's Sentry **DSN** — client configuration, not a secret (a DSN only tells
 * the SDK where to send events), but still never a value this interface's own implementation
 * should read from committed Kotlin source; see `docs/sentry-setup.md` for exactly where each
 * platform's real value goes. Never a Sentry **auth token** — that is private CI/server
 * configuration for source-map/dSYM upload, and must never be placed in this app at all (see
 * `docs/sentry-setup.md`'s own CI environment variables section).
 *
 * Same shape as [com.togetherly.data.purchase.RevenueCatApiKeyProvider]/
 * [PostHogApiKeyProvider] — each platform's app-level bootstrap (`TogetherlyApplication` on
 * Android, `initializeTogetherlyIos` on iOS) supplies a real implementation via
 * `additionalModules`, reading from that platform's own local, gitignored configuration.
 * Returning `null`/blank means "not configured yet" — the telemetry DI wiring decides what that
 * means (falls back to [com.togetherly.core.telemetry.NoOpOperationalDiagnostics]), never this
 * interface.
 */
fun interface SentryDsnProvider {
    fun dsn(): String?
}

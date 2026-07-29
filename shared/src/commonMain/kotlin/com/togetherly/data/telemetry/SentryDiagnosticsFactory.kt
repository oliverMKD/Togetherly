package com.togetherly.data.telemetry

import com.togetherly.core.logging.AppLogger
import com.togetherly.core.telemetry.DiagnosticSanitizer
import com.togetherly.core.telemetry.NoOpOperationalDiagnostics
import com.togetherly.core.telemetry.OperationalDiagnostics

private const val TAG = "SentryDiagnosticsFactory"

/**
 * The one place "is Sentry configured?" is decided — pulled out of `app/di/TelemetryModule.kt`'s
 * own Koin lambda so it's directly unit-testable without spinning up a Koin graph, the same
 * [createProductAnalytics] shape. [adapter]/[sanitizer]/[release] are lazy (`() -> `) so none of
 * them — including whatever release/build lookup [release] performs — ever runs when [dsn] is
 * missing: no [SentrySdkAdapter] instance, nothing Sentry-adjacent happens at all in that case,
 * only [NoOpOperationalDiagnostics].
 *
 * A missing/blank [dsn] always falls back to [NoOpOperationalDiagnostics] — "missing configuration
 * falls back to no-op diagnostics," on every build type, so the app remains fully usable. [debug]
 * additionally gets a clear warning log; a release build stays silent, matching
 * [createProductAnalytics]'s own asymmetry for a missing PostHog key.
 */
internal fun createOperationalDiagnostics(
    dsn: String?,
    release: () -> String,
    environment: String,
    debug: Boolean,
    logger: AppLogger,
    adapter: () -> SentrySdkAdapter,
    sanitizer: () -> DiagnosticSanitizer = { DiagnosticSanitizer() },
): OperationalDiagnostics {
    if (dsn.isNullOrBlank()) {
        if (debug) {
            logger.warn(
                TAG,
                "Sentry DSN is missing. See docs/sentry-setup.md for where to place it. Falling back to no-op diagnostics.",
            )
        }
        return NoOpOperationalDiagnostics()
    }

    return SentryOperationalDiagnostics(
        adapter = adapter(),
        sanitizer = sanitizer(),
        dsn = dsn,
        release = release(),
        environment = environment,
        debug = debug,
        logger = logger,
    )
}

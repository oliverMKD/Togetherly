package com.togetherly.data.telemetry

import com.togetherly.core.logging.AppLogger
import com.togetherly.core.telemetry.DiagnosticBreadcrumb
import com.togetherly.core.telemetry.DiagnosticContext
import com.togetherly.core.telemetry.DiagnosticSanitizer
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.core.telemetry.ProviderConfigurationStatus

private const val TAG = "SentryOperationalDiagnostics"

/**
 * The real [OperationalDiagnostics] implementation, behind [SentrySdkAdapter] — no
 * `io.sentry.kotlin.multiplatform` type appears in this class's own public surface (it implements
 * the provider-neutral [OperationalDiagnostics] contract only), and this class is the only place
 * [SentrySdkAdapter] is called from feature-adjacent code. Mirrors [PostHogProductAnalytics]'s own
 * shape exactly:
 *
 * [setupSucceeded] guards every method — a failed [SentrySdkAdapter.setup] leaves this instance
 * permanently, safely inert for the rest of the process rather than retrying or crashing.
 * [collectionEnabled] is this class's own defense-in-depth gate, checked directly by
 * [captureHandledException]/[addBreadcrumb] before ever reaching the adapter —
 * [com.togetherly.core.telemetry.TelemetryCoordinator] is the only caller of
 * [setCollectionEnabled], and no pre-consent call is ever queued for later replay: "when consent
 * becomes granted, enable future reporting — do not manufacture reports for earlier failures."
 *
 * [clearContext] calls [SentrySdkAdapter.clearScope] — the one thing the Sentry KMP SDK's public
 * API actually supports for "stop carrying old context forward." **It does not purge any envelope
 * already queued on-device before revocation** — the KMP SDK exposes no such method (`close()`
 * itself flushes/attempts to *send* pending events, the opposite of discarding them, so it is
 * deliberately never called here). This is a disclosed SDK limitation, not an oversight — see
 * `docs/sentry-setup.md`. [setCollectionEnabled] `false` is still what actually matters most:
 * every future [captureHandledException]/[addBreadcrumb] call is a no-op the instant collection is
 * disabled, regardless of what the SDK does with anything already queued.
 */
internal class SentryOperationalDiagnostics(
    private val adapter: SentrySdkAdapter,
    private val sanitizer: DiagnosticSanitizer,
    private val dsn: String,
    private val release: String,
    private val environment: String,
    private val debug: Boolean,
    private val logger: AppLogger,
) : OperationalDiagnostics {

    private var setupSucceeded = false
    private var collectionEnabled = false

    init {
        setupSucceeded = runCatching {
            adapter.setup(dsn = dsn, release = release, environment = environment, debug = debug)
        }.onFailure {
            logger.error(TAG, "Sentry setup failed — diagnostics stays inert for this process.", it)
        }.isSuccess
    }

    override fun captureHandledException(throwable: Throwable, context: DiagnosticContext) {
        if (!setupSucceeded || !collectionEnabled) return
        val tags = sanitizer.sanitizeTags(throwable, context)
        runCatching { adapter.captureException(throwable, tags) }
            .onFailure { logger.error(TAG, "Failed to capture handled exception.", it) }
    }

    override fun addBreadcrumb(breadcrumb: DiagnosticBreadcrumb) {
        if (!setupSucceeded || !collectionEnabled) return
        val safeMessage = sanitizer.sanitizeBreadcrumbMessage(breadcrumb.message) ?: return
        runCatching { adapter.addBreadcrumb(safeMessage, breadcrumb.category) }
            .onFailure { logger.error(TAG, "Failed to add breadcrumb.", it) }
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        collectionEnabled = enabled
    }

    override fun clearContext() {
        if (!setupSucceeded) return
        runCatching { adapter.clearScope() }.onFailure { logger.error(TAG, "Failed to clear scope.", it) }
    }

    override fun configurationStatus(): ProviderConfigurationStatus = when {
        !setupSucceeded -> ProviderConfigurationStatus.INITIALIZATION_FAILED
        !collectionEnabled -> ProviderConfigurationStatus.DISABLED_BY_CONSENT
        else -> ProviderConfigurationStatus.CONFIGURED
    }
}

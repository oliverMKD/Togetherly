package com.togetherly.core.telemetry

import com.togetherly.core.logging.AppLogger

private const val TAG = "DebugOperationalDiagnostics"
private const val REDACTED = "<redacted>"

/**
 * Prints a safe, structured representation of every call to [logger] — the diagnostics-side
 * counterpart to [DebugProductAnalytics]; see that class's own KDoc for why it exists and when
 * it's bound. [collectionEnabled] starts `false` and only [TelemetryCoordinator] flips it — same
 * contract as [DebugProductAnalytics].
 *
 * Unlike event properties, [DiagnosticContext.tags]/[DiagnosticBreadcrumb.message] have no
 * per-key allowlist to check against (they're inherently freeform — see those types' own KDoc), so
 * each value only goes through [TelemetryPrivacyValidator.isSafeText]. An unsafe value is redacted
 * in place rather than dropping the whole report — the exception type/stack itself (never logged
 * here beyond its class name) is still useful developer signal even when one tag isn't safe to
 * show.
 */
class DebugOperationalDiagnostics(
    private val logger: AppLogger,
    private val validator: TelemetryPrivacyValidator = TelemetryPrivacyValidator.default(),
) : OperationalDiagnostics {

    private var collectionEnabled = false

    override fun captureHandledException(throwable: Throwable, context: DiagnosticContext) {
        if (!collectionEnabled) return
        val safeMessage = throwable.message?.let { redactUnlessSafe(it) }
        val safeTags = context.tags.mapValues { (_, value) -> redactUnlessSafe(value) }
        logger.debug(TAG, "captureHandledException type=${throwable::class.simpleName} message=$safeMessage context=$safeTags")
    }

    override fun addBreadcrumb(breadcrumb: DiagnosticBreadcrumb) {
        if (!collectionEnabled) return
        logger.debug(TAG, "addBreadcrumb category=${breadcrumb.category} message=${redactUnlessSafe(breadcrumb.message)}")
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        collectionEnabled = enabled
        logger.debug(TAG, "setCollectionEnabled($enabled)")
    }

    override fun clearContext() {
        logger.debug(TAG, "clearContext")
    }

    override fun configurationStatus(): ProviderConfigurationStatus =
        if (collectionEnabled) ProviderConfigurationStatus.CONFIGURED else ProviderConfigurationStatus.DISABLED_BY_CONSENT

    private fun redactUnlessSafe(value: String): String = if (validator.isSafeText(value)) value else REDACTED
}

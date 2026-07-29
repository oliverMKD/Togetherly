package com.togetherly.core.telemetry

import com.togetherly.core.logging.AppLogger

private const val TAG = "DebugProductAnalytics"

/**
 * Prints a safe, structured representation of every call to [logger] — useful for a developer to
 * confirm the telemetry pipeline actually fires, without a real provider SDK. Bound only in debug
 * builds (see `TelemetryModule.kt`); production builds resolve [NoOpProductAnalytics] instead.
 *
 * Runs every [capture] through [validator] — the exact same allowlist a real provider
 * implementation must use — so what a developer sees printed here is what would actually be sent,
 * not a permissive debug-only shortcut. [collectionEnabled] starts `false` and is never
 * optimistically flipped on; nothing is printed (not even a rejection warning) before
 * [setCollectionEnabled] `true` is called by [TelemetryCoordinator], matching "no analytics event
 * leaves the device before analytics consent" even in this local, nothing-actually-leaves-the-
 * device debug form.
 */
class DebugProductAnalytics(
    private val logger: AppLogger,
    private val validator: TelemetryPrivacyValidator = TelemetryPrivacyValidator.default(),
) : ProductAnalytics {

    private var collectionEnabled = false

    override fun capture(event: AnalyticsEvent) {
        if (!collectionEnabled) return
        when (val result = validator.validateEvent(event)) {
            is TelemetryValidationResult.Rejected ->
                logger.warn(TAG, "Rejected analytics event '${event.name}': ${result.reason}")
            is TelemetryValidationResult.Accepted ->
                logger.debug(TAG, "capture name=${event.name} properties=${result.sanitizedProperties.toSafeString()}")
        }
    }

    override fun screen(screen: AnalyticsScreen) {
        if (!collectionEnabled) return
        logger.debug(TAG, "screen name=${screen.name}")
    }

    override fun flush() {
        if (!collectionEnabled) return
        logger.debug(TAG, "flush")
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        collectionEnabled = enabled
        logger.debug(TAG, "setCollectionEnabled($enabled)")
    }

    override fun reset() {
        logger.debug(TAG, "reset")
    }

    override fun configurationStatus(): ProviderConfigurationStatus =
        if (collectionEnabled) ProviderConfigurationStatus.CONFIGURED else ProviderConfigurationStatus.DISABLED_BY_CONSENT
}

private fun Map<String, AnalyticsValue>.toSafeString(): String =
    entries.joinToString(prefix = "{", postfix = "}") { (key, value) -> "$key=${value.toSafeString()}" }

private fun AnalyticsValue.toSafeString(): String = when (this) {
    is AnalyticsValue.Text -> value
    is AnalyticsValue.Number -> value.toString()
    is AnalyticsValue.Decimal -> value.toString()
    is AnalyticsValue.BooleanValue -> value.toString()
}

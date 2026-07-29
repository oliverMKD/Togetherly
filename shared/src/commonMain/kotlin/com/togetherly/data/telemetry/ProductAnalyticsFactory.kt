package com.togetherly.data.telemetry

import com.togetherly.core.logging.AppLogger
import com.togetherly.core.telemetry.NoOpProductAnalytics
import com.togetherly.core.telemetry.ProductAnalytics
import com.togetherly.core.telemetry.TelemetryPrivacyValidator

private const val TAG = "ProductAnalyticsFactory"

/**
 * The one place "is PostHog configured?" is decided — pulled out of `app/di/TelemetryModule.kt`'s
 * own Koin lambda so it's directly unit-testable (see `ProductAnalyticsFactoryTest`) without
 * spinning up a Koin graph. [adapter]/[commonProperties] are lazy (`() -> `) so neither is ever
 * constructed when [projectKey] is missing — no [PostHogSdkAdapter] instance, no
 * [PostHogCommonProperties] entitlement subscription, nothing PostHog-adjacent happens at all in
 * that case, only [NoOpProductAnalytics].
 *
 * A missing/blank [projectKey] always falls back to [NoOpProductAnalytics] — "missing
 * configuration falls back to no-op analytics," on every build type, so the app remains fully
 * usable. [debug] additionally gets a clear warning log ("debug build reports missing
 * configuration clearly"); a release build stays silent, matching
 * `RevenueCatConfigurator`'s own asymmetry for a missing RevenueCat key.
 */
internal fun createProductAnalytics(
    projectKey: String?,
    host: String?,
    debug: Boolean,
    logger: AppLogger,
    adapter: () -> PostHogSdkAdapter,
    commonProperties: () -> PostHogCommonProperties,
    validator: TelemetryPrivacyValidator = TelemetryPrivacyValidator.default(),
): ProductAnalytics {
    if (projectKey.isNullOrBlank()) {
        if (debug) {
            logger.warn(
                TAG,
                "PostHog project key is missing. See docs/analytics-setup.md for where to place it. Falling back to no-op analytics.",
            )
        }
        return NoOpProductAnalytics()
    }

    return PostHogProductAnalytics(
        adapter = adapter(),
        validator = validator,
        commonProperties = commonProperties(),
        projectKey = projectKey,
        host = host,
        debug = debug,
        logger = logger,
    )
}

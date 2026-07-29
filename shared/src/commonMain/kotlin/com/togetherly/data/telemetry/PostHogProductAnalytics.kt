package com.togetherly.data.telemetry

import com.togetherly.core.logging.AppLogger
import com.togetherly.core.telemetry.AnalyticsEvent
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.AnalyticsValue
import com.togetherly.core.telemetry.ProductAnalytics
import com.togetherly.core.telemetry.ProviderConfigurationStatus
import com.togetherly.core.telemetry.TelemetryPrivacyValidator
import com.togetherly.core.telemetry.TelemetryValidationResult

private const val TAG = "PostHogProductAnalytics"

/**
 * The real [ProductAnalytics] implementation, behind [PostHogSdkAdapter] — no `com.posthog.kmp`
 * type appears in this class's own public surface (it implements the provider-neutral
 * [ProductAnalytics] contract only), and this class is the only place [PostHogSdkAdapter] is
 * called from feature-adjacent code.
 *
 * [setupSucceeded] guards every method — a failed [PostHogSdkAdapter.setup] (a misbehaving 0.x
 * SDK, a network issue during whatever setup does synchronously, a bad host string) leaves this
 * instance permanently, safely inert for the rest of the process rather than retrying or crashing;
 * see this class's own `init` for the one place that failure is caught and logged (never
 * rethrown — "SDK failure isolation" per `docs/analytics-setup.md`).
 *
 * [collectionEnabled] is this class's own defense-in-depth gate, independent of
 * [PostHogSdkAdapter.optIn]/[PostHogSdkAdapter.optOut] — [capture]/[screen]/[flush] check it
 * directly and never reach the adapter at all while disabled, rather than trusting a very-early
 * (0.x) third-party SDK's own opt-out enforcement alone. [com.togetherly.core.telemetry.TelemetryCoordinator]
 * is the only caller of [setCollectionEnabled]; nothing here queues a call made while disabled for
 * later replay.
 *
 * Every property this app ever attaches to an event passes through [validator] first — the same
 * allowlist a [com.togetherly.core.telemetry.DebugProductAnalytics] rejection would also enforce —
 * plus [PostHogCommonProperties.current], which is trusted, hardcoded, low-cardinality data this
 * class itself controls entirely (never sourced from [event]/[screen] or any family content).
 */
internal class PostHogProductAnalytics(
    private val adapter: PostHogSdkAdapter,
    private val validator: TelemetryPrivacyValidator,
    private val commonProperties: PostHogCommonProperties,
    private val projectKey: String,
    private val host: String?,
    private val debug: Boolean,
    private val logger: AppLogger,
) : ProductAnalytics {

    private var setupSucceeded = false
    private var collectionEnabled = false

    init {
        setupSucceeded = runCatching {
            adapter.setup(projectKey = projectKey, host = host, debug = debug)
            commonProperties.start()
        }.onFailure {
            logger.error(TAG, "PostHog setup failed — analytics stays inert for this process.", it)
        }.isSuccess
    }

    override fun capture(event: AnalyticsEvent) {
        if (!setupSucceeded || !collectionEnabled) return
        when (val result = validator.validateEvent(event)) {
            is TelemetryValidationResult.Rejected ->
                logger.warn(TAG, "Rejected analytics event '${event.name}': ${result.reason}")
            is TelemetryValidationResult.Accepted -> {
                val properties = result.sanitizedProperties.toPostHogProperties() + commonProperties.current()
                runCatching { adapter.capture(event.name, properties) }
                    .onFailure { logger.error(TAG, "Failed to capture event '${event.name}'.", it) }
            }
        }
    }

    override fun screen(screen: AnalyticsScreen) {
        if (!setupSucceeded || !collectionEnabled) return
        runCatching { adapter.screen(screen.name, commonProperties.current()) }
            .onFailure { logger.error(TAG, "Failed to capture screen '${screen.name}'.", it) }
    }

    override fun flush() {
        if (!setupSucceeded || !collectionEnabled) return
        runCatching { adapter.flush() }.onFailure { logger.error(TAG, "Flush failed.", it) }
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        collectionEnabled = enabled
        if (!setupSucceeded) return
        runCatching {
            if (enabled) adapter.optIn() else adapter.optOut()
        }.onFailure { logger.error(TAG, "Failed to update collection state.", it) }
    }

    override fun reset() {
        if (!setupSucceeded) return
        runCatching { adapter.reset() }.onFailure { logger.error(TAG, "Reset failed.", it) }
    }

    override fun configurationStatus(): ProviderConfigurationStatus = when {
        !setupSucceeded -> ProviderConfigurationStatus.INITIALIZATION_FAILED
        !collectionEnabled -> ProviderConfigurationStatus.DISABLED_BY_CONSENT
        else -> ProviderConfigurationStatus.CONFIGURED
    }
}

private fun Map<String, AnalyticsValue>.toPostHogProperties(): Map<String, Any> = mapValues { (_, value) ->
    when (value) {
        is AnalyticsValue.Text -> value.value
        is AnalyticsValue.Number -> value.value
        is AnalyticsValue.Decimal -> value.value
        is AnalyticsValue.BooleanValue -> value.value
    }
}

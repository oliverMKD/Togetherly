package com.togetherly.core.telemetry

/**
 * Provider-neutral — nothing here mentions PostHog or any other vendor (see `docs/telemetry.md`).
 * Every implementation must validate through the same [TelemetryPrivacyValidator] a production
 * provider would use before actually sending anything — [DebugProductAnalytics] already does this;
 * a future real provider implementation must too.
 *
 * [setCollectionEnabled] is the one gate [com.togetherly.core.telemetry.TelemetryCoordinator]
 * drives from consent — an implementation must never send/print anything while disabled, and must
 * default to disabled until told otherwise (never optimistically enabled at construction). A
 * future real provider whose SDK supports delayed initialization must defer actually configuring
 * itself until the first `setCollectionEnabled(true)`, never eagerly initializing at construction
 * — see [TelemetryCoordinator]'s own KDoc.
 *
 * [reset] clears any provider-held identity/state (e.g. a distinct-user ID) without itself
 * re-enabling or disabling collection — [TelemetryCoordinator] calls both together when consent is
 * revoked. [flush] must be a no-op when collection is disabled; nothing should ever leave the
 * device outside an explicit, currently-granted consent window.
 */
interface ProductAnalytics {
    fun capture(event: AnalyticsEvent)
    fun screen(screen: AnalyticsScreen)
    fun flush()
    fun setCollectionEnabled(enabled: Boolean)
    fun reset()

    /** See [ProviderConfigurationStatus]'s own KDoc for what each value means and who reports it. */
    fun configurationStatus(): ProviderConfigurationStatus
}

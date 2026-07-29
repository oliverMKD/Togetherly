package com.togetherly.core.telemetry

/** Does nothing, unconditionally — the production binding whenever no real provider is configured yet (see `docs/telemetry.md`). Never validates, never logs, never holds state; there is nothing here for consent to gate because nothing here ever does anything. */
class NoOpProductAnalytics : ProductAnalytics {
    override fun capture(event: AnalyticsEvent) = Unit
    override fun screen(screen: AnalyticsScreen) = Unit
    override fun flush() = Unit
    override fun setCollectionEnabled(enabled: Boolean) = Unit
    override fun reset() = Unit
    override fun configurationStatus(): ProviderConfigurationStatus = ProviderConfigurationStatus.MISSING
}

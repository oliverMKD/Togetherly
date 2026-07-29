package com.togetherly.domain.family

/**
 * [diagnosticsEnabled] defaults to `false` and nothing reads it — Togetherly collected no
 * analytics or diagnostics as of Step 13.1 (explicitly out of scope then; see this feature's own
 * task spec at the time: "Do not add: Analytics in this step"). This exists so a future
 * diagnostics opt-in has a place to persist a family's choice without another Room migration when
 * that day comes — the same forward-looking-plumbing reasoning [com.togetherly.feature.paywall.model.PaywallContext.PREMIUM_PACK]
 * used before Explore's premium packs existed.
 *
 * **This is not that mechanism.** Step 14.1's telemetry consent architecture
 * ([com.togetherly.domain.telemetry.TelemetryConsent]/[com.togetherly.domain.telemetry.repository.TelemetryConsentRepository])
 * needed an independent [com.togetherly.domain.telemetry.ConsentDecision.NotAsked] state distinct
 * from "denied," which a plain `Boolean` can't represent, so it persists separately (see that
 * repository's own KDoc) rather than repurposing this field. [diagnosticsEnabled] remains exactly
 * as inert as it always was; do not wire new UI to it.
 */
data class PrivacyPreferences(
    val diagnosticsEnabled: Boolean,
) {
    companion object {
        fun defaults(): PrivacyPreferences = PrivacyPreferences(diagnosticsEnabled = false)
    }
}

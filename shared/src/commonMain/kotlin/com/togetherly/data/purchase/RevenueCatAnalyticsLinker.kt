package com.togetherly.data.purchase

import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.logging.AppLogger
import com.togetherly.data.telemetry.PostHogSdkAdapter
import com.togetherly.domain.telemetry.ConsentDecision
import com.togetherly.domain.telemetry.repository.TelemetryConsentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "RevenueCatAnalyticsLinker"

/**
 * Associates PostHog's own anonymous distinct id with RevenueCat's `$posthogUserId` reserved
 * subscriber attribute — the one-directional link RevenueCat's official PostHog integration reads
 * to attribute server-verified revenue events to a PostHog identity, without this app ever calling
 * `Purchases.sharedInstance.logIn()` or replacing RevenueCat's own anonymous App User ID. See
 * `docs/revenuecat-posthog-integration.md` for the full dashboard-side setup this backs.
 *
 * Mirrors [com.togetherly.core.telemetry.TelemetryCoordinator]'s own consent-driven shape: [start]
 * is called exactly once, from [com.togetherly.app.di.KoinConfiguration.initKoin], and every
 * subsequent [TelemetryConsentRepository.observeConsent] emission drives [associate]/[clear] purely
 * off [ConsentDecision.Granted] transitions — never before consent is actually granted.
 * [associate] is attempted on every `Granted` emission (not only the edge into it) — cheap and
 * idempotent on RevenueCat's own side, and covers the case where PostHog's anonymous id changed
 * underneath (e.g. after a prior revoke-then-regrant cycle called
 * [com.togetherly.core.telemetry.ProductAnalytics.reset]) without needing a separate signal.
 * [clear] only runs on an actual transition *out of* `Granted`, never on every non-granted
 * emission — nothing to clear on a fresh install that never granted consent in the first place.
 *
 * [associate]/[clear] both run through [RevenueCatDataSource.setPostHogDistinctId], which itself
 * never throws (see that method's own KDoc) — every call here is additionally wrapped in
 * [runCatching] as a second line of defense, so a RevenueCat SDK failure can never break purchases,
 * restores, or this linker's own ability to react to future consent changes. Neither [associate]
 * nor [clear] ever touches [RevenueCatDataSource.getCustomerAccess]/entitlement state — this class
 * only ever writes one subscriber attribute.
 *
 * **Limitation, disclosed rather than silently assumed away**: clearing this attribute on
 * revocation stops *future* RevenueCat-to-PostHog event forwarding from being associated with this
 * distinct id — it does not retroactively unlink revenue events RevenueCat may already have
 * forwarded to PostHog's server side while consent was granted. Neither this app nor RevenueCat's
 * client SDK can reach into PostHog's own server-side records to undo that; see
 * `docs/revenuecat-posthog-integration.md`. This app never promises retroactive deletion from
 * RevenueCat or PostHog in any UI copy.
 */
internal class RevenueCatAnalyticsLinker(
    private val consentRepository: TelemetryConsentRepository,
    private val revenueCatDataSource: RevenueCatDataSource,
    private val postHogSdkAdapter: PostHogSdkAdapter,
    private val logger: AppLogger,
    dispatchers: AppDispatchers,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    private var started = false
    private var lastAnalyticsDecision: ConsentDecision? = null

    fun start() {
        if (started) return
        started = true

        scope.launch {
            runCatching {
                consentRepository.observeConsent().collect { consent -> applyAnalyticsConsent(consent.analytics) }
            }.onFailure { logger.warn(TAG, "Consent observation stopped unexpectedly", it) }
        }
    }

    private fun applyAnalyticsConsent(decision: ConsentDecision) {
        val wasGranted = lastAnalyticsDecision == ConsentDecision.Granted
        lastAnalyticsDecision = decision
        if (decision == ConsentDecision.Granted) {
            associate()
        } else if (wasGranted) {
            clear()
        }
    }

    private fun associate() {
        runCatching {
            val distinctId = postHogSdkAdapter.anonymousId()
            if (!distinctId.isNullOrBlank()) {
                revenueCatDataSource.setPostHogDistinctId(distinctId)
            }
        }.onFailure { logger.warn(TAG, "Failed to associate the PostHog distinct id with RevenueCat", it) }
    }

    private fun clear() {
        runCatching { revenueCatDataSource.setPostHogDistinctId(null) }
            .onFailure { logger.warn(TAG, "Failed to clear the PostHog distinct id on RevenueCat", it) }
    }
}

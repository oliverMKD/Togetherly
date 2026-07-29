package com.togetherly.core.telemetry

import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.logging.AppLogger
import com.togetherly.domain.telemetry.ConsentDecision
import com.togetherly.domain.telemetry.repository.TelemetryConsentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "TelemetryCoordinator"

/**
 * The one place consent turns into provider state — feature code never calls
 * `ProductAnalytics.setCollectionEnabled`/`OperationalDiagnostics.setCollectionEnabled` itself.
 * [start] is called exactly once, from [com.togetherly.app.di.KoinConfiguration.initKoin] (outside
 * any `@Composable`), the same call-site shape
 * [com.togetherly.data.purchase.RevenueCatConfigurator.configure] already establishes.
 *
 * Both providers are forced disabled *before* [start] ever subscribes to
 * [TelemetryConsentRepository.observeConsent] — collection is disabled the instant this
 * coordinator exists, never only "eventually" once the first consent emission arrives. From then
 * on, every emission drives [applyAnalyticsConsent]/[applyDiagnosticsConsent] independently —
 * analytics and diagnostics are never coupled to each other's state.
 *
 * A transition *out of* [ConsentDecision.Granted] (revoking, or never having granted at all) calls
 * the provider's own `reset`/`clearContext` — "revocation must stop future collection promptly"
 * and "resets provider state when consent is revoked" both fall out of this one rule, including the
 * [com.togetherly.domain.localdata.usecase.DeleteAllLocalData] case, which resets consent to
 * [ConsentDecision.NotAsked] the same way a family manually revoking would.
 *
 * There is deliberately no `identify(userId)`-style method anywhere in [ProductAnalytics] for this
 * coordinator to call — granting consent must never itself identify an otherwise-anonymous family;
 * see `docs/telemetry.md`.
 *
 * Every provider call is wrapped in [runCatching] — a throwing provider (a real SDK misbehaving, a
 * future bug) is logged and never rethrown, so a telemetry failure can never break app startup or
 * this coordinator's own ability to keep reacting to future consent changes.
 */
class TelemetryCoordinator(
    private val consentRepository: TelemetryConsentRepository,
    private val analytics: ProductAnalytics,
    private val diagnostics: OperationalDiagnostics,
    private val logger: AppLogger,
    dispatchers: AppDispatchers,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    private var started = false
    private var lastAnalyticsDecision: ConsentDecision? = null
    private var lastDiagnosticsDecision: ConsentDecision? = null

    fun start() {
        if (started) return
        started = true

        safely("disable analytics at startup") { analytics.setCollectionEnabled(false) }
        safely("disable diagnostics at startup") { diagnostics.setCollectionEnabled(false) }

        scope.launch {
            runCatching {
                consentRepository.observeConsent().collect { consent ->
                    applyAnalyticsConsent(consent.analytics)
                    applyDiagnosticsConsent(consent.diagnostics)
                }
            }.onFailure { logger.warn(TAG, "Telemetry consent observation stopped unexpectedly", it) }
        }
    }

    /** A no-op whenever analytics consent isn't currently [ConsentDecision.Granted] — "flushes only when collection is enabled." */
    fun flush() {
        if (lastAnalyticsDecision != ConsentDecision.Granted) return
        safely("flush analytics") { analytics.flush() }
    }

    private fun applyAnalyticsConsent(decision: ConsentDecision) {
        val wasGranted = lastAnalyticsDecision == ConsentDecision.Granted
        lastAnalyticsDecision = decision
        val granted = decision == ConsentDecision.Granted
        safely("update analytics collection state") { analytics.setCollectionEnabled(granted) }
        if (wasGranted && !granted) {
            safely("reset analytics provider") { analytics.reset() }
        }
    }

    private fun applyDiagnosticsConsent(decision: ConsentDecision) {
        val wasGranted = lastDiagnosticsDecision == ConsentDecision.Granted
        lastDiagnosticsDecision = decision
        val granted = decision == ConsentDecision.Granted
        safely("update diagnostics collection state") { diagnostics.setCollectionEnabled(granted) }
        if (wasGranted && !granted) {
            safely("clear diagnostics context") { diagnostics.clearContext() }
        }
    }

    private inline fun safely(action: String, block: () -> Unit) {
        runCatching(block).onFailure { logger.warn(TAG, "Failed to $action", it) }
    }
}

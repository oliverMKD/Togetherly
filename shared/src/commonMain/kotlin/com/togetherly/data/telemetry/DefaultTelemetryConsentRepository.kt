package com.togetherly.data.telemetry

import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.logging.AppLogger
import com.togetherly.domain.telemetry.ConsentDecision
import com.togetherly.domain.telemetry.TelemetryConsent
import com.togetherly.domain.telemetry.repository.TelemetryConsentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "TelemetryConsent"

/**
 * Seeded free — [TelemetryConsent.default] — and reconciled from [TelemetryConsentCache]
 * asynchronously in [init], the same "seed a safe default immediately, load the real state in the
 * background" shape [com.togetherly.data.purchase.RevenueCatEntitlementRepository] already uses,
 * on its own long-lived [scope] rather than a caller's (this singleton lives as long as the app
 * process). [observeConsent] is backed by in-memory state precisely so it never blocks on a Room
 * read — [com.togetherly.core.telemetry.TelemetryCoordinator] subscribes to it from app startup,
 * before a caller can meaningfully await a suspend load.
 *
 * Every persistence operation is wrapped so a storage exception can never propagate to a caller —
 * see [TelemetryConsentRepository]'s own KDoc on why no method here returns `DataResult`. A save
 * failure is logged and otherwise swallowed: in-memory state (what every observer and every
 * `ProductAnalytics`/`OperationalDiagnostics` gating decision actually reads) is always correct
 * immediately, even if the persisted copy briefly lags behind after an I/O failure.
 */
internal class DefaultTelemetryConsentRepository(
    private val cache: TelemetryConsentCache,
    private val logger: AppLogger,
    dispatchers: AppDispatchers,
) : TelemetryConsentRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val _consent = MutableStateFlow(TelemetryConsent.default())

    init {
        scope.launch {
            cache.load()?.let { loaded -> _consent.value = loaded }
        }
    }

    override fun observeConsent(): Flow<TelemetryConsent> = _consent.asStateFlow()

    override suspend fun updateAnalyticsConsent(decision: ConsentDecision) {
        _consent.update { it.copy(analytics = decision) }
        persist()
    }

    override suspend fun updateDiagnosticsConsent(decision: ConsentDecision) {
        _consent.update { it.copy(diagnostics = decision) }
        persist()
    }

    /** Resets in-memory state immediately, then best-effort deletes the persisted row — see [TelemetryConsentCache.clear]'s own KDoc for the transmitted-data distinction this does not cover. */
    override suspend fun resetConsent() {
        _consent.value = TelemetryConsent.default()
        runCatching { cache.clear() }
            .onFailure { logger.warn(TAG, "Failed to clear persisted telemetry consent", it) }
    }

    private suspend fun persist() {
        runCatching { cache.save(_consent.value) }
            .onFailure { logger.warn(TAG, "Failed to persist telemetry consent", it) }
    }
}

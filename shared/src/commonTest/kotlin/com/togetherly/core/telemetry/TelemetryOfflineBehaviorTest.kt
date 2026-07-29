package com.togetherly.core.telemetry

import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.logging.FakeAppLogger
import com.togetherly.domain.telemetry.ConsentDecision
import com.togetherly.domain.telemetry.TelemetryConsent
import com.togetherly.domain.telemetry.repository.FakeTelemetryConsentRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Step 14.6's offline-behavior suite for the analytics/diagnostics provider side of telemetry.
 * Togetherly is offline-first by construction (see `docs/telemetry.md`, `docs/privacy.md`): a
 * family with no network connection, or a fresh install with no PostHog/Sentry key configured at
 * all ([NoOpProductAnalytics]/[NoOpOperationalDiagnostics] — [ProviderConfigurationStatus.MISSING]),
 * must be able to complete onboarding, grant/revoke consent, and use every quest/memory/journey
 * feature exactly as if telemetry didn't exist. This file makes that guarantee explicit rather than
 * only implicit in each component's own KDoc:
 *
 * - [NoOpProductAnalytics]/[NoOpOperationalDiagnostics] — the actual production binding while
 *   unconfigured — never throw and never need a network connection, by construction (every method
 *   is a no-op).
 * - [TelemetryCoordinator] keeps reacting correctly to every consent change across a full
 *   grant/revoke/reset lifecycle even when the bound provider is completely unavailable (modeled
 *   here as a provider that throws on every call, the strictest possible stand-in for "offline").
 *
 * See [com.togetherly.data.telemetry.TelemetryConsentOfflineBehaviorTest] for the consent
 * *persistence* half of this same Step 14.6 requirement (local storage I/O failure resilience —
 * consent itself already has zero network dependency, Room-backed only), and
 * `PostHogProductAnalyticsTest`/`SentryOperationalDiagnosticsTest` (`data/telemetry`) for the real
 * provider adapters' own "a network-style adapter failure never throws" coverage, which this file
 * deliberately does not re-test at that lower level.
 */
class TelemetryOfflineBehaviorTest {

    // ==================== NoOp providers: the actual "no key configured" / offline binding ====================

    @Test
    fun noOpAnalyticsNeverThrowsRegardlessOfCollectionStateAndReportsMissing() {
        val analytics = NoOpProductAnalytics()

        analytics.setCollectionEnabled(true)
        analytics.capture(OnboardingCompleted)
        analytics.screen(AnalyticsScreen.TODAY)
        analytics.flush()
        analytics.reset()
        analytics.setCollectionEnabled(false)

        assertEquals(ProviderConfigurationStatus.MISSING, analytics.configurationStatus())
    }

    @Test
    fun noOpDiagnosticsNeverThrowsRegardlessOfCollectionStateAndReportsMissing() {
        val diagnostics = NoOpOperationalDiagnostics()

        diagnostics.setCollectionEnabled(true)
        diagnostics.captureHandledException(IllegalStateException("test"), DiagnosticContext())
        diagnostics.addBreadcrumb(DiagnosticBreadcrumb(message = "test"))
        diagnostics.clearContext()
        diagnostics.setCollectionEnabled(false)

        assertEquals(ProviderConfigurationStatus.MISSING, diagnostics.configurationStatus())
    }

    // ==================== TelemetryCoordinator: full lifecycle with an always-failing provider ====================

    private fun coordinator(
        consentRepository: FakeTelemetryConsentRepository,
        analytics: FakeProductAnalytics,
        diagnostics: FakeOperationalDiagnostics,
        logger: FakeAppLogger,
    ) = TelemetryCoordinator(
        consentRepository = consentRepository,
        analytics = analytics,
        diagnostics = diagnostics,
        logger = logger,
        dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
    )

    @Test
    fun consentDrivenLifecycleCompletesNormallyAcrossMultipleChangesEvenWhenTheAnalyticsProviderIsUnavailableAtStart() = runTest {
        // A provider that fails the very first call it ever receives is the closest single-shot
        // fake stand-in for "unreachable when the app launches" — TelemetryCoordinator must still
        // finish wiring itself up and keep responding to every later consent change normally.
        val analytics = FakeProductAnalytics().apply { throwOnNextSetCollectionEnabled(IllegalStateException("unavailable")) }
        val diagnostics = FakeOperationalDiagnostics()
        val consentRepository = FakeTelemetryConsentRepository()
        val logger = FakeAppLogger()
        val coordinator = coordinator(consentRepository, analytics, diagnostics, logger)

        coordinator.start()
        assertTrue(logger.calls.any { it.level == "warn" }, "The startup failure must be logged, not silently swallowed")

        // The whole point: later consent changes are unaffected by that one failed call — neither
        // provider, and neither this coordinator, is left permanently broken.
        consentRepository.setConsent(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.Granted))
        assertTrue(analytics.collectionEnabled)
        assertTrue(diagnostics.collectionEnabled)

        consentRepository.setConsent(TelemetryConsent(ConsentDecision.Denied, ConsentDecision.Denied))
        assertEquals(1, analytics.resetCallCount)
        assertEquals(1, diagnostics.clearContextCallCount)
    }

    @Test
    fun aProviderReportingMissingConfigurationStillReceivesNormalConsentDrivenCalls() = runTest {
        // configurationStatus() (MISSING here — "no key configured", the production offline state)
        // is read only by the debug telemetry screen, never by TelemetryCoordinator's own control
        // flow — consent gating must be completely decoupled from whether a provider considers
        // itself configured.
        val analytics = FakeProductAnalytics().apply { configurationStatusValue = ProviderConfigurationStatus.MISSING }
        val diagnostics = FakeOperationalDiagnostics().apply { configurationStatusValue = ProviderConfigurationStatus.MISSING }
        val consentRepository = FakeTelemetryConsentRepository()
        val coordinator = coordinator(consentRepository, analytics, diagnostics, FakeAppLogger())
        coordinator.start()

        consentRepository.setConsent(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.Granted))

        assertTrue(analytics.collectionEnabled)
        assertTrue(diagnostics.collectionEnabled)
        assertEquals(ProviderConfigurationStatus.MISSING, analytics.configurationStatus())
        assertEquals(ProviderConfigurationStatus.MISSING, diagnostics.configurationStatus())
    }
}

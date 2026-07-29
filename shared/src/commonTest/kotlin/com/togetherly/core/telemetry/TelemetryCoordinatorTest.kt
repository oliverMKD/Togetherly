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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [UnconfinedTestDispatcher] so [TelemetryCoordinator]'s own internally-owned
 * [kotlinx.coroutines.CoroutineScope] runs its `observeConsent().collect { ... }` loop eagerly and
 * synchronously within each test body — the same reasoning
 * `com.togetherly.data.purchase.RevenueCatEntitlementRepositoryTest` documents for testing a
 * component that owns its own long-lived scope.
 */
class TelemetryCoordinatorTest {

    private fun coordinator(
        consentRepository: FakeTelemetryConsentRepository = FakeTelemetryConsentRepository(),
        analytics: FakeProductAnalytics = FakeProductAnalytics(),
        diagnostics: FakeOperationalDiagnostics = FakeOperationalDiagnostics(),
        logger: FakeAppLogger = FakeAppLogger(),
    ) = TelemetryCoordinator(
        consentRepository = consentRepository,
        analytics = analytics,
        diagnostics = diagnostics,
        logger = logger,
        dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
    )

    @Test
    fun startsWithBothProvidersDisabled() = runTest {
        val analytics = FakeProductAnalytics()
        val diagnostics = FakeOperationalDiagnostics()
        val coordinator = coordinator(analytics = analytics, diagnostics = diagnostics)

        coordinator.start()

        assertFalse(analytics.collectionEnabled)
        assertFalse(diagnostics.collectionEnabled)
    }

    @Test
    fun grantingAnalyticsOnlyEnablesOnlyAnalytics() = runTest {
        val analytics = FakeProductAnalytics()
        val diagnostics = FakeOperationalDiagnostics()
        val consentRepository = FakeTelemetryConsentRepository()
        val coordinator = coordinator(consentRepository, analytics, diagnostics)
        coordinator.start()

        consentRepository.setConsent(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.NotAsked))

        assertTrue(analytics.collectionEnabled)
        assertFalse(diagnostics.collectionEnabled)
    }

    @Test
    fun grantingDiagnosticsOnlyEnablesOnlyDiagnostics() = runTest {
        val analytics = FakeProductAnalytics()
        val diagnostics = FakeOperationalDiagnostics()
        val consentRepository = FakeTelemetryConsentRepository()
        val coordinator = coordinator(consentRepository, analytics, diagnostics)
        coordinator.start()

        consentRepository.setConsent(TelemetryConsent(ConsentDecision.NotAsked, ConsentDecision.Granted))

        assertFalse(analytics.collectionEnabled)
        assertTrue(diagnostics.collectionEnabled)
    }

    @Test
    fun grantingBothEnablesBoth() = runTest {
        val analytics = FakeProductAnalytics()
        val diagnostics = FakeOperationalDiagnostics()
        val consentRepository = FakeTelemetryConsentRepository()
        val coordinator = coordinator(consentRepository, analytics, diagnostics)
        coordinator.start()

        consentRepository.setConsent(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.Granted))

        assertTrue(analytics.collectionEnabled)
        assertTrue(diagnostics.collectionEnabled)
    }

    @Test
    fun denyingBothKeepsBothDisabled() = runTest {
        val analytics = FakeProductAnalytics()
        val diagnostics = FakeOperationalDiagnostics()
        val consentRepository = FakeTelemetryConsentRepository()
        val coordinator = coordinator(consentRepository, analytics, diagnostics)
        coordinator.start()

        consentRepository.setConsent(TelemetryConsent(ConsentDecision.Denied, ConsentDecision.Denied))

        assertFalse(analytics.collectionEnabled)
        assertFalse(diagnostics.collectionEnabled)
    }

    @Test
    fun revokingAnalyticsAfterGrantingDisablesAndResetsOnlyAnalytics() = runTest {
        val analytics = FakeProductAnalytics()
        val diagnostics = FakeOperationalDiagnostics()
        val consentRepository = FakeTelemetryConsentRepository()
        val coordinator = coordinator(consentRepository, analytics, diagnostics)
        coordinator.start()
        consentRepository.setConsent(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.Granted))

        consentRepository.setConsent(TelemetryConsent(ConsentDecision.Denied, ConsentDecision.Granted))

        assertFalse(analytics.collectionEnabled)
        assertEquals(1, analytics.resetCallCount)
        assertTrue(diagnostics.collectionEnabled)
        assertEquals(0, diagnostics.clearContextCallCount)
    }

    @Test
    fun revokingDiagnosticsAfterGrantingClearsOnlyDiagnosticsContext() = runTest {
        val analytics = FakeProductAnalytics()
        val diagnostics = FakeOperationalDiagnostics()
        val consentRepository = FakeTelemetryConsentRepository()
        val coordinator = coordinator(consentRepository, analytics, diagnostics)
        coordinator.start()
        consentRepository.setConsent(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.Granted))

        consentRepository.setConsent(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.Denied))

        assertTrue(analytics.collectionEnabled)
        assertEquals(0, analytics.resetCallCount)
        assertFalse(diagnostics.collectionEnabled)
        assertEquals(1, diagnostics.clearContextCallCount)
    }

    @Test
    fun resettingConsentToNotAskedAfterGrantingAlsoResetsProviders() = runTest {
        val analytics = FakeProductAnalytics()
        val diagnostics = FakeOperationalDiagnostics()
        val consentRepository = FakeTelemetryConsentRepository()
        val coordinator = coordinator(consentRepository, analytics, diagnostics)
        coordinator.start()
        consentRepository.setConsent(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.Granted))

        consentRepository.resetConsent()

        assertFalse(analytics.collectionEnabled)
        assertEquals(1, analytics.resetCallCount)
        assertFalse(diagnostics.collectionEnabled)
        assertEquals(1, diagnostics.clearContextCallCount)
    }

    @Test
    fun aThrowingProviderDuringStartNeverPropagatesAndOtherProviderStillWorks() = runTest {
        val analytics = FakeProductAnalytics().apply { throwOnNextSetCollectionEnabled(IllegalStateException("provider misbehaving")) }
        val diagnostics = FakeOperationalDiagnostics()
        val consentRepository = FakeTelemetryConsentRepository()
        val logger = FakeAppLogger()
        val coordinator = coordinator(consentRepository, analytics, diagnostics, logger)

        // Must not throw — a telemetry provider failure can never break app startup.
        coordinator.start()

        assertTrue(logger.calls.any { it.level == "warn" })
        consentRepository.setConsent(TelemetryConsent(ConsentDecision.NotAsked, ConsentDecision.Granted))
        assertTrue(diagnostics.collectionEnabled, "A failure in the analytics provider must not stop diagnostics from still reacting to consent")
    }
}

package com.togetherly.feature.debug.presentation

import app.cash.turbine.test
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.logging.FakeAppLogger
import com.togetherly.core.telemetry.DuplicateSignalDetector
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import com.togetherly.core.telemetry.FakeProductAnalytics
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.core.telemetry.ProductAnalytics
import com.togetherly.core.telemetry.ProviderConfigurationStatus
import com.togetherly.core.telemetry.TelemetryDebugRecorder
import com.togetherly.data.purchase.RevenueCatConfigurator
import com.togetherly.data.telemetry.DebugRecordingOperationalDiagnostics
import com.togetherly.data.telemetry.DebugRecordingProductAnalytics
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import com.togetherly.domain.telemetry.ConsentDecision
import com.togetherly.domain.telemetry.TelemetryConsent
import com.togetherly.domain.telemetry.repository.FakeTelemetryConsentRepository
import com.togetherly.feature.debug.model.DebugTelemetryAction
import com.togetherly.feature.debug.model.DebugTelemetryEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

private fun accessSnapshot(access: FamilyAccess = FamilyAccess.free()) = AccessSnapshot(
    familyAccess = access,
    activeEntitlements = emptySet(),
    verifiedAt = Instant.fromEpochSeconds(0),
)

@OptIn(ExperimentalCoroutinesApi::class)
class DebugTelemetryViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        consentRepository: FakeTelemetryConsentRepository = FakeTelemetryConsentRepository(),
        productAnalytics: ProductAnalytics = FakeProductAnalytics(),
        diagnostics: OperationalDiagnostics = FakeOperationalDiagnostics(),
        entitlementRepository: FakeEntitlementRepository = FakeEntitlementRepository(accessSnapshot()),
        recorder: TelemetryDebugRecorder = TelemetryDebugRecorder(TestAppClock(Instant.fromEpochSeconds(0))),
    ) = DebugTelemetryViewModel(
        consentRepository = consentRepository,
        productAnalytics = productAnalytics,
        diagnostics = diagnostics,
        revenueCatConfigurator = RevenueCatConfigurator(
            apiKeyProvider = { null },
            logger = FakeAppLogger(),
            diagnostics = diagnostics,
        ),
        entitlementRepository = entitlementRepository,
        recorder = recorder,
    )

    @Test
    fun initialStateReflectsConsentProviderStatusAndAccess() {
        val consentRepository = FakeTelemetryConsentRepository()
        consentRepository.setConsent(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.Denied))
        val productAnalytics = FakeProductAnalytics().apply { configurationStatusValue = ProviderConfigurationStatus.CONFIGURED }
        val diagnostics = FakeOperationalDiagnostics().apply { configurationStatusValue = ProviderConfigurationStatus.DISABLED_BY_CONSENT }
        val model = viewModel(consentRepository = consentRepository, productAnalytics = productAnalytics, diagnostics = diagnostics)

        assertEquals(ConsentDecision.Granted, model.uiState.value.analyticsConsent)
        assertEquals(ConsentDecision.Denied, model.uiState.value.diagnosticsConsent)
        assertEquals(ProviderConfigurationStatus.CONFIGURED, model.uiState.value.postHogStatus)
        assertEquals(ProviderConfigurationStatus.DISABLED_BY_CONSENT, model.uiState.value.sentryStatus)
        assertEquals(ProviderConfigurationStatus.MISSING, model.uiState.value.revenueCatStatus, "No RevenueCat key configured — NotConfigured maps to MISSING")
        assertEquals("Free", model.uiState.value.accessSummary)
    }

    @Test
    fun familyPlusSubscriptionAccessIsSummarizedClearly() {
        val entitlementRepository = FakeEntitlementRepository(accessSnapshot(FamilyAccess.subscription(Instant.fromEpochSeconds(1_000), willRenew = true)))
        val model = viewModel(entitlementRepository = entitlementRepository)

        assertEquals("Family Plus (subscription)", model.uiState.value.accessSummary)
    }

    @Test
    fun backClickedEmitsNavigateBack() = runTest {
        val model = viewModel()

        model.events.test {
            model.onAction(DebugTelemetryAction.BackClicked)
            assertEquals(DebugTelemetryEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun flushClickedCallsFlushOnProductAnalytics() {
        val productAnalytics = FakeProductAnalytics()
        val model = viewModel(productAnalytics = productAnalytics)

        model.onAction(DebugTelemetryAction.FlushClicked)

        assertEquals(1, productAnalytics.flushCallCount)
    }

    /**
     * Mirrors production DI exactly (`app/di/TelemetryModule.kt`): the [DebugTelemetryViewModel]'s
     * own `productAnalytics`/`diagnostics` params are always the debug-decorated instance on a
     * debug build, never the bare real/fake provider directly — a bare fake here would never write
     * anything into [recorder] at all, since only the decorator does that recording.
     */
    @Test
    fun sendTestEventClickedCapturesADebugEventAndRefreshesRecentEvents() {
        val fakeDelegate = FakeProductAnalytics()
        val recorder = TelemetryDebugRecorder(TestAppClock(Instant.fromEpochSeconds(0)))
        val decorated = DebugRecordingProductAnalytics(
            delegate = fakeDelegate,
            recorder = recorder,
            duplicateDetector = DuplicateSignalDetector(TestAppClock(Instant.fromEpochSeconds(0)), FakeAppLogger()),
        )
        decorated.setCollectionEnabled(true)
        val model = viewModel(productAnalytics = decorated, recorder = recorder)

        model.onAction(DebugTelemetryAction.SendTestEventClicked)

        assertEquals(1, fakeDelegate.capturedEvents.size)
        assertEquals("debug_test_event", model.uiState.value.recentEvents.single().name)
        assertTrue(model.uiState.value.testEventJustSent)
    }

    @Test
    fun sendTestExceptionClickedCapturesAHandledExceptionAndRefreshesRecentErrors() {
        val fakeDelegate = FakeOperationalDiagnostics()
        val recorder = TelemetryDebugRecorder(TestAppClock(Instant.fromEpochSeconds(0)))
        val decorated = DebugRecordingOperationalDiagnostics(
            delegate = fakeDelegate,
            recorder = recorder,
            duplicateDetector = DuplicateSignalDetector(TestAppClock(Instant.fromEpochSeconds(0)), FakeAppLogger()),
        )
        decorated.setCollectionEnabled(true)
        val model = viewModel(diagnostics = decorated, recorder = recorder)

        model.onAction(DebugTelemetryAction.SendTestExceptionClicked)

        assertEquals(1, fakeDelegate.capturedExceptions.size)
        assertEquals(1, model.uiState.value.recentProviderErrors.size)
        assertTrue(model.uiState.value.testExceptionJustSent)
    }

    @Test
    fun clearHistoryClickedClearsTheRecorderAndTheDisplayedHistory() {
        val recorder = TelemetryDebugRecorder(TestAppClock(Instant.fromEpochSeconds(0)))
        recorder.recordEvent("quest_completed", emptyMap())
        val model = viewModel(recorder = recorder)
        assertTrue(model.uiState.value.recentEvents.isNotEmpty())

        model.onAction(DebugTelemetryAction.ClearHistoryClicked)

        assertTrue(model.uiState.value.recentEvents.isEmpty())
        assertTrue(recorder.recentEvents().isEmpty())
    }

    @Test
    fun refreshClickedRePullsTheLatestConfigurationStatus() {
        val productAnalytics = FakeProductAnalytics()
        val model = viewModel(productAnalytics = productAnalytics)
        productAnalytics.configurationStatusValue = ProviderConfigurationStatus.INITIALIZATION_FAILED

        model.onAction(DebugTelemetryAction.RefreshClicked)

        assertEquals(ProviderConfigurationStatus.INITIALIZATION_FAILED, model.uiState.value.postHogStatus)
    }
}

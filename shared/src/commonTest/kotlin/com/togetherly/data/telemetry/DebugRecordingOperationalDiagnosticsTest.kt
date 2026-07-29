package com.togetherly.data.telemetry

import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.logging.FakeAppLogger
import com.togetherly.core.telemetry.DiagnosticBreadcrumb
import com.togetherly.core.telemetry.DiagnosticContext
import com.togetherly.core.telemetry.DuplicateSignalDetector
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import com.togetherly.core.telemetry.ProviderConfigurationStatus
import com.togetherly.core.telemetry.TelemetryDebugRecorder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

private class DebugRecordingSampleException : Exception("contact parent@example.com for the real error")

class DebugRecordingOperationalDiagnosticsTest {

    private fun decorator(
        delegate: FakeOperationalDiagnostics = FakeOperationalDiagnostics(),
        recorder: TelemetryDebugRecorder = TelemetryDebugRecorder(TestAppClock(Instant.fromEpochSeconds(0))),
    ) = DebugRecordingOperationalDiagnostics(
        delegate = delegate,
        recorder = recorder,
        duplicateDetector = DuplicateSignalDetector(TestAppClock(Instant.fromEpochSeconds(0)), FakeAppLogger()),
    )

    @Test
    fun everyCaptureIsForwardedToTheRealDelegateUnchanged() {
        val delegate = FakeOperationalDiagnostics()
        val debug = decorator(delegate = delegate)
        debug.setCollectionEnabled(true)
        val throwable = DebugRecordingSampleException()
        val context = DiagnosticContext(mapOf("feature" to "purchase"))

        debug.captureHandledException(throwable, context)

        assertEquals(throwable, delegate.capturedExceptions.single().throwable)
        assertEquals(context, delegate.capturedExceptions.single().context)
    }

    @Test
    fun noPreConsentProviderErrorIsEverRecorded() {
        val recorder = TelemetryDebugRecorder(TestAppClock(Instant.fromEpochSeconds(0)))
        val debug = decorator(recorder = recorder)

        debug.captureHandledException(DebugRecordingSampleException(), DiagnosticContext())

        assertTrue(recorder.recentProviderErrors().isEmpty(), "Nothing may be retained — even locally — before setCollectionEnabled(true)")
    }

    @Test
    fun onlyTheExceptionClassNameIsRecordedNeverTheMessageOnceCollectionIsEnabled() {
        val recorder = TelemetryDebugRecorder(TestAppClock(Instant.fromEpochSeconds(0)))
        val debug = decorator(recorder = recorder)
        debug.setCollectionEnabled(true)

        debug.captureHandledException(DebugRecordingSampleException(), DiagnosticContext())

        assertEquals("DebugRecordingSampleException", recorder.recentProviderErrors().single().category)
    }

    @Test
    fun aSafeBreadcrumbIsRecordedAndForwardedOnceCollectionIsEnabled() {
        val recorder = TelemetryDebugRecorder(TestAppClock(Instant.fromEpochSeconds(0)))
        val delegate = FakeOperationalDiagnostics()
        val debug = decorator(delegate = delegate, recorder = recorder)
        debug.setCollectionEnabled(true)

        debug.addBreadcrumb(DiagnosticBreadcrumb("catalogue load started", "content"))

        assertEquals("catalogue load started", recorder.recentBreadcrumbs().single().message)
        assertEquals(1, delegate.breadcrumbs.size)
    }

    @Test
    fun noPreConsentBreadcrumbIsEverRecordedButIsStillForwarded() {
        val recorder = TelemetryDebugRecorder(TestAppClock(Instant.fromEpochSeconds(0)))
        val delegate = FakeOperationalDiagnostics()
        delegate.setCollectionEnabled(true)
        val debug = decorator(delegate = delegate, recorder = recorder)
        val breadcrumb = DiagnosticBreadcrumb("catalogue load started")

        debug.addBreadcrumb(breadcrumb)

        assertTrue(recorder.recentBreadcrumbs().isEmpty())
        assertEquals(listOf(breadcrumb), delegate.breadcrumbs)
    }

    @Test
    fun anUnsafeBreadcrumbIsNeverRecordedButIsStillForwardedToTheDelegate() {
        val recorder = TelemetryDebugRecorder(TestAppClock(Instant.fromEpochSeconds(0)))
        val delegate = FakeOperationalDiagnostics()
        val debug = decorator(delegate = delegate, recorder = recorder)
        debug.setCollectionEnabled(true)
        val unsafeBreadcrumb = DiagnosticBreadcrumb("contact parent@example.com")

        debug.addBreadcrumb(unsafeBreadcrumb)

        assertTrue(recorder.recentBreadcrumbs().isEmpty())
        assertEquals(listOf(unsafeBreadcrumb), delegate.breadcrumbs)
    }

    @Test
    fun setCollectionEnabledAndClearContextDelegateDirectly() {
        val delegate = FakeOperationalDiagnostics()
        val debug = decorator(delegate = delegate)

        debug.setCollectionEnabled(true)
        debug.clearContext()

        assertTrue(delegate.collectionEnabled)
        assertEquals(1, delegate.clearContextCallCount)
    }

    @Test
    fun configurationStatusDelegatesToTheRealProvider() {
        val delegate = FakeOperationalDiagnostics()
        delegate.configurationStatusValue = ProviderConfigurationStatus.DISABLED_BY_CONSENT
        val debug = decorator(delegate = delegate)

        assertEquals(ProviderConfigurationStatus.DISABLED_BY_CONSENT, debug.configurationStatus())
    }
}

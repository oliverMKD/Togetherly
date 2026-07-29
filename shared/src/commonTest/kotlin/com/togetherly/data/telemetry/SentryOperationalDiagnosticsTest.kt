package com.togetherly.data.telemetry

import com.togetherly.core.logging.FakeAppLogger
import com.togetherly.core.telemetry.DiagnosticBreadcrumb
import com.togetherly.core.telemetry.DiagnosticContext
import com.togetherly.core.telemetry.DiagnosticSanitizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class SampleException(message: String? = null) : Exception(message)

class SentryOperationalDiagnosticsTest {

    private fun diagnostics(
        adapter: FakeSentrySdkAdapter = FakeSentrySdkAdapter(),
        sanitizer: DiagnosticSanitizer = DiagnosticSanitizer(),
        logger: FakeAppLogger = FakeAppLogger(),
    ) = SentryOperationalDiagnostics(
        adapter = adapter,
        sanitizer = sanitizer,
        dsn = "https://example@o1.ingest.sentry.io/1",
        release = "togetherly@1.0+1",
        environment = "debug",
        debug = true,
        logger = logger,
    )

    @Test
    fun setupRunsOnConstructionWithTheSuppliedReleaseAndEnvironment() {
        val adapter = FakeSentrySdkAdapter()

        diagnostics(adapter = adapter)

        assertEquals(1, adapter.setupCalls.size)
        assertEquals("togetherly@1.0+1", adapter.setupCalls.single().release)
        assertEquals("debug", adapter.setupCalls.single().environment)
    }

    @Test
    fun disabledByDefaultCaptureIsANoOp() {
        val adapter = FakeSentrySdkAdapter()
        val instance = diagnostics(adapter = adapter)

        instance.captureHandledException(SampleException(), DiagnosticContext())

        assertTrue(adapter.captureCalls.isEmpty())
    }

    @Test
    fun disabledByDefaultBreadcrumbIsANoOp() {
        val adapter = FakeSentrySdkAdapter()
        val instance = diagnostics(adapter = adapter)

        instance.addBreadcrumb(DiagnosticBreadcrumb("catalogue load started"))

        assertTrue(adapter.breadcrumbCalls.isEmpty())
    }

    @Test
    fun grantedConsentAllowsCaptureToReachTheAdapter() {
        val adapter = FakeSentrySdkAdapter()
        val instance = diagnostics(adapter = adapter)
        instance.setCollectionEnabled(true)

        instance.captureHandledException(SampleException(), DiagnosticContext(mapOf("feature" to "purchase")))

        assertEquals(1, adapter.captureCalls.size)
        assertEquals("purchase", adapter.captureCalls.single().tags["feature"])
    }

    @Test
    fun revokedConsentStopsFutureCapture() {
        val adapter = FakeSentrySdkAdapter()
        val instance = diagnostics(adapter = adapter)
        instance.setCollectionEnabled(true)
        instance.captureHandledException(SampleException(), DiagnosticContext())

        instance.setCollectionEnabled(false)
        instance.captureHandledException(SampleException(), DiagnosticContext())

        assertEquals(1, adapter.captureCalls.size, "Only the capture made while enabled should have reached the adapter")
    }

    @Test
    fun clearContextCallsAdapterClearScope() {
        val adapter = FakeSentrySdkAdapter()
        val instance = diagnostics(adapter = adapter)

        instance.clearContext()

        assertEquals(1, adapter.clearScopeCallCount)
    }

    @Test
    fun clearContextNeverCallsAdapterClose() {
        val adapter = FakeSentrySdkAdapter()
        val instance = diagnostics(adapter = adapter)

        instance.setCollectionEnabled(false)
        instance.clearContext()

        assertEquals(0, adapter.closeCallCount, "close() flushes/sends pending events — the opposite of discarding them on revocation")
    }

    @Test
    fun unexpectedExceptionIsReportedWithSanitizedTags() {
        val adapter = FakeSentrySdkAdapter()
        val instance = diagnostics(adapter = adapter)
        instance.setCollectionEnabled(true)

        instance.captureHandledException(
            SampleException(),
            DiagnosticContext(mapOf("feature" to "content", "leaked_email" to "parent@example.com")),
        )

        val call = adapter.captureCalls.single()
        assertEquals("content", call.tags["feature"])
        assertTrue("leaked_email" !in call.tags, "An unsafe tag value must be dropped, never forwarded to the real provider")
        assertEquals("SampleException", call.tags["exception_type"])
    }

    @Test
    fun aSetupFailureIsolatesTheProviderPermanently() {
        val adapter = FakeSentrySdkAdapter()
        adapter.throwOnNextSetup(IllegalStateException("Sentry.init blew up"))
        val instance = diagnostics(adapter = adapter)
        instance.setCollectionEnabled(true)

        instance.captureHandledException(SampleException(), DiagnosticContext())
        instance.clearContext()

        assertTrue(adapter.captureCalls.isEmpty(), "A failed setup must permanently disable this instance, not just this one call")
        assertEquals(0, adapter.clearScopeCallCount)
    }

    @Test
    fun aThrowingAdapterCaptureNeverPropagates() {
        val adapter = FakeSentrySdkAdapter()
        val instance = diagnostics(adapter = adapter)
        instance.setCollectionEnabled(true)
        adapter.throwOnNextCapture(IllegalStateException("native SDK misbehaving"))

        instance.captureHandledException(SampleException(), DiagnosticContext())
    }

    @Test
    fun unsafeBreadcrumbMessageIsDropped() {
        val adapter = FakeSentrySdkAdapter()
        val instance = diagnostics(adapter = adapter)
        instance.setCollectionEnabled(true)

        instance.addBreadcrumb(DiagnosticBreadcrumb("contact parent@example.com"))

        assertTrue(adapter.breadcrumbCalls.isEmpty())
    }

    @Test
    fun safeBreadcrumbMessageReachesTheAdapter() {
        val adapter = FakeSentrySdkAdapter()
        val instance = diagnostics(adapter = adapter)
        instance.setCollectionEnabled(true)

        instance.addBreadcrumb(DiagnosticBreadcrumb("catalogue load started", category = "content"))

        val call = adapter.breadcrumbCalls.single()
        assertEquals("catalogue load started", call.message)
        assertEquals("content", call.category)
    }
}

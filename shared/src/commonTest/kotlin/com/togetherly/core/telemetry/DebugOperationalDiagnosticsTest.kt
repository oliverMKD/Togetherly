package com.togetherly.core.telemetry

import com.togetherly.core.logging.FakeAppLogger
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugOperationalDiagnosticsTest {

    private fun diagnostics(logger: FakeAppLogger = FakeAppLogger()) = DebugOperationalDiagnostics(logger = logger)

    @Test
    fun noReportIsLoggedBeforeCollectionIsEnabled() {
        val logger = FakeAppLogger()
        val diagnostics = diagnostics(logger)

        diagnostics.captureHandledException(IllegalStateException("boom"), DiagnosticContext())
        diagnostics.addBreadcrumb(DiagnosticBreadcrumb("something happened"))

        assertTrue(logger.calls.isEmpty())
    }

    @Test
    fun handledExceptionIsLoggedAfterConsentIsGranted() {
        val logger = FakeAppLogger()
        val diagnostics = diagnostics(logger)
        diagnostics.setCollectionEnabled(true)

        diagnostics.captureHandledException(IllegalStateException("boom"), DiagnosticContext())

        assertTrue(logger.calls.any { it.message.contains("captureHandledException") })
    }

    @Test
    fun unsafeContextValueIsRedactedNeverLogged() {
        val logger = FakeAppLogger()
        val diagnostics = diagnostics(logger)
        diagnostics.setCollectionEnabled(true)

        diagnostics.captureHandledException(
            IllegalStateException("boom"),
            DiagnosticContext(tags = mapOf("path" to "/data/user/0/com.togetherly.app/files/media/photo.jpg")),
        )

        val logged = logger.calls.single { it.message.contains("captureHandledException") }.message
        assertFalse(logged.contains("/data/user"))
        assertTrue(logged.contains("<redacted>"))
    }

    @Test
    fun unsafeBreadcrumbMessageIsRedactedNeverLogged() {
        val logger = FakeAppLogger()
        val diagnostics = diagnostics(logger)
        diagnostics.setCollectionEnabled(true)

        diagnostics.addBreadcrumb(DiagnosticBreadcrumb("contact parent@example.com for details"))

        val logged = logger.calls.single { it.message.contains("addBreadcrumb") }.message
        assertFalse(logged.contains("parent@example.com"))
        assertTrue(logged.contains("<redacted>"))
    }
}

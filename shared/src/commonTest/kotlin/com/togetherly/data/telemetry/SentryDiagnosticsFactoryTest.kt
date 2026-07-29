package com.togetherly.data.telemetry

import com.togetherly.core.logging.FakeAppLogger
import com.togetherly.core.telemetry.DiagnosticContext
import com.togetherly.core.telemetry.NoOpOperationalDiagnostics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FactoryTestException : Exception()

class SentryDiagnosticsFactoryTest {

    @Test
    fun blankDsnFallsBackToNoOpOnADebugBuild() {
        val logger = FakeAppLogger()

        val instance = createOperationalDiagnostics(
            dsn = "",
            release = { "togetherly@1.0+1" },
            environment = "debug",
            debug = true,
            logger = logger,
            adapter = { error("adapter must never be constructed when the DSN is missing") },
        )

        assertIs<NoOpOperationalDiagnostics>(instance)
        assertTrue(logger.calls.any { it.level == "warn" }, "A debug build should log a clear warning about the missing DSN")
    }

    @Test
    fun nullDsnFallsBackToNoOpSilentlyOnAReleaseBuild() {
        val logger = FakeAppLogger()

        val instance = createOperationalDiagnostics(
            dsn = null,
            release = { "togetherly@1.0+1" },
            environment = "release",
            debug = false,
            logger = logger,
            adapter = { error("adapter must never be constructed when the DSN is missing") },
        )

        assertIs<NoOpOperationalDiagnostics>(instance)
        assertFalse(logger.calls.any { it.level == "warn" }, "A release build must stay silent about a missing DSN")
    }

    @Test
    fun releaseIsNeverEvaluatedWhenTheDsnIsMissing() {
        var releaseWasEvaluated = false

        createOperationalDiagnostics(
            dsn = null,
            release = { releaseWasEvaluated = true; "togetherly@1.0+1" },
            environment = "debug",
            debug = true,
            logger = FakeAppLogger(),
            adapter = { error("must not be constructed") },
        )

        assertFalse(releaseWasEvaluated, "release() may resolve a platform VersionInfoProvider — it must never run when there is nothing to report to")
    }

    @Test
    fun aRealDsnConstructsAWorkingProvider() {
        val adapter = FakeSentrySdkAdapter()

        val instance = createOperationalDiagnostics(
            dsn = "https://example@o1.ingest.sentry.io/1",
            release = { "togetherly@1.0+1" },
            environment = "debug",
            debug = true,
            logger = FakeAppLogger(),
            adapter = { adapter },
        )

        assertEquals(1, adapter.setupCalls.size)
        instance.setCollectionEnabled(true)
        instance.captureHandledException(FactoryTestException(), DiagnosticContext())
        assertEquals(1, adapter.captureCalls.size)
    }
}

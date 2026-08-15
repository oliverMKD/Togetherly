package com.togetherly.app.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KoinConfigurationTest {

    @Test
    fun telemetryStartupIsDeferredOffTheCallingThread() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var telemetryStarted = false
        var linkerStarted = false

        val job = launchTelemetryStartup(
            scope = CoroutineScope(SupervisorJob() + dispatcher),
            startTelemetry = { telemetryStarted = true },
            startRevenueCatAnalyticsLinker = { linkerStarted = true },
            logFailure = { _, _ -> },
        )

        assertFalse(telemetryStarted)
        assertFalse(linkerStarted)
        testScheduler.runCurrent()
        assertTrue(telemetryStarted)
        assertTrue(linkerStarted)
        job.cancel()
    }

    @Test
    fun oneTelemetryFailureDoesNotPreventTheOtherIntegrationStarting() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val failures = mutableListOf<String>()
        var linkerStarted = false

        launchTelemetryStartup(
            scope = CoroutineScope(SupervisorJob() + dispatcher),
            startTelemetry = { error("SDK setup failed") },
            startRevenueCatAnalyticsLinker = { linkerStarted = true },
            logFailure = { message, _ -> failures += message },
        )

        testScheduler.runCurrent()
        assertTrue(linkerStarted)
        assertEquals(listOf("Telemetry coordinator failed to start"), failures)
    }
}

package com.togetherly.data.telemetry

import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.logging.FakeAppLogger
import com.togetherly.core.telemetry.AnalyticsEvent
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.DuplicateSignalDetector
import com.togetherly.core.telemetry.FakeProductAnalytics
import com.togetherly.core.telemetry.ProviderConfigurationStatus
import com.togetherly.core.telemetry.TelemetryDebugRecorder
import com.togetherly.core.telemetry.TestAnalyticsEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class DebugRecordingProductAnalyticsTest {

    private fun decorator(
        delegate: FakeProductAnalytics = FakeProductAnalytics(),
        recorder: TelemetryDebugRecorder = TelemetryDebugRecorder(TestAppClock(Instant.fromEpochSeconds(0))),
    ) = DebugRecordingProductAnalytics(
        delegate = delegate,
        recorder = recorder,
        duplicateDetector = DuplicateSignalDetector(TestAppClock(Instant.fromEpochSeconds(0)), FakeAppLogger()),
    )

    @Test
    fun everyCaptureIsForwardedToTheRealDelegateUnchanged() {
        val delegate = FakeProductAnalytics()
        val debug = decorator(delegate = delegate)
        debug.setCollectionEnabled(true)
        val event: AnalyticsEvent = TestAnalyticsEvent(name = "onboarding_completed")

        debug.capture(event)

        assertEquals(listOf(event), delegate.capturedEvents)
    }

    @Test
    fun noPreConsentEventIsEverRecordedEvenIfItWouldHavePassedValidation() {
        val recorder = TelemetryDebugRecorder(TestAppClock(Instant.fromEpochSeconds(0)))
        val debug = decorator(recorder = recorder)

        debug.capture(TestAnalyticsEvent(name = "onboarding_completed"))

        assertTrue(recorder.recentEvents().isEmpty(), "Nothing may be retained — even locally — before setCollectionEnabled(true)")
    }

    @Test
    fun anAcceptedEventIsRecordedWithItsSanitizedPropertiesOnceCollectionIsEnabled() {
        val recorder = TelemetryDebugRecorder(TestAppClock(Instant.fromEpochSeconds(0)))
        val debug = decorator(recorder = recorder)
        debug.setCollectionEnabled(true)

        debug.capture(TestAnalyticsEvent(name = "onboarding_completed"))

        val recorded = recorder.recentEvents().single()
        assertEquals("onboarding_completed", recorded.name)
    }

    @Test
    fun aRejectedEventIsNeverRecordedButIsStillForwarded() {
        val recorder = TelemetryDebugRecorder(TestAppClock(Instant.fromEpochSeconds(0)))
        val delegate = FakeProductAnalytics()
        val debug = decorator(delegate = delegate, recorder = recorder)
        debug.setCollectionEnabled(true)
        val unregisteredEvent: AnalyticsEvent = TestAnalyticsEvent(name = "not_a_registered_event")

        debug.capture(unregisteredEvent)

        assertTrue(recorder.recentEvents().isEmpty())
        assertEquals(listOf(unregisteredEvent), delegate.capturedEvents)
    }

    @Test
    fun revokingCollectionAfterGrantingStopsFurtherRecordingButKeepsWhatWasAlreadyThere() {
        val recorder = TelemetryDebugRecorder(TestAppClock(Instant.fromEpochSeconds(0)))
        val debug = decorator(recorder = recorder)
        debug.setCollectionEnabled(true)
        debug.capture(TestAnalyticsEvent(name = "onboarding_completed"))

        debug.setCollectionEnabled(false)
        debug.capture(TestAnalyticsEvent(name = "memory_deleted"))

        assertEquals(1, recorder.recentEvents().size)
        assertEquals("onboarding_completed", recorder.recentEvents().single().name)
    }

    @Test
    fun screenFlushSetCollectionEnabledAndResetAllDelegateDirectly() {
        val delegate = FakeProductAnalytics()
        val debug = decorator(delegate = delegate)

        debug.screen(AnalyticsScreen.TODAY)
        debug.setCollectionEnabled(true)
        debug.flush()
        debug.reset()

        assertEquals(1, delegate.flushCallCount)
        assertEquals(1, delegate.resetCallCount)
        assertTrue(delegate.collectionEnabled)
    }

    @Test
    fun configurationStatusDelegatesToTheRealProvider() {
        val delegate = FakeProductAnalytics()
        delegate.configurationStatusValue = ProviderConfigurationStatus.INITIALIZATION_FAILED
        val debug = decorator(delegate = delegate)

        assertEquals(ProviderConfigurationStatus.INITIALIZATION_FAILED, debug.configurationStatus())
    }
}

package com.togetherly.core.telemetry

import com.togetherly.core.datetime.TestAppClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class TelemetryDebugRecorderTest {

    @Test
    fun recordsAnEventAndReturnsItInRecentEvents() {
        val recorder = TelemetryDebugRecorder(TestAppClock(Instant.fromEpochSeconds(0)))

        recorder.recordEvent("quest_completed", mapOf("quest_category" to AnalyticsValue.Text("outdoor")))

        val recorded = recorder.recentEvents().single()
        assertEquals("quest_completed", recorded.name)
        assertEquals(AnalyticsValue.Text("outdoor"), recorded.properties["quest_category"])
    }

    @Test
    fun recordsABreadcrumb() {
        val recorder = TelemetryDebugRecorder(TestAppClock(Instant.fromEpochSeconds(0)))

        recorder.recordBreadcrumb("catalogue load started", "content")

        val recorded = recorder.recentBreadcrumbs().single()
        assertEquals("catalogue load started", recorded.message)
        assertEquals("content", recorded.category)
    }

    @Test
    fun recordsAProviderErrorCategoryOnly() {
        val recorder = TelemetryDebugRecorder(TestAppClock(Instant.fromEpochSeconds(0)))

        recorder.recordProviderError("IllegalStateException")

        assertEquals("IllegalStateException", recorder.recentProviderErrors().single().category)
    }

    @Test
    fun eventHistoryIsBoundedAndDropsTheOldestEntry() {
        val recorder = TelemetryDebugRecorder(TestAppClock(Instant.fromEpochSeconds(0)))

        repeat(25) { index -> recorder.recordEvent("event_$index", emptyMap()) }

        val recent = recorder.recentEvents()
        assertEquals(20, recent.size)
        assertEquals("event_5", recent.first().name)
        assertEquals("event_24", recent.last().name)
    }

    @Test
    fun clearRemovesEverything() {
        val recorder = TelemetryDebugRecorder(TestAppClock(Instant.fromEpochSeconds(0)))
        recorder.recordEvent("quest_completed", emptyMap())
        recorder.recordBreadcrumb("message", null)
        recorder.recordProviderError("SomeException")

        recorder.clear()

        assertTrue(recorder.recentEvents().isEmpty())
        assertTrue(recorder.recentBreadcrumbs().isEmpty())
        assertTrue(recorder.recentProviderErrors().isEmpty())
    }
}

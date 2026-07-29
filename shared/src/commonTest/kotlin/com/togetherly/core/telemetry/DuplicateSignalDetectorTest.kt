package com.togetherly.core.telemetry

import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.logging.FakeAppLogger
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class DuplicateSignalDetectorTest {

    @Test
    fun firstOccurrenceOfAnOperationIdNeverWarns() {
        val logger = FakeAppLogger()
        val detector = DuplicateSignalDetector(TestAppClock(Instant.fromEpochMilliseconds(0)), logger)

        detector.check("quest_completed")

        assertTrue(logger.calls.isEmpty())
    }

    @Test
    fun theSameOperationIdWithinTheThresholdWarns() {
        val clock = TestAppClock(Instant.fromEpochMilliseconds(0))
        val logger = FakeAppLogger()
        val detector = DuplicateSignalDetector(clock, logger, threshold = 500.milliseconds)

        detector.check("quest_completed")
        clock.advanceTo(Instant.fromEpochMilliseconds(100))
        detector.check("quest_completed")

        assertTrue(logger.calls.any { it.level == "warn" && it.message.contains("quest_completed") })
    }

    @Test
    fun theSameOperationIdAfterTheThresholdNeverWarns() {
        val clock = TestAppClock(Instant.fromEpochMilliseconds(0))
        val logger = FakeAppLogger()
        val detector = DuplicateSignalDetector(clock, logger, threshold = 500.milliseconds)

        detector.check("quest_completed")
        clock.advanceTo(Instant.fromEpochMilliseconds(600))
        detector.check("quest_completed")

        assertFalse(logger.calls.any { it.level == "warn" })
    }

    @Test
    fun differentOperationIdsNeverWarnAgainstEachOther() {
        val clock = TestAppClock(Instant.fromEpochMilliseconds(0))
        val logger = FakeAppLogger()
        val detector = DuplicateSignalDetector(clock, logger, threshold = 500.milliseconds)

        detector.check("quest_completed")
        clock.advanceTo(Instant.fromEpochMilliseconds(50))
        detector.check("quest_started")

        assertTrue(logger.calls.isEmpty())
    }

    @Test
    fun checkNeverThrowsRegardlessOfWhatItLogs() {
        val detector = DuplicateSignalDetector(TestAppClock(Instant.fromEpochMilliseconds(0)), FakeAppLogger())

        detector.check("quest_completed")
        detector.check("quest_completed")
    }
}

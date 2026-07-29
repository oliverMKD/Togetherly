package com.togetherly.core.notification

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import com.togetherly.domain.family.ReminderPreference
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * A real-device/emulator smoke test — same shape as `AndroidQuestFeedbackControllerTest`: proves
 * the real `AlarmManager` calls never throw and complete successfully, not that a notification
 * actually fires (which needs real wall-clock waiting/manual verification — see
 * docs/reminders.md's manual test procedure). Scheduling twice for the same day is exercised
 * deliberately (see [reSchedulingTheSameDayNeverThrows]) since that's exactly the "avoid duplicate
 * notifications" path — the same `PendingIntent` request code replaces the prior alarm rather than
 * adding a second one.
 */
@RunWith(AndroidJUnit4::class)
internal class AndroidReminderSchedulerTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val clock = TestAppClock(Instant.parse("2026-06-15T08:00:00Z"))
    private val timeZoneProvider = object : com.togetherly.core.datetime.AppTimeZoneProvider {
        override fun current(): TimeZone = TimeZone.UTC
    }
    private val scheduler = AndroidReminderScheduler(context, clock, timeZoneProvider, FakeOperationalDiagnostics())

    @Test
    fun schedulingNeverThrowsAndSucceeds() = runTest {
        val result = scheduler.schedule(ReminderPreference(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), LocalTime(18, 0)))

        assertTrue(result is DataResult.Success)
    }

    @Test
    fun reSchedulingTheSameDayNeverThrows() = runTest {
        val preference = ReminderPreference(setOf(DayOfWeek.MONDAY), LocalTime(18, 0))

        scheduler.schedule(preference)
        val result = scheduler.schedule(preference)

        assertTrue(result is DataResult.Success)
    }

    @Test
    fun cancelingWithNothingScheduledNeverThrows() = runTest {
        val result = scheduler.cancel()

        assertTrue(result is DataResult.Success)
    }

    @Test
    fun cancelingAfterSchedulingNeverThrows() = runTest {
        scheduler.schedule(ReminderPreference(setOf(DayOfWeek.MONDAY), LocalTime(18, 0)))

        val result = scheduler.cancel()

        assertTrue(result is DataResult.Success)
    }
}

package com.togetherly.core.notification

import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import com.togetherly.domain.family.ReminderPreference
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * A real-simulator smoke test — same shape as `IosVoiceRecorderTest`: proves the real
 * `UNUserNotificationCenter` calls never throw and complete successfully, not that a notification
 * actually fires (which needs a granted permission and real wall-clock waiting/manual
 * verification — see docs/reminders.md's manual test procedure). The simulator has no user present
 * to grant notification permission interactively, so `addNotificationRequest` itself still
 * succeeds (scheduling doesn't require authorization — only *delivery* does), which is exactly
 * what this test can honestly verify in this environment.
 */
class IosReminderSchedulerTest {

    private val scheduler = IosReminderScheduler(FakeOperationalDiagnostics())

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
}

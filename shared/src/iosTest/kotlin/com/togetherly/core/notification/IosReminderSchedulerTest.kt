package com.togetherly.core.notification

import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import com.togetherly.domain.family.ReminderPreference
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosReminderSchedulerTest {

    private fun scheduler(adapter: FakeIosNotificationCenterAdapter) =
        IosReminderScheduler(adapter, FakeOperationalDiagnostics())

    @Test
    fun scheduleSucceeds() = runTest {
        val adapter = FakeIosNotificationCenterAdapter()
        val result = scheduler(adapter).schedule(ReminderPreference(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), LocalTime(18, 0)))

        assertTrue(result is DataResult.Success)
        assertEquals(setOf("togetherly.reminder.MONDAY", "togetherly.reminder.FRIDAY"), adapter.pendingRequestIdentifiers().toSet())
        assertEquals(2, adapter.addCalls)
    }

    @Test
    fun permissionDeniedIsReportedThroughTheAdapter() = runTest {
        val adapter = FakeIosNotificationCenterAdapter(authorizationStatusValue = NotificationPermissionState.PermanentlyDenied)
        val status = adapter.authorizationStatus()

        assertEquals(NotificationPermissionState.PermanentlyDenied, status)
        assertEquals(1, adapter.authorizationStatusCalls)
    }

    @Test
    fun addRequestFailureReturnsUnexpectedError() = runTest {
        val adapter = FakeIosNotificationCenterAdapter(addFailurePredicate = { it.identifier == "togetherly.reminder.MONDAY" })
        val result = scheduler(adapter).schedule(ReminderPreference(setOf(DayOfWeek.MONDAY), LocalTime(18, 0)))

        assertTrue(result is DataResult.Error)
        assertTrue(adapter.pendingRequestIdentifiers().isEmpty())
    }

    @Test
    fun existingReminderReplacementKeepsOnePendingRequestPerDay() = runTest {
        val adapter = FakeIosNotificationCenterAdapter()
        val scheduler = scheduler(adapter)

        scheduler.schedule(ReminderPreference(setOf(DayOfWeek.MONDAY), LocalTime(18, 0)))
        scheduler.schedule(ReminderPreference(setOf(DayOfWeek.MONDAY), LocalTime(19, 30)))

        val pending = adapter.pendingRequestIdentifiers()
        assertEquals(listOf("togetherly.reminder.MONDAY"), pending)
        assertEquals(19, adapter.addedRequests.last().hour)
        assertEquals(30, adapter.addedRequests.last().minute)
    }

    @Test
    fun narrowingTheEnabledDaysRemovesTheNoLongerSelectedDaysPendingRequest() = runTest {
        val adapter = FakeIosNotificationCenterAdapter()
        val scheduler = scheduler(adapter)

        scheduler.schedule(ReminderPreference(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), LocalTime(18, 0)))
        scheduler.schedule(ReminderPreference(setOf(DayOfWeek.MONDAY), LocalTime(18, 0)))

        assertEquals(listOf("togetherly.reminder.MONDAY"), adapter.pendingRequestIdentifiers())
    }

    @Test
    fun cancelRemovesAllTogetherlyReminderRequests() = runTest {
        val adapter = FakeIosNotificationCenterAdapter()
        val scheduler = scheduler(adapter)

        scheduler.schedule(ReminderPreference(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), LocalTime(8, 15)))
        // schedule() itself now also calls removePendingRequests once (clearing stale days before
        // re-adding the enabled ones) — isolate cancel()'s own contribution rather than asserting a
        // hardcoded total, so this stays correct regardless of how schedule() implements its cleanup.
        val removeCallsBeforeCancel = adapter.removeCalls
        val result = scheduler.cancel()

        assertTrue(result is DataResult.Success)
        assertTrue(adapter.pendingRequestIdentifiers().isEmpty())
        assertEquals(removeCallsBeforeCancel + 1, adapter.removeCalls)
    }

    @Test
    fun selectedWeekdaysMapToStableIdentifiersAndCalendarWeekdays() = runTest {
        val adapter = FakeIosNotificationCenterAdapter()
        val result = scheduler(adapter).schedule(ReminderPreference(setOf(DayOfWeek.SUNDAY, DayOfWeek.TUESDAY), LocalTime(6, 45)))

        assertTrue(result is DataResult.Success)
        assertEquals(
            setOf("togetherly.reminder.SUNDAY", "togetherly.reminder.TUESDAY"),
            adapter.pendingRequestIdentifiers().toSet(),
        )
        val sunday = adapter.addedRequests.single { it.identifier.endsWith("SUNDAY") }
        val tuesday = adapter.addedRequests.single { it.identifier.endsWith("TUESDAY") }
        assertEquals(1, sunday.weekday)
        assertEquals(3, tuesday.weekday)
        assertEquals(6, sunday.hour)
        assertEquals(45, sunday.minute)
    }

    @Test
    fun schedulingTwiceDoesNotCreateDuplicateRequests() = runTest {
        val adapter = FakeIosNotificationCenterAdapter()
        val scheduler = scheduler(adapter)
        val preference = ReminderPreference(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), LocalTime(18, 0))

        scheduler.schedule(preference)
        scheduler.schedule(preference)

        assertEquals(2, adapter.pendingRequestIdentifiers().size)
        assertEquals(setOf("togetherly.reminder.MONDAY", "togetherly.reminder.FRIDAY"), adapter.pendingRequestIdentifiers().toSet())
    }
}

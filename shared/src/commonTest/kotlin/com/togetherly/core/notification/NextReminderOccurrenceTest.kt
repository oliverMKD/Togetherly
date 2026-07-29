package com.togetherly.core.notification

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val UTC = TimeZone.UTC

class NextReminderOccurrenceTest {

    @Test
    fun laterTheSameDayReturnsThatSameDay() {
        // 2026-06-15 is a Monday, 08:00 UTC.
        val now = Instant.parse("2026-06-15T08:00:00Z")

        val next = nextReminderOccurrence(DayOfWeek.MONDAY, LocalTime(18, 0), now, UTC)

        assertEquals(Instant.parse("2026-06-15T18:00:00Z"), next)
    }

    @Test
    fun earlierTheSameDayRollsForwardToNextWeek() {
        val now = Instant.parse("2026-06-15T08:00:00Z")

        val next = nextReminderOccurrence(DayOfWeek.MONDAY, LocalTime(7, 0), now, UTC)

        assertEquals(Instant.parse("2026-06-22T07:00:00Z"), next)
    }

    @Test
    fun exactlyNowRollsForwardRatherThanFiringImmediately() {
        val now = Instant.parse("2026-06-15T18:00:00Z")

        val next = nextReminderOccurrence(DayOfWeek.MONDAY, LocalTime(18, 0), now, UTC)

        assertEquals(Instant.parse("2026-06-22T18:00:00Z"), next)
    }

    @Test
    fun aLaterDayInTheSameWeekIsFound() {
        val now = Instant.parse("2026-06-15T08:00:00Z") // Monday

        val next = nextReminderOccurrence(DayOfWeek.FRIDAY, LocalTime(9, 0), now, UTC)

        assertEquals(Instant.parse("2026-06-19T09:00:00Z"), next)
    }

    @Test
    fun anEarlierDayInTheWeekWrapsAroundToNextWeek() {
        val now = Instant.parse("2026-06-19T08:00:00Z") // Friday

        val next = nextReminderOccurrence(DayOfWeek.MONDAY, LocalTime(9, 0), now, UTC)

        assertEquals(Instant.parse("2026-06-22T09:00:00Z"), next)
    }
}

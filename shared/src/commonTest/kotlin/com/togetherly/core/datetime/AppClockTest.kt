package com.togetherly.core.datetime

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private class FixedAppClock(private val instant: Instant) : AppClock {
    override fun now(): Instant = instant

    override fun today(timeZone: TimeZone): LocalDate =
        instant.toLocalDateTime(timeZone).date
}

class AppClockTest {

    @Test
    fun nowReturnsConfiguredInstant() {
        val configured = Instant.parse("2026-07-23T10:00:00Z")
        val clock = FixedAppClock(configured)

        assertEquals(configured, clock.now())
    }

    @Test
    fun todayConvertsInstantUsingProvidedTimeZone() {
        val instant = Instant.parse("2026-07-23T10:00:00Z")
        val clock = FixedAppClock(instant)

        assertEquals(LocalDate(2026, 7, 23), clock.today(TimeZone.of("UTC+9")))
    }

    @Test
    fun dateConversionHandlesMidnightBoundaryPerTimeZone() {
        // 23:30 UTC is still 2026-07-23 in UTC, but already 2026-07-24 nine hours east of it.
        val instant = Instant.parse("2026-07-23T23:30:00Z")
        val clock = FixedAppClock(instant)

        assertEquals(LocalDate(2026, 7, 23), clock.today(TimeZone.UTC))
        assertEquals(LocalDate(2026, 7, 24), clock.today(TimeZone.of("UTC+9")))
    }
}

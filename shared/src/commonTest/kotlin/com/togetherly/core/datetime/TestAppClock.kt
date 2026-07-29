package com.togetherly.core.datetime

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class TestAppClock(
    private var instant: Instant,
) : AppClock {
    override fun now(): Instant = instant

    override fun today(timeZone: TimeZone): LocalDate = instant.toLocalDateTime(timeZone).date

    fun advanceTo(newInstant: Instant) {
        instant = newInstant
    }
}

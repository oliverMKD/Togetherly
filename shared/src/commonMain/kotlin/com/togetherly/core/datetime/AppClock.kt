package com.togetherly.core.datetime

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

interface AppClock {
    fun now(): Instant

    fun today(
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): LocalDate
}

internal class DefaultAppClock : AppClock {
    override fun now(): Instant = Clock.System.now()

    override fun today(timeZone: TimeZone): LocalDate =
        now().toLocalDateTime(timeZone).date
}

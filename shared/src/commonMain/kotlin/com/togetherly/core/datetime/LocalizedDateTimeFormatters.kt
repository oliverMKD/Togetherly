package com.togetherly.core.datetime

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

expect fun LocalDateTime.localizedDateDisplay(): String
expect fun LocalDateTime.localizedTimeDisplay(): String
expect fun LocalDateTime.localizedDateTimeDisplay(): String
expect fun LocalTime.localizedTimeDisplay(): String

fun Instant.localizedDateDisplay(timeZone: TimeZone): String = toLocalDateTime(timeZone).localizedDateDisplay()

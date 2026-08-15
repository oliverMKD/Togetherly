package com.togetherly.core.datetime

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter

private fun dateFormatter(template: String): NSDateFormatter = NSDateFormatter().apply {
    setLocalizedDateFormatFromTemplate(template)
}

private fun LocalDateTime.toNSDateComponents(): NSDateComponents {
    val dateYear = year
    val dateMonth = monthNumber
    val dateDay = day
    val dateHour = hour
    val dateMinute = minute
    val dateSecond = second
    return NSDateComponents().apply {
        year = dateYear.toLong()
        month = dateMonth.toLong()
        day = dateDay.toLong()
        hour = dateHour.toLong()
        minute = dateMinute.toLong()
        second = dateSecond.toLong()
    }
}

private fun LocalTime.toNSDateComponents(): NSDateComponents {
    val timeHour = hour
    val timeMinute = minute
    val timeSecond = second
    return NSDateComponents().apply {
        hour = timeHour.toLong()
        minute = timeMinute.toLong()
        second = timeSecond.toLong()
    }
}

private fun formatDateTime(components: NSDateComponents, template: String): String {
    val calendar = NSCalendar.currentCalendar
    val date = calendar.dateFromComponents(components) ?: return ""
    return dateFormatter(template).stringFromDate(date)
}

actual fun LocalDateTime.localizedDateDisplay(): String =
    formatDateTime(toNSDateComponents(), "yMMMd")

actual fun LocalDateTime.localizedTimeDisplay(): String =
    formatDateTime(toNSDateComponents(), "jmm")

actual fun LocalDateTime.localizedDateTimeDisplay(): String =
    formatDateTime(toNSDateComponents(), "yMMMdjmm")

actual fun LocalTime.localizedTimeDisplay(): String =
    formatDateTime(toNSDateComponents(), "jmm")

package com.togetherly.feature.journey.mapper

import kotlinx.datetime.LocalDateTime

/**
 * Plain manual formatting — no platform-specific date/time API, matching
 * [com.togetherly.feature.completion.mapper.toCelebrationDisplay]'s own convention (English month
 * names only for now; full localization is future work). Split into separate date/time strings,
 * not one combined string, since [com.togetherly.feature.journey.model.JourneyEntryUi] renders
 * them as two separate pieces of text.
 */
private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

internal fun LocalDateTime.toJourneyDateDisplay(): String = "${MONTH_NAMES[month.ordinal]} $day, $year"

internal fun LocalDateTime.toJourneyTimeDisplay(): String {
    val hour12 = when (hour % 12) {
        0 -> 12
        else -> hour % 12
    }
    val period = if (hour < 12) "AM" else "PM"
    val minutePadded = minute.toString().padStart(2, '0')
    return "$hour12:$minutePadded $period"
}

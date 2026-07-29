package com.togetherly.feature.completion.mapper

import kotlinx.datetime.LocalDateTime

/**
 * Plain manual formatting — no platform-specific date/time API (matches
 * [com.togetherly.feature.questmode.mapper.toTimerDisplay]'s own convention). English month names
 * only for now; full localization is future work, same as the rest of this MVP's copy.
 */
private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

internal fun LocalDateTime.toCelebrationDisplay(): String {
    val monthName = MONTH_NAMES[month.ordinal]
    val hour12 = when (hour % 12) {
        0 -> 12
        else -> hour % 12
    }
    val period = if (hour < 12) "AM" else "PM"
    val minutePadded = minute.toString().padStart(2, '0')
    return "$monthName $day, $year at $hour12:$minutePadded $period"
}

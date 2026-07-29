package com.togetherly.domain.family

import com.togetherly.domain.validation.requireNotEmpty
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

data class ReminderPreference(
    val enabledDays: Set<DayOfWeek>,
    val localTime: LocalTime,
) {
    init {
        requireNotEmpty(enabledDays)
    }
}

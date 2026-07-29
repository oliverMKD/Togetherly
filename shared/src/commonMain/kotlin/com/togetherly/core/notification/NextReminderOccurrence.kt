package com.togetherly.core.notification

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * The next moment [time] falls on [day], strictly after [now], in [timeZone]. Pure and
 * deterministic given its inputs — no platform time API, testable with
 * [com.togetherly.core.datetime.TestAppClock] the same way every other date/time computation in
 * this project already is. Used only by [com.togetherly.core.notification.ReminderScheduler]'s
 * Android implementation, which needs a concrete trigger instant for `AlarmManager`; iOS's
 * `UNCalendarNotificationTrigger` computes its own next occurrence natively from day-of-week/time
 * date components, so this function has no iOS caller.
 *
 * "Strictly after" (never "on or after") means a reminder whose time is exactly *now* is scheduled
 * for next week, not fired immediately — a gentle daily reminder should never surprise-fire the
 * instant it's turned on.
 */
fun nextReminderOccurrence(day: DayOfWeek, time: LocalTime, now: Instant, timeZone: TimeZone): Instant {
    var date = now.toLocalDateTime(timeZone).date
    while (date.dayOfWeek != day) {
        date = date.plus(1, DateTimeUnit.DAY)
    }
    var candidate = LocalDateTime(date, time).toInstant(timeZone)
    if (candidate <= now) {
        candidate = candidate.plus(7, DateTimeUnit.DAY, timeZone)
    }
    return candidate
}

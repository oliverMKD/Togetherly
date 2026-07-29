package com.togetherly.core.notification

import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.DiagnosticContext
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.domain.family.ReminderPreference
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.getString
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.reminder_notification_body
import togetherly.shared.generated.resources.reminder_notification_title
import kotlin.coroutines.resume

/**
 * One [UNCalendarNotificationTrigger] per selected [DayOfWeek], each `repeats = true` — iOS
 * computes its own next weekly occurrence natively from the `weekday`/`hour`/`minute` date
 * components, so (unlike [AndroidReminderScheduler][com.togetherly.core.notification.AndroidReminderScheduler])
 * no [nextReminderOccurrence] math is needed here at all. Each day's request uses a stable
 * identifier ([identifierFor]) — `addNotificationRequest` with an existing identifier replaces the
 * prior pending request for it, so re-scheduling (an edit, [ReminderScheduler.refresh]) can never
 * create duplicates.
 */
internal class IosReminderScheduler(
    private val diagnostics: OperationalDiagnostics,
) : ReminderScheduler {

    override suspend fun schedule(preference: ReminderPreference): DataResult<Unit> = runCatching {
        cancelAllRequests()
        val title = getString(Res.string.reminder_notification_title)
        val body = getString(Res.string.reminder_notification_body)
        DayOfWeek.entries.filter { it in preference.enabledDays }.forEach { day ->
            addRequest(day, preference, title, body)
        }
    }.toReminderResult(diagnostics)

    override suspend fun cancel(): DataResult<Unit> = runCatching {
        cancelAllRequests()
    }.toReminderResult(diagnostics)

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun addRequest(day: DayOfWeek, preference: ReminderPreference, title: String, body: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
        }
        val components = NSDateComponents().apply {
            weekday = nsWeekdayFor(day).toLong()
            hour = preference.localTime.hour.toLong()
            minute = preference.localTime.minute.toLong()
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = true)
        val request = UNNotificationRequest.requestWithIdentifier(identifierFor(day), content, trigger)

        suspendCancellableCoroutine<Unit> { continuation ->
            UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request) { _ ->
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }

    private fun cancelAllRequests() {
        val identifiers = DayOfWeek.entries.map { identifierFor(it) }
        UNUserNotificationCenter.currentNotificationCenter().removePendingNotificationRequestsWithIdentifiers(identifiers)
    }

    private fun identifierFor(day: DayOfWeek): String = "togetherly.reminder.${day.name}"

    /**
     * `NSDateComponents.weekday` is 1=Sunday..7=Saturday; [DayOfWeek] declares `MONDAY` first
     * (`ordinal` 0..6 = Monday..Sunday, ISO order) — so `ordinal + 1` is Monday=2..Sunday=7, and
     * Sunday needs its own case since ISO Sunday (7) maps to NSCalendar Sunday (1), not 8.
     */
    private fun nsWeekdayFor(day: DayOfWeek): Int = if (day == DayOfWeek.SUNDAY) 1 else day.ordinal + 2
}

private fun Result<Unit>.toReminderResult(diagnostics: OperationalDiagnostics): DataResult<Unit> = fold(
    onSuccess = { DataResult.Success(Unit) },
    onFailure = { throwable ->
        diagnostics.captureHandledException(throwable, DiagnosticContext(mapOf("feature" to "reminder", "operation" to "schedule")))
        DataResult.Error(AppError.Unexpected(throwable))
    },
)

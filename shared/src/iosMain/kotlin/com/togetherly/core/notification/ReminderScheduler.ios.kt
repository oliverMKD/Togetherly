package com.togetherly.core.notification

import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.DiagnosticContext
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.domain.family.ReminderPreference
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.getString
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.reminder_notification_body
import togetherly.shared.generated.resources.reminder_notification_title

/**
 * One [IosReminderRequest] per selected [DayOfWeek], each `repeats = true` — iOS computes its own
 * next weekly occurrence natively from the `weekday`/`hour`/`minute` date components, so (unlike
 * [AndroidReminderScheduler][com.togetherly.core.notification.AndroidReminderScheduler]) no
 * [nextReminderOccurrence] math is needed here at all. Each day's request uses a stable identifier
 * ([reminderNotificationIdentifier]), but [schedule] still cancels every possible day's request
 * first — matching [AndroidReminderScheduler]'s own documented guarantee — since only re-adding
 * the *currently* enabled days would leave a day's request behind forever once that day is
 * disabled (its identifier would never be touched again by `add`/`removePendingRequests`).
 */
internal class IosReminderScheduler(
    private val notifications: IosNotificationCenterAdapter,
    private val diagnostics: OperationalDiagnostics,
) : ReminderScheduler {

    override suspend fun schedule(preference: ReminderPreference): DataResult<Unit> = runCatching {
        notifications.removePendingRequests(DayOfWeek.entries.map(::reminderNotificationIdentifier))
        val title = getString(Res.string.reminder_notification_title)
        val body = getString(Res.string.reminder_notification_body)
        DayOfWeek.entries.filter { it in preference.enabledDays }.forEach { day ->
            notifications.add(reminderNotificationRequest(day, preference, title, body))
        }
    }.toReminderResult(diagnostics)

    override suspend fun cancel(): DataResult<Unit> = runCatching {
        notifications.removePendingRequests(DayOfWeek.entries.map(::reminderNotificationIdentifier))
    }.toReminderResult(diagnostics)
}

private fun Result<Unit>.toReminderResult(diagnostics: OperationalDiagnostics): DataResult<Unit> = fold(
    onSuccess = { DataResult.Success(Unit) },
    onFailure = { throwable ->
        diagnostics.captureHandledException(throwable, DiagnosticContext(mapOf("feature" to "reminder", "operation" to "schedule")))
        DataResult.Error(AppError.Unexpected(throwable))
    },
)

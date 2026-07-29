package com.togetherly.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.togetherly.core.datetime.AppClock
import com.togetherly.core.datetime.AppTimeZoneProvider
import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.DiagnosticContext
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.domain.family.ReminderPreference
import kotlinx.datetime.DayOfWeek

/**
 * One [AlarmManager.setInexactRepeating] alarm per selected [DayOfWeek] — `INTERVAL_DAY * 7` gives
 * a native weekly repeat with no self-rescheduling `BroadcastReceiver` loop needed. Inexact
 * (never `setExact*`/`setAlarmClock`) deliberately — a gentle daily reminder has no need for
 * second-level precision, and inexact scheduling needs no `SCHEDULE_EXACT_ALARM` permission at
 * all (see this class's own callers: [com.togetherly.app.di.PlatformModule.android.kt] never
 * requests it). [schedule] always cancels every possible day's alarm first, so editing preferences
 * (fewer days, a different time) never leaves a stale alarm from the previous configuration
 * behind — the per-day [PendingIntent] request code ([REQUEST_CODE_BASE] + [DayOfWeek.ordinal]) is
 * what makes each day's alarm independently addressable/cancelable.
 *
 * `AlarmManager` alarms are cleared on device reboot — see [ReminderBootReceiver] for the
 * corresponding reschedule.
 */
internal class AndroidReminderScheduler(
    private val context: Context,
    private val clock: AppClock,
    private val timeZoneProvider: AppTimeZoneProvider,
    private val diagnostics: OperationalDiagnostics,
) : ReminderScheduler {

    private val alarmManager: AlarmManager?
        get() = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    override suspend fun schedule(preference: ReminderPreference): DataResult<Unit> = runCatching {
        val manager = alarmManager ?: return@runCatching
        val now = clock.now()
        val timeZone = timeZoneProvider.current()
        cancelAll(manager)
        DayOfWeek.entries.filter { it in preference.enabledDays }.forEach { day ->
            val triggerAt = nextReminderOccurrence(day, preference.localTime, now, timeZone)
            manager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAt.toEpochMilliseconds(),
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntentFor(day),
            )
        }
    }.toReminderResult(diagnostics)

    override suspend fun cancel(): DataResult<Unit> = runCatching {
        val manager = alarmManager ?: return@runCatching
        cancelAll(manager)
    }.toReminderResult(diagnostics)

    private fun cancelAll(manager: AlarmManager) {
        DayOfWeek.entries.forEach { day -> manager.cancel(pendingIntentFor(day)) }
    }

    private fun pendingIntentFor(day: DayOfWeek): PendingIntent {
        val intent = Intent(context, ReminderNotificationReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + day.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val REQUEST_CODE_BASE = 4200
    }
}

private fun Result<Unit>.toReminderResult(diagnostics: OperationalDiagnostics): DataResult<Unit> = fold(
    onSuccess = { DataResult.Success(Unit) },
    onFailure = { throwable ->
        diagnostics.captureHandledException(throwable, DiagnosticContext(mapOf("feature" to "reminder", "operation" to "schedule")))
        DataResult.Error(AppError.Unexpected(throwable))
    },
)

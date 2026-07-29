package com.togetherly.core.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.togetherly.app.shared.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.reminder_notification_body
import togetherly.shared.generated.resources.reminder_notification_channel_name
import togetherly.shared.generated.resources.reminder_notification_title

internal const val REMINDER_NOTIFICATION_CHANNEL_ID = "togetherly_reminders"
private const val REMINDER_NOTIFICATION_ID = 4200

/**
 * Fires when one of [AndroidReminderScheduler]'s alarms goes off. Never carries any family memory
 * content in the notification itself (title/body are the same fixed, generic copy every time —
 * see this feature's own step spec: "Do not include sensitive family data in notification
 * content"). [goAsync] + a background-dispatcher coroutine is required here since reading the
 * notification copy uses the suspend `getString` (Compose Multiplatform resources) — a plain
 * `BroadcastReceiver.onReceive` is not itself a coroutine scope and must not block the main thread.
 */
internal class ReminderNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                showNotification(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun showNotification(context: Context) {
        val channelName = getString(Res.string.reminder_notification_channel_name)
        val title = getString(Res.string.reminder_notification_title)
        val body = getString(Res.string.reminder_notification_body)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        ensureChannel(manager, channelName)

        val notification = Notification.Builder(context, REMINDER_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_reminder_notification)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent(context))
            .build()

        val canPost = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (canPost) {
            manager.notify(REMINDER_NOTIFICATION_ID, notification)
        }
    }

    /** A calm, plain user-facing channel name — no description needed beyond that. */
    private fun ensureChannel(manager: NotificationManager, channelName: String) {
        val channel = NotificationChannel(REMINDER_NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(channel)
    }

    private fun openAppPendingIntent(context: Context): PendingIntent? {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(launchIntent)
            .getPendingIntent(REMINDER_NOTIFICATION_ID, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

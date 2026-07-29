package com.togetherly.core.notification

import androidx.compose.runtime.Composable

fun interface NotificationPermissionController {
    fun request()
}

/**
 * Requests notification permission through the platform's own system prompt — same shape as
 * [com.togetherly.core.media.MicrophonePermissionRequester], called only when the parent
 * explicitly enables reminders (see [com.togetherly.feature.reminder.presentation.ReminderViewModel]'s
 * own KDoc), never at app startup. [onResult] is how the caller finds out what happened.
 */
@Composable
expect fun rememberNotificationPermissionController(
    onResult: (NotificationPermissionState) -> Unit,
): NotificationPermissionController

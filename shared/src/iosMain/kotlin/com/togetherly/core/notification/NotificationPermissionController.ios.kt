package com.togetherly.core.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * iOS has no separate "already granted, don't prompt" branch to check first the way
 * [com.togetherly.core.media.MicrophonePermissionRequester]'s Android actual does —
 * `requestAuthorizationWithOptions` itself is safe to call unconditionally; the system only ever
 * shows the real prompt on the very first call and silently replays the stored answer on every
 * later one, so this can always just call it directly.
 */
@Composable
actual fun rememberNotificationPermissionController(
    onResult: (NotificationPermissionState) -> Unit,
): NotificationPermissionController {
    val scope = rememberCoroutineScope()
    return remember {
        NotificationPermissionController {
            scope.launch {
                val granted = requestAuthorization()
                onResult(if (granted) NotificationPermissionState.Granted else NotificationPermissionState.PermanentlyDenied)
            }
        }
    }
}

private suspend fun requestAuthorization(): Boolean = suspendCancellableCoroutine { continuation ->
    val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound
    UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(options) { granted, _ ->
        if (continuation.isActive) continuation.resume(granted)
    }
}

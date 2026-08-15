package com.togetherly.core.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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
    val notifications = koinInject<IosNotificationCenterAdapter>()
    return remember {
        NotificationPermissionController {
            scope.launch {
                val granted = notifications.requestAuthorization()
                onResult(if (granted) NotificationPermissionState.Granted else NotificationPermissionState.PermanentlyDenied)
            }
        }
    }
}

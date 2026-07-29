package com.togetherly.core.notification

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * [NotificationPermissionState.NotRequired] never appears here — iOS always has a real runtime
 * permission for notifications, unlike Android below API 33. A `denied` status is reported as
 * [NotificationPermissionState.PermanentlyDenied] rather than [NotificationPermissionState.Denied]
 * — iOS only ever shows its own system prompt once per install (same reasoning
 * [com.togetherly.core.media.MicrophonePermissionRequester]'s iOS actual already documents), so a
 * denial here always means Settings is the only remaining path.
 */
internal class IosNotificationPermissionStatusProvider : NotificationPermissionStatusProvider {

    override suspend fun currentStatus(): NotificationPermissionState = suspendCancellableCoroutine { continuation ->
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            val state = when (settings?.authorizationStatus) {
                UNAuthorizationStatusNotDetermined -> NotificationPermissionState.NotDetermined
                UNAuthorizationStatusAuthorized, UNAuthorizationStatusProvisional, UNAuthorizationStatusEphemeral -> NotificationPermissionState.Granted
                UNAuthorizationStatusDenied -> NotificationPermissionState.PermanentlyDenied
                else -> NotificationPermissionState.NotDetermined
            }
            if (continuation.isActive) continuation.resume(state)
        }
    }
}

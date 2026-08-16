package com.togetherly.core.notification

/**
 * [NotificationPermissionState.NotRequired] never appears here — iOS always has a real runtime
 * permission for notifications, unlike Android below API 33. A `denied` status is reported as
 * [NotificationPermissionState.PermanentlyDenied] rather than [NotificationPermissionState.Denied]
 * — iOS only ever shows its own system prompt once per install (same reasoning
 * [com.togetherly.core.media.MicrophonePermissionRequester]'s iOS actual already documents), so a
 * denial here always means Settings is the only remaining path.
 */
internal class IosNotificationPermissionStatusProvider(
    private val notifications: IosNotificationCenterAdapter,
) : NotificationPermissionStatusProvider {

    override suspend fun currentStatus(): NotificationPermissionState = notifications.authorizationStatus()
}

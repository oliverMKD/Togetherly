package com.togetherly.core.notification

class FakeNotificationPermissionStatusProvider(
    private var status: NotificationPermissionState = NotificationPermissionState.NotDetermined,
) : NotificationPermissionStatusProvider {

    fun setStatus(value: NotificationPermissionState) {
        status = value
    }

    override suspend fun currentStatus(): NotificationPermissionState = status
}

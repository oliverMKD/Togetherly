package com.togetherly.feature.reminder.model

/**
 * [RequestNotificationPermission]/[OpenSystemSettings] climb out to the Route because only
 * Compose-lifecycle-bound code ([com.togetherly.core.notification.rememberNotificationPermissionController],
 * [com.togetherly.core.media.rememberAppSettingsLauncher]) can actually launch either — the
 * ViewModel itself never touches a platform permission/settings API directly.
 */
sealed interface ReminderEvent {
    data object RequestNotificationPermission : ReminderEvent
    data object OpenSystemSettings : ReminderEvent
    data object SaveCompleted : ReminderEvent
    data object NavigatedBackWithoutSaving : ReminderEvent
}

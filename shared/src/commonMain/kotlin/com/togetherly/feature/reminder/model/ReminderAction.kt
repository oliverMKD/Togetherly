package com.togetherly.feature.reminder.model

import com.togetherly.core.notification.NotificationPermissionState
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

sealed interface ReminderAction {
    data class EnabledChanged(val enabled: Boolean) : ReminderAction
    data class DayToggled(val day: DayOfWeek) : ReminderAction
    data class TimeChanged(val time: LocalTime) : ReminderAction

    /** Dispatched by the Route once [com.togetherly.core.notification.rememberNotificationPermissionController]'s request completes. */
    data class PermissionResultReceived(val state: NotificationPermissionState) : ReminderAction
    data object OpenSettingsClicked : ReminderAction
    data object SaveClicked : ReminderAction
    data object BackClicked : ReminderAction
    data object DiscardConfirmed : ReminderAction
    data object DismissDiscardDialog : ReminderAction
}

package com.togetherly.core.notification

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.DayOfWeek
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

internal data class IosReminderRequest(
    val identifier: String,
    val weekday: Int,
    val hour: Int,
    val minute: Int,
    val title: String,
    val body: String,
)

internal interface IosNotificationCenterAdapter {
    suspend fun authorizationStatus(): NotificationPermissionState
    suspend fun requestAuthorization(): Boolean
    suspend fun add(request: IosReminderRequest)
    fun removePendingRequests(identifiers: List<String>)
    suspend fun pendingRequestIdentifiers(): List<String>
}

@OptIn(ExperimentalForeignApi::class)
internal class RealIosNotificationCenterAdapter : IosNotificationCenterAdapter {

    override suspend fun authorizationStatus(): NotificationPermissionState = suspendCancellableCoroutine { continuation ->
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

    override suspend fun requestAuthorization(): Boolean = suspendCancellableCoroutine { continuation ->
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(options) { granted, _ ->
            if (continuation.isActive) continuation.resume(granted)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun add(request: IosReminderRequest) {
        val content = UNMutableNotificationContent().apply {
            setTitle(request.title)
            setBody(request.body)
        }
        val components = NSDateComponents().apply {
            weekday = request.weekday.toLong()
            hour = request.hour.toLong()
            minute = request.minute.toLong()
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = true)
        val notificationRequest = UNNotificationRequest.requestWithIdentifier(request.identifier, content, trigger)

        suspendCancellableCoroutine<Unit> { continuation ->
            UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(notificationRequest) { error ->
                if (error != null) {
                    continuation.resumeWith(Result.failure(IllegalStateException(error.localizedDescription ?: "Failed to add notification request")))
                } else if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
        }
    }

    override fun removePendingRequests(identifiers: List<String>) {
        UNUserNotificationCenter.currentNotificationCenter().removePendingNotificationRequestsWithIdentifiers(identifiers)
    }

    override suspend fun pendingRequestIdentifiers(): List<String> = suspendCancellableCoroutine { continuation ->
        UNUserNotificationCenter.currentNotificationCenter().getPendingNotificationRequestsWithCompletionHandler { requests ->
            val pending = requests as? List<UNNotificationRequest>
            val identifiers: List<String> = pending?.map { request -> request.identifier } ?: emptyList()
            if (continuation.isActive) continuation.resume(identifiers)
        }
    }
}

internal fun reminderNotificationIdentifier(day: DayOfWeek): String = "togetherly.reminder.${day.name}"

internal fun reminderNotificationRequest(
    day: DayOfWeek,
    preference: com.togetherly.domain.family.ReminderPreference,
    title: String,
    body: String,
): IosReminderRequest = IosReminderRequest(
    identifier = reminderNotificationIdentifier(day),
    weekday = nsWeekdayFor(day),
    hour = preference.localTime.hour,
    minute = preference.localTime.minute,
    title = title,
    body = body,
)

private fun nsWeekdayFor(day: DayOfWeek): Int = if (day == DayOfWeek.SUNDAY) 1 else day.ordinal + 2

package com.togetherly.core.notification

/**
 * A one-shot "what's the current status" read — separate from [NotificationPermissionController]
 * (which *requests*, i.e. can show a system prompt) so the Reminder screen can display the current
 * state on load without ever triggering a prompt just by being viewed. Same split
 * [com.togetherly.core.media.MicrophonePermissionRequester] doesn't need (it only ever requests,
 * never displays a passive status), so this is a new, small interface rather than a forced fit
 * into that one's shape.
 */
interface NotificationPermissionStatusProvider {
    suspend fun currentStatus(): NotificationPermissionState
}

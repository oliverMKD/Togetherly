package com.togetherly.core.notification

/**
 * [NotDetermined] — the platform prompt has never been shown (Android: never asked; iOS:
 * `.notDetermined`). [PermanentlyDenied] means the platform will no longer show its own prompt —
 * same distinction [com.togetherly.core.media.MicrophonePermissionResult.PermanentlyDenied]
 * already makes, only Settings can change it from here. [NotRequired] is Android-only: below API
 * 33, posting a notification never needed a runtime permission at all — treating that as
 * equivalent to [Granted] would be misleading (the family never actually granted anything), so
 * callers that only care "can I post a notification" should treat both as go-ahead, but a
 * permission-status *display* should say something different for each.
 */
enum class NotificationPermissionState {
    NotDetermined,
    Granted,
    Denied,
    PermanentlyDenied,
    NotRequired,
}

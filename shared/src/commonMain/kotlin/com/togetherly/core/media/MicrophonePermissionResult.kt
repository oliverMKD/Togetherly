package com.togetherly.core.media

/**
 * [PermanentlyDenied] means the platform will no longer show its own permission prompt (Android:
 * denied once without "don't ask again" rationale available; iOS: any request after the very
 * first one just replays the stored answer) — the UI's only remaining option is directing the
 * user to system Settings, not asking again.
 */
sealed interface MicrophonePermissionResult {
    data object Granted : MicrophonePermissionResult
    data object Denied : MicrophonePermissionResult
    data object PermanentlyDenied : MicrophonePermissionResult
}

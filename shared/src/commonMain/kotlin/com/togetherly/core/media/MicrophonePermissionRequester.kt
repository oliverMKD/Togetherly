package com.togetherly.core.media

import androidx.compose.runtime.Composable

fun interface MicrophonePermissionRequester {
    fun request()
}

/**
 * Requests microphone permission through the platform's own system prompt — called only when the
 * user taps Record, never at onboarding/startup. [onResult] is how the caller finds out what
 * happened; nothing about the platform permission API itself is exposed past this seam.
 */
@Composable
expect fun rememberMicrophonePermissionRequester(
    onResult: (MicrophonePermissionResult) -> Unit,
): MicrophonePermissionRequester

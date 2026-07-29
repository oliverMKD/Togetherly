package com.togetherly.core.media

import androidx.compose.runtime.Composable

fun interface AppSettingsLauncher {
    fun launch()
}

/**
 * Opens this app's own system Settings page — the only recourse once microphone access is
 * permanently denied (see [MicrophonePermissionResult.PermanentlyDenied]'s own KDoc); there is no
 * in-app way to re-prompt at that point on either platform, only a system Settings toggle.
 */
@Composable
expect fun rememberAppSettingsLauncher(): AppSettingsLauncher

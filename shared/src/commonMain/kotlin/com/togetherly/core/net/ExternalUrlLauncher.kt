package com.togetherly.core.net

import androidx.compose.runtime.Composable

private val SUPPORTED_SCHEMES = listOf("https://", "mailto:")

/**
 * Whether [url] is safe to hand to a platform URL launcher — an explicit scheme allow-list
 * (`https://` for legal/subscription links, `mailto:` for a support-contact link), never a bare
 * "does this parse" check. ViewModels must call this before emitting a navigation event that opens
 * a URL (see [ExternalUrlLauncher]'s KDoc); platform actuals re-check it too as defense in depth.
 */
fun isValidExternalUrl(url: String): Boolean {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return false
    return when {
        trimmed.startsWith("https://", ignoreCase = true) -> trimmed.length > "https://".length
        trimmed.startsWith("mailto:", ignoreCase = true) -> trimmed.substringAfter(':').contains('@')
        else -> false
    }
}

/**
 * Opens an external URL through the platform's own URL handler. Never called directly from
 * reusable UI components — presentation validates with [isValidExternalUrl] and emits a
 * navigation event; only the owning Route resolves [rememberExternalUrlLauncher] and calls
 * [launch] in response to that event (same convention as
 * [com.togetherly.core.media.rememberAppSettingsLauncher]). Implementations swallow failures
 * (no app available to handle the URL, malformed URL) rather than crashing.
 */
fun interface ExternalUrlLauncher {
    fun launch(url: String)
}

@Composable
expect fun rememberExternalUrlLauncher(): ExternalUrlLauncher

package com.togetherly.feature.family.model

/**
 * [OpenExternalUrl] only ever carries a URL that already passed
 * [com.togetherly.core.net.isValidExternalUrl] — see [com.togetherly.feature.family.presentation.LegalViewModel]'s
 * own KDoc. An invalid/unconfigured URL simply never produces this event, rather than producing it
 * with a bad value for the Route to filter out.
 */
sealed interface LegalEvent {
    data object NavigateBack : LegalEvent
    data class OpenExternalUrl(val url: String) : LegalEvent
    data object OpenOpenSourceLicenses : LegalEvent
}

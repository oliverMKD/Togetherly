package com.togetherly.feature.family.model

/**
 * Deliberately excludes anything internal — applicationId, build paths, API keys. See
 * [com.togetherly.feature.family.presentation.AboutViewModel]'s own KDoc for the sources of each
 * field.
 */
data class AboutUiState(
    val applicationName: String = "",
    val versionName: String = "",
    val buildNumber: String = "",
    val showEnvironmentLabel: Boolean = false,
    val supportContactUrl: String? = null,
    val showDiagnosticsTestAction: Boolean = false,
    val diagnosticsTestJustSent: Boolean = false,
    val showDebugTelemetryAction: Boolean = false,
)

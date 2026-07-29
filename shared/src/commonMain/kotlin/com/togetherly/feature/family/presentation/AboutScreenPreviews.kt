package com.togetherly.feature.family.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.feature.family.model.AboutUiState

@Composable
private fun AboutPreview(state: AboutUiState) {
    TogetherlyTheme {
        AboutScreen(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun AboutReleaseNoSupportLinkPreview() {
    AboutPreview(
        AboutUiState(applicationName = "Togetherly", versionName = "1.0", buildNumber = "1", showEnvironmentLabel = false, supportContactUrl = null),
    )
}

@Preview
@Composable
private fun AboutDebugWithSupportLinkPreview() {
    AboutPreview(
        AboutUiState(
            applicationName = "Togetherly",
            versionName = "1.0",
            buildNumber = "1",
            showEnvironmentLabel = true,
            supportContactUrl = "mailto:support@example.com",
        ),
    )
}

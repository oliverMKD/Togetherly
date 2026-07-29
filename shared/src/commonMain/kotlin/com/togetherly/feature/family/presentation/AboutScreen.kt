package com.togetherly.feature.family.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.button.TogetherlyTextButton
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.family.model.AboutAction
import com.togetherly.feature.family.model.AboutUiState
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.about_debug_telemetry_action
import togetherly.shared.generated.resources.about_diagnostics_test_action
import togetherly.shared.generated.resources.about_diagnostics_test_sent
import togetherly.shared.generated.resources.about_environment_debug_label
import togetherly.shared.generated.resources.about_support_action
import togetherly.shared.generated.resources.about_title
import togetherly.shared.generated.resources.about_version_label

@Composable
internal fun AboutScreen(
    state: AboutUiState,
    onAction: (AboutAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        topBar = {
            TogetherlyTopBar(
                title = stringResource(Res.string.about_title),
                navigationIcon = {
                    TogetherlyIconButton(
                        icon = { Text("‹", style = MaterialTheme.togetherlyTypography.headlineM) },
                        contentDescription = "Back",
                        onClick = { onAction(AboutAction.BackClicked) },
                    )
                },
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s),
        ) {
            Text(
                text = state.applicationName,
                style = MaterialTheme.togetherlyTypography.headlineM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
            Text(
                text = stringResource(Res.string.about_version_label, state.versionName, state.buildNumber),
                style = MaterialTheme.togetherlyTypography.bodyM,
                color = MaterialTheme.togetherlyColors.foregroundSecondary,
            )
            if (state.showEnvironmentLabel) {
                Text(
                    text = stringResource(Res.string.about_environment_debug_label),
                    style = MaterialTheme.togetherlyTypography.bodyS,
                    color = MaterialTheme.togetherlyColors.foregroundSecondary,
                )
            }
            if (state.supportContactUrl != null) {
                TogetherlyTextButton(
                    label = stringResource(Res.string.about_support_action),
                    onClick = { onAction(AboutAction.SupportClicked) },
                )
            }
            if (state.showDiagnosticsTestAction) {
                TogetherlyTextButton(
                    label = stringResource(Res.string.about_diagnostics_test_action),
                    onClick = { onAction(AboutAction.SendTestDiagnosticClicked) },
                )
                if (state.diagnosticsTestJustSent) {
                    Text(
                        text = stringResource(Res.string.about_diagnostics_test_sent),
                        style = MaterialTheme.togetherlyTypography.bodyS,
                        color = MaterialTheme.togetherlyColors.foregroundSecondary,
                    )
                }
            }
            if (state.showDebugTelemetryAction) {
                TogetherlyTextButton(
                    label = stringResource(Res.string.about_debug_telemetry_action),
                    onClick = { onAction(AboutAction.OpenDebugTelemetryClicked) },
                )
            }
        }
    }
}

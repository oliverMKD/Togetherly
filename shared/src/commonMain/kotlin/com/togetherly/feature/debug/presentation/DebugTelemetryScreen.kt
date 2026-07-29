package com.togetherly.feature.debug.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.togetherly.core.telemetry.ProviderConfigurationStatus
import com.togetherly.core.telemetry.RecordedAnalyticsEvent
import com.togetherly.core.telemetry.RecordedBreadcrumb
import com.togetherly.core.telemetry.RecordedProviderError
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.button.TogetherlyTextButton
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.domain.telemetry.ConsentDecision
import com.togetherly.feature.debug.model.DebugTelemetryAction
import com.togetherly.feature.debug.model.DebugTelemetryUiState
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.debug_access_summary
import togetherly.shared.generated.resources.debug_action_clear_history
import togetherly.shared.generated.resources.debug_action_flush
import togetherly.shared.generated.resources.debug_action_refresh
import togetherly.shared.generated.resources.debug_action_send_test_event
import togetherly.shared.generated.resources.debug_action_send_test_exception
import togetherly.shared.generated.resources.debug_analytics_consent
import togetherly.shared.generated.resources.debug_diagnostics_consent
import togetherly.shared.generated.resources.debug_no_breadcrumbs
import togetherly.shared.generated.resources.debug_no_errors
import togetherly.shared.generated.resources.debug_no_events
import togetherly.shared.generated.resources.debug_posthog_status
import togetherly.shared.generated.resources.debug_revenuecat_status
import togetherly.shared.generated.resources.debug_section_access
import togetherly.shared.generated.resources.debug_section_actions
import togetherly.shared.generated.resources.debug_section_breadcrumbs
import togetherly.shared.generated.resources.debug_section_consent
import togetherly.shared.generated.resources.debug_section_errors
import togetherly.shared.generated.resources.debug_section_events
import togetherly.shared.generated.resources.debug_section_providers
import togetherly.shared.generated.resources.debug_sentry_status
import togetherly.shared.generated.resources.debug_telemetry_title
import togetherly.shared.generated.resources.debug_test_event_sent
import togetherly.shared.generated.resources.debug_test_exception_sent

@Composable
internal fun DebugTelemetryScreen(
    state: DebugTelemetryUiState,
    onAction: (DebugTelemetryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        topBar = {
            TogetherlyTopBar(
                title = stringResource(Res.string.debug_telemetry_title),
                navigationIcon = {
                    TogetherlyIconButton(
                        icon = { Text("‹", style = MaterialTheme.togetherlyTypography.headlineM) },
                        contentDescription = "Back",
                        onClick = { onAction(DebugTelemetryAction.BackClicked) },
                    )
                },
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m),
        ) {
            SectionHeader(stringResource(Res.string.debug_section_consent))
            BodyRow(stringResource(Res.string.debug_analytics_consent, state.analyticsConsent.toLabel()))
            BodyRow(stringResource(Res.string.debug_diagnostics_consent, state.diagnosticsConsent.toLabel()))

            SectionHeader(stringResource(Res.string.debug_section_providers))
            BodyRow(stringResource(Res.string.debug_posthog_status, state.postHogStatus.toLabel()))
            BodyRow(stringResource(Res.string.debug_sentry_status, state.sentryStatus.toLabel()))
            BodyRow(stringResource(Res.string.debug_revenuecat_status, state.revenueCatStatus.toLabel()))

            SectionHeader(stringResource(Res.string.debug_section_access))
            BodyRow(stringResource(Res.string.debug_access_summary, state.accessSummary))

            SectionHeader(stringResource(Res.string.debug_section_actions))
            TogetherlyTextButton(
                label = stringResource(Res.string.debug_action_refresh),
                onClick = { onAction(DebugTelemetryAction.RefreshClicked) },
            )
            TogetherlyTextButton(
                label = stringResource(Res.string.debug_action_flush),
                onClick = { onAction(DebugTelemetryAction.FlushClicked) },
            )
            TogetherlyTextButton(
                label = stringResource(Res.string.debug_action_clear_history),
                onClick = { onAction(DebugTelemetryAction.ClearHistoryClicked) },
            )
            TogetherlyTextButton(
                label = stringResource(Res.string.debug_action_send_test_event),
                onClick = { onAction(DebugTelemetryAction.SendTestEventClicked) },
            )
            if (state.testEventJustSent) {
                BodyRow(stringResource(Res.string.debug_test_event_sent))
            }
            TogetherlyTextButton(
                label = stringResource(Res.string.debug_action_send_test_exception),
                onClick = { onAction(DebugTelemetryAction.SendTestExceptionClicked) },
            )
            if (state.testExceptionJustSent) {
                BodyRow(stringResource(Res.string.debug_test_exception_sent))
            }

            SectionHeader(stringResource(Res.string.debug_section_events))
            if (state.recentEvents.isEmpty()) {
                BodyRow(stringResource(Res.string.debug_no_events))
            } else {
                state.recentEvents.forEach { event -> BodyRow(event.toDisplayLine()) }
            }

            SectionHeader(stringResource(Res.string.debug_section_breadcrumbs))
            if (state.recentBreadcrumbs.isEmpty()) {
                BodyRow(stringResource(Res.string.debug_no_breadcrumbs))
            } else {
                state.recentBreadcrumbs.forEach { breadcrumb -> BodyRow(breadcrumb.toDisplayLine()) }
            }

            SectionHeader(stringResource(Res.string.debug_section_errors))
            if (state.recentProviderErrors.isEmpty()) {
                BodyRow(stringResource(Res.string.debug_no_errors))
            } else {
                state.recentProviderErrors.forEach { error -> BodyRow(error.toDisplayLine()) }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text = text, style = MaterialTheme.togetherlyTypography.titleM, color = MaterialTheme.togetherlyColors.foregroundPrimary)
}

@Composable
private fun BodyRow(text: String) {
    Text(text = text, style = MaterialTheme.togetherlyTypography.bodyS, color = MaterialTheme.togetherlyColors.foregroundSecondary)
}

private fun ConsentDecision.toLabel(): String = when (this) {
    ConsentDecision.NotAsked -> "Not asked"
    ConsentDecision.Granted -> "Granted"
    ConsentDecision.Denied -> "Denied"
}

private fun ProviderConfigurationStatus.toLabel(): String = when (this) {
    ProviderConfigurationStatus.CONFIGURED -> "Configured"
    ProviderConfigurationStatus.MISSING -> "Missing"
    ProviderConfigurationStatus.DISABLED_BY_CONSENT -> "Disabled by consent"
    ProviderConfigurationStatus.INITIALIZATION_FAILED -> "Initialization failed"
}

private fun RecordedAnalyticsEvent.toDisplayLine(): String = "$name @ $recordedAt"
private fun RecordedBreadcrumb.toDisplayLine(): String = "$message (${category ?: "-"}) @ $recordedAt"
private fun RecordedProviderError.toDisplayLine(): String = "$category @ $recordedAt"

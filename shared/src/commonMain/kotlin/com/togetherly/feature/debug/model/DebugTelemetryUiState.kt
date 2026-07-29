package com.togetherly.feature.debug.model

import com.togetherly.core.telemetry.ProviderConfigurationStatus
import com.togetherly.core.telemetry.RecordedAnalyticsEvent
import com.togetherly.core.telemetry.RecordedBreadcrumb
import com.togetherly.core.telemetry.RecordedProviderError
import com.togetherly.domain.telemetry.ConsentDecision

/**
 * Every field here is either a closed enum, a count, or the already-sanitized history
 * [com.togetherly.core.telemetry.TelemetryDebugRecorder] itself holds — never a raw SDK key, a
 * full DSN, a RevenueCat App User ID, a PostHog distinct id, a receipt/purchase token, or any
 * family/memory content. See [com.togetherly.feature.debug.presentation.DebugTelemetryViewModel]'s
 * own KDoc.
 */
data class DebugTelemetryUiState(
    val analyticsConsent: ConsentDecision = ConsentDecision.NotAsked,
    val diagnosticsConsent: ConsentDecision = ConsentDecision.NotAsked,
    val postHogStatus: ProviderConfigurationStatus = ProviderConfigurationStatus.MISSING,
    val sentryStatus: ProviderConfigurationStatus = ProviderConfigurationStatus.MISSING,
    val revenueCatStatus: ProviderConfigurationStatus = ProviderConfigurationStatus.MISSING,
    val accessSummary: String = "",
    val recentEvents: List<RecordedAnalyticsEvent> = emptyList(),
    val recentBreadcrumbs: List<RecordedBreadcrumb> = emptyList(),
    val recentProviderErrors: List<RecordedProviderError> = emptyList(),
    val testEventJustSent: Boolean = false,
    val testExceptionJustSent: Boolean = false,
)

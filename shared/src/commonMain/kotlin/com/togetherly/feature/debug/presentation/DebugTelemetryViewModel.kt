package com.togetherly.feature.debug.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.DiagnosticContext
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.core.telemetry.ProductAnalytics
import com.togetherly.core.telemetry.ProviderConfigurationStatus
import com.togetherly.core.telemetry.DebugTestEvent
import com.togetherly.core.telemetry.TelemetryDebugRecorder
import com.togetherly.data.purchase.RevenueCatConfigurator
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.AccessSource
import com.togetherly.domain.purchase.PurchaseStartupState
import com.togetherly.domain.purchase.repository.EntitlementRepository
import com.togetherly.domain.telemetry.repository.TelemetryConsentRepository
import com.togetherly.feature.debug.model.DebugTelemetryAction
import com.togetherly.feature.debug.model.DebugTelemetryEvent
import com.togetherly.feature.debug.model.DebugTelemetryUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** The one synthetic exception this screen ever deliberately sends — see this class's own KDoc, mirroring [com.togetherly.feature.family.presentation.AboutViewModel]'s existing `TestDiagnosticException`. */
private object DebugTestException : Exception("Manual test exception triggered from the debug telemetry screen (debug build only).")

/**
 * Debug-only (Step 14.6) — only ever reachable from [com.togetherly.feature.family.presentation.AboutScreen]'s
 * own debug-gated "Open debug telemetry tools" action (see that class's own KDoc), never from
 * anywhere else. Makes analytics/diagnostics/RevenueCat state verifiable during development and a
 * live demo without exposing anything a family shouldn't see — see [DebugTelemetryUiState]'s own
 * KDoc for exactly what is and isn't shown, and `docs/debug-telemetry.md`.
 *
 * [ProductAnalytics.configurationStatus]/[OperationalDiagnostics.configurationStatus] and
 * [TelemetryDebugRecorder]'s three histories have no reactive stream of their own (they're plain
 * synchronous reads) — [refreshSnapshot] re-pulls all of them on construction, after every action
 * that could plausibly change one, and whenever [DebugTelemetryAction.RefreshClicked] is
 * dispatched. [TelemetryConsentRepository.observeConsent]/[EntitlementRepository.observeAccess]
 * are genuinely reactive, so those two are collected continuously instead.
 */
class DebugTelemetryViewModel(
    private val consentRepository: TelemetryConsentRepository,
    private val productAnalytics: ProductAnalytics,
    private val diagnostics: OperationalDiagnostics,
    private val revenueCatConfigurator: RevenueCatConfigurator,
    private val entitlementRepository: EntitlementRepository,
    private val recorder: TelemetryDebugRecorder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebugTelemetryUiState())
    val uiState: StateFlow<DebugTelemetryUiState> = _uiState.asStateFlow()

    private val _events = Channel<DebugTelemetryEvent>(Channel.BUFFERED)
    val events: Flow<DebugTelemetryEvent> = _events.receiveAsFlow()

    init {
        refreshSnapshot()

        consentRepository.observeConsent()
            .onEach { consent ->
                _uiState.value = _uiState.value.copy(analyticsConsent = consent.analytics, diagnosticsConsent = consent.diagnostics)
            }
            .launchIn(viewModelScope)

        entitlementRepository.observeAccess()
            .onEach { result -> _uiState.value = _uiState.value.copy(accessSummary = result.toAccessSummary()) }
            .launchIn(viewModelScope)
    }

    fun onAction(action: DebugTelemetryAction) {
        when (action) {
            DebugTelemetryAction.BackClicked -> send(DebugTelemetryEvent.NavigateBack)
            DebugTelemetryAction.RefreshClicked -> refreshSnapshot()
            DebugTelemetryAction.FlushClicked -> productAnalytics.flush()
            DebugTelemetryAction.ClearHistoryClicked -> {
                recorder.clear()
                refreshSnapshot()
            }
            DebugTelemetryAction.SendTestEventClicked -> {
                productAnalytics.capture(DebugTestEvent)
                refreshSnapshot()
                _uiState.value = _uiState.value.copy(testEventJustSent = true)
            }
            DebugTelemetryAction.SendTestExceptionClicked -> {
                diagnostics.captureHandledException(
                    DebugTestException,
                    DiagnosticContext(mapOf("feature" to "diagnostics", "operation" to "debug_screen_manual_test_capture")),
                )
                refreshSnapshot()
                _uiState.value = _uiState.value.copy(testExceptionJustSent = true)
            }
        }
    }

    private fun refreshSnapshot() {
        _uiState.value = _uiState.value.copy(
            postHogStatus = productAnalytics.configurationStatus(),
            sentryStatus = diagnostics.configurationStatus(),
            revenueCatStatus = revenueCatConfigurator.state.value.toProviderConfigurationStatus(),
            recentEvents = recorder.recentEvents(),
            recentBreadcrumbs = recorder.recentBreadcrumbs(),
            recentProviderErrors = recorder.recentProviderErrors(),
        )
    }

    private fun send(event: DebugTelemetryEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}

private fun PurchaseStartupState.toProviderConfigurationStatus(): ProviderConfigurationStatus = when (this) {
    // Initializing is transient and synchronous (see RevenueCatConfigurator's own KDoc — configure()
    // always finishes before app startup returns), so this screen can never realistically observe
    // it; folding it into MISSING ("not yet configured") is the closest honest bucket.
    PurchaseStartupState.NotConfigured, PurchaseStartupState.Initializing -> ProviderConfigurationStatus.MISSING
    PurchaseStartupState.Ready -> ProviderConfigurationStatus.CONFIGURED
    PurchaseStartupState.Failed -> ProviderConfigurationStatus.INITIALIZATION_FAILED
}

private fun DataResult<AccessSnapshot>.toAccessSummary(): String = when (this) {
    is DataResult.Error -> "Unknown (read error)"
    is DataResult.Success -> when (value.familyAccess.source) {
        AccessSource.FREE -> "Free"
        AccessSource.SUBSCRIPTION -> "Family Plus (subscription)"
        AccessSource.LIFETIME -> "Family Plus (lifetime)"
        AccessSource.PROMOTIONAL -> "Family Plus (promotional)"
        AccessSource.CACHED -> "Family Plus (cached)".takeIf { value.familyAccess.isPlus } ?: "Free (cached)"
    }
}

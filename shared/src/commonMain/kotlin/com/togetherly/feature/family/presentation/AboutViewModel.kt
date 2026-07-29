package com.togetherly.feature.family.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.app.application.AppConfiguration
import com.togetherly.app.application.LegalConfiguration
import com.togetherly.app.foundation.VersionInfoProvider
import com.togetherly.core.net.isValidExternalUrl
import com.togetherly.core.telemetry.DiagnosticContext
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.feature.family.model.AboutAction
import com.togetherly.feature.family.model.AboutEvent
import com.togetherly.feature.family.model.AboutUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** The one synthetic exception this app ever deliberately sends to diagnostics — see [AboutViewModel]'s own KDoc. */
private object TestDiagnosticException : Exception("Manual test diagnostic event triggered from the About screen (debug build only).")

/**
 * [AppConfiguration.applicationName]/[AppConfiguration.debug] and [VersionInfoProvider] are both
 * synchronous, already-resolved-at-startup reads — no `onScreenStarted`/loading state needed, same
 * as [FamilyPlusManagementViewModel]'s "no distinct RevenueCat-unavailable state" reasoning would
 * apply if this screen had anything to actually fail. [LegalConfiguration.supportContactUrl]
 * staying `null` is what hides the support row entirely in [AboutScreen], not an empty string.
 *
 * [AboutUiState.showDiagnosticsTestAction] mirrors [AboutUiState.showEnvironmentLabel]'s own
 * `appConfiguration.debug` gate — [AboutAction.SendTestDiagnosticClicked] calls
 * [OperationalDiagnostics.captureHandledException] with [TestDiagnosticException], a fixed,
 * already-safe synthetic exception (Step 14.5's "debug-only manual test action" requirement; see
 * `docs/sentry-setup.md`). This is never reachable on a release build: [AboutScreen] only renders
 * the button behind [AboutUiState.showDiagnosticsTestAction], so there is no production-accessible
 * crash/test-report button — a release build's `debug = false` means this action, even if somehow
 * dispatched, still only ever reaches [OperationalDiagnostics], never anything destructive.
 */
class AboutViewModel(
    appConfiguration: AppConfiguration,
    versionInfoProvider: VersionInfoProvider,
    private val legalConfiguration: LegalConfiguration,
    private val diagnostics: OperationalDiagnostics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AboutUiState(
            applicationName = appConfiguration.applicationName,
            versionName = versionInfoProvider.versionName(),
            buildNumber = versionInfoProvider.buildNumber(),
            showEnvironmentLabel = appConfiguration.debug,
            supportContactUrl = legalConfiguration.supportContactUrl,
            showDiagnosticsTestAction = appConfiguration.debug,
            showDebugTelemetryAction = appConfiguration.debug,
        ),
    )
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    private val _events = Channel<AboutEvent>(Channel.BUFFERED)
    val events: Flow<AboutEvent> = _events.receiveAsFlow()

    fun onAction(action: AboutAction) {
        when (action) {
            AboutAction.BackClicked -> send(AboutEvent.NavigateBack)
            AboutAction.SupportClicked -> {
                val url = legalConfiguration.supportContactUrl ?: return
                if (isValidExternalUrl(url)) send(AboutEvent.OpenExternalUrl(url))
            }
            AboutAction.SendTestDiagnosticClicked -> {
                if (!_uiState.value.showDiagnosticsTestAction) return
                diagnostics.captureHandledException(
                    TestDiagnosticException,
                    DiagnosticContext(mapOf("feature" to "diagnostics", "operation" to "manual_test_capture")),
                )
                _uiState.value = _uiState.value.copy(diagnosticsTestJustSent = true)
            }
            AboutAction.OpenDebugTelemetryClicked -> {
                if (!_uiState.value.showDebugTelemetryAction) return
                send(AboutEvent.OpenDebugTelemetry)
            }
        }
    }

    private fun send(event: AboutEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}

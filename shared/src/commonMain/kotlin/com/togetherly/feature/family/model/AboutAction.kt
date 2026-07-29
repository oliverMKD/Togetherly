package com.togetherly.feature.family.model

sealed interface AboutAction {
    data object BackClicked : AboutAction
    data object SupportClicked : AboutAction

    /** Debug-only — [com.togetherly.feature.family.presentation.AboutViewModel] never exposes [com.togetherly.feature.family.model.AboutUiState.showDiagnosticsTestAction] as `true` on a release build, so [AboutScreen][com.togetherly.feature.family.presentation.AboutScreen] never renders a button that could send this. */
    data object SendTestDiagnosticClicked : AboutAction

    /** Debug-only — same gate as [SendTestDiagnosticClicked], via [com.togetherly.feature.family.model.AboutUiState.showDebugTelemetryAction]. */
    data object OpenDebugTelemetryClicked : AboutAction
}

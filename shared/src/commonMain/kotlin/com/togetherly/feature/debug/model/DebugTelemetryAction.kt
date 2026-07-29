package com.togetherly.feature.debug.model

sealed interface DebugTelemetryAction {
    data object BackClicked : DebugTelemetryAction
    data object RefreshClicked : DebugTelemetryAction
    data object FlushClicked : DebugTelemetryAction
    data object ClearHistoryClicked : DebugTelemetryAction
    data object SendTestEventClicked : DebugTelemetryAction
    data object SendTestExceptionClicked : DebugTelemetryAction
}

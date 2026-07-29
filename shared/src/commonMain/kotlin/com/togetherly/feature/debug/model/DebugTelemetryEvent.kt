package com.togetherly.feature.debug.model

sealed interface DebugTelemetryEvent {
    data object NavigateBack : DebugTelemetryEvent
}

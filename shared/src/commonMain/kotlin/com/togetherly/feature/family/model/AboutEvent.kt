package com.togetherly.feature.family.model

sealed interface AboutEvent {
    data object NavigateBack : AboutEvent
    data class OpenExternalUrl(val url: String) : AboutEvent
    data object OpenDebugTelemetry : AboutEvent
}

package com.togetherly.feature.journey.presentation

sealed interface JourneyEvent {
    data object NavigateToToday : JourneyEvent
}

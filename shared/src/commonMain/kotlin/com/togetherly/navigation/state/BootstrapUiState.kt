package com.togetherly.navigation.state

import com.togetherly.core.ui.UiText

/**
 * What [com.togetherly.navigation.host.BootstrapScreen] renders while
 * [BootstrapViewModel] decides whether the family profile — the one source of truth for whether
 * onboarding has happened, never a stored boolean flag — exists yet.
 */
sealed interface BootstrapUiState {

    data object Loading : BootstrapUiState

    data object RequiresOnboarding : BootstrapUiState

    data object Ready : BootstrapUiState

    data class Error(val error: UiText) : BootstrapUiState
}

package com.togetherly.navigation.host

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.feedback.TogetherlyInlineError
import com.togetherly.designsystem.component.feedback.TogetherlyLoadingIndicator
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.navigation.state.BootstrapUiState
import com.togetherly.navigation.state.BootstrapViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * The stateful root of the [com.togetherly.navigation.destination.RootDestination.Bootstrap]
 * destination. [onRequiresOnboarding]/[onReady] fire exactly once per state transition (the
 * [LaunchedEffect] below is keyed on [BootstrapUiState] itself, not on every recomposition) —
 * navigating away is a one-shot side effect of the state changing, never something this
 * composable's own body triggers directly while rendering.
 *
 * Loading and the brief instant after the state resolves to [BootstrapUiState.RequiresOnboarding]/
 * [BootstrapUiState.Ready] (before navigation actually happens) render the same branded loading
 * UI — this is what keeps onboarding or Main from ever flashing on screen underneath Bootstrap.
 */
@Composable
fun BootstrapScreen(
    onRequiresOnboarding: () -> Unit,
    onReady: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BootstrapViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        when (state) {
            BootstrapUiState.RequiresOnboarding -> onRequiresOnboarding()
            BootstrapUiState.Ready -> onReady()
            BootstrapUiState.Loading, is BootstrapUiState.Error -> Unit
        }
    }

    TogetherlyScreen(modifier = modifier, scrollable = false) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (val current = state) {
                is BootstrapUiState.Error -> TogetherlyInlineError(
                    message = current.error.asString(),
                    onRetry = viewModel::retry,
                )
                BootstrapUiState.Loading,
                BootstrapUiState.RequiresOnboarding,
                BootstrapUiState.Ready,
                -> TogetherlyLoadingIndicator()
            }
        }
    }
}

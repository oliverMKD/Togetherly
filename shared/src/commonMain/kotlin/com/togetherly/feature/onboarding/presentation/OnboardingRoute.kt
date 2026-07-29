package com.togetherly.feature.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

/**
 * The stateful boundary between navigation and [OnboardingScreen] — the *only* place in this
 * feature that reads a [OnboardingViewModel] and the only place [onNavigateBack]/[onFamilyCreated]
 * (real navigation callbacks, wired to the app's actual `NavController` one layer up) are called.
 * [OnboardingScreen] itself never sees the ViewModel, matching this feature's own
 * state-in/action-out boundary throughout.
 *
 * Event collection uses `collectLatest` on a lifecycle-aware, `Composable`-scoped
 * [LaunchedEffect] keyed on `Unit` — started once per composition, not re-started on every
 * recomposition — so a [OnboardingEvent] is consumed exactly once, never repeated by an unrelated
 * recomposition.
 */
@Composable
fun OnboardingRoute(
    viewModel: OnboardingViewModel,
    onNavigateBack: () -> Unit,
    onFamilyCreated: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.onScreenStarted()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                OnboardingEvent.NavigateBack -> onNavigateBack()
                OnboardingEvent.FamilyCreated -> onFamilyCreated()
            }
        }
    }

    OnboardingScreen(state = state, onAction = viewModel::onAction)
}

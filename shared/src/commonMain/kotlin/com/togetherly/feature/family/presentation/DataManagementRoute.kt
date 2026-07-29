package com.togetherly.feature.family.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.feature.family.model.DataManagementEvent
import org.koin.compose.viewmodel.koinViewModel

/**
 * [BackHandler] (enabled only while [com.togetherly.feature.family.model.DataManagementUiState.isBusy])
 * blocks the system/gesture back action from bypassing an in-flight deletion — this is on top of
 * (not instead of) [DataManagementViewModel] itself no-op'ing [com.togetherly.feature.family.model.DataManagementAction.BackClicked]
 * while busy, since a hardware/gesture back press never goes through that action at all otherwise.
 * [BackHandler] is `@ExperimentalComposeUiApi` (and upstream-deprecated in favor of a
 * `NavigationEventHandler` not yet available in this project's Compose Multiplatform version) —
 * still the only commonMain mechanism available for this, so it's used deliberately here rather
 * than left unblocked.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DataManagementRoute(
    onNavigateBack: () -> Unit,
    onLocalDataDeleted: () -> Unit,
    viewModel: DataManagementViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = state.isBusy) {
        // Intentionally consumes the back gesture with no effect — see this file's own KDoc.
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                DataManagementEvent.NavigateBack -> onNavigateBack()
                DataManagementEvent.LocalDataDeleted -> onLocalDataDeleted()
            }
        }
    }

    DataManagementScreen(state = state, onAction = viewModel::onAction)
}

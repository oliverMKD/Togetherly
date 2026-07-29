package com.togetherly.feature.debug.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.feature.debug.model.DebugTelemetryEvent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DebugTelemetryRoute(
    onNavigateBack: () -> Unit,
    viewModel: DebugTelemetryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                DebugTelemetryEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    DebugTelemetryScreen(state = state, onAction = viewModel::onAction)
}

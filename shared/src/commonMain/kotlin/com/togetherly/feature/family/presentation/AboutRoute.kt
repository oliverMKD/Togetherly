package com.togetherly.feature.family.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.core.net.rememberExternalUrlLauncher
import com.togetherly.feature.family.model.AboutEvent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AboutRoute(
    onNavigateBack: () -> Unit,
    onOpenDebugTelemetry: () -> Unit,
    viewModel: AboutViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val urlLauncher = rememberExternalUrlLauncher()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AboutEvent.NavigateBack -> onNavigateBack()
                is AboutEvent.OpenExternalUrl -> urlLauncher.launch(event.url)
                AboutEvent.OpenDebugTelemetry -> onOpenDebugTelemetry()
            }
        }
    }

    AboutScreen(state = state, onAction = viewModel::onAction)
}

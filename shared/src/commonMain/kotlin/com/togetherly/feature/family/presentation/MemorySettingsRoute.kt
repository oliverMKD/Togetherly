package com.togetherly.feature.family.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.feature.family.model.MemorySettingsEvent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MemorySettingsRoute(
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    onOpenManageMemories: () -> Unit,
    viewModel: MemorySettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.onScreenStarted() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                MemorySettingsEvent.OpenManageMemories -> onOpenManageMemories()
                MemorySettingsEvent.SaveCompleted -> onSaved()
                MemorySettingsEvent.NavigatedBackWithoutSaving -> onNavigateBack()
            }
        }
    }

    MemorySettingsScreen(state = state, onAction = viewModel::onAction)
}

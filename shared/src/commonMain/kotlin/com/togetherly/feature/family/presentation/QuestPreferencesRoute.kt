package com.togetherly.feature.family.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.feature.family.model.QuestPreferencesEvent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun QuestPreferencesRoute(
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: QuestPreferencesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.onScreenStarted() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                QuestPreferencesEvent.SaveCompleted -> onSaved()
                QuestPreferencesEvent.NavigatedBackWithoutSaving -> onNavigateBack()
            }
        }
    }

    QuestPreferencesScreen(state = state, onAction = viewModel::onAction)
}

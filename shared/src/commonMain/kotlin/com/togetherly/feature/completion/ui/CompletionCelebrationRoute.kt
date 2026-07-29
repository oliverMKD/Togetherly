package com.togetherly.feature.completion.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.domain.completion.CompletionId
import com.togetherly.feature.completion.presentation.CompletionCelebrationAction
import com.togetherly.feature.completion.presentation.CompletionCelebrationEvent
import com.togetherly.feature.completion.presentation.CompletionCelebrationViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CompletionCelebrationRoute(
    completionId: CompletionId,
    onNavigateToToday: () -> Unit,
    onAddMemory: (CompletionId) -> Unit,
    viewModel: CompletionCelebrationViewModel = koinViewModel(key = completionId.value) { parametersOf(completionId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.onAction(CompletionCelebrationAction.ScreenStarted)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CompletionCelebrationEvent.NavigateToToday -> onNavigateToToday()
                is CompletionCelebrationEvent.NavigateToMemory -> onAddMemory(event.completionId)
            }
        }
    }

    CompletionCelebrationScreen(state = state, onAction = viewModel::onAction)
}

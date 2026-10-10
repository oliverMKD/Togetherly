package com.togetherly.feature.language.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.feature.language.model.LanguageEvent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LanguageRoute(
    onNavigateBack: () -> Unit,
    viewModel: LanguageViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                LanguageEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    LanguageScreen(state = state, onAction = viewModel::onAction)
}

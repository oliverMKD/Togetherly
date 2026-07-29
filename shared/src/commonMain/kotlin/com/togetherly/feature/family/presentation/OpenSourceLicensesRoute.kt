package com.togetherly.feature.family.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.togetherly.core.net.rememberExternalUrlLauncher
import com.togetherly.feature.family.model.OpenSourceLicensesEvent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OpenSourceLicensesRoute(
    onNavigateBack: () -> Unit,
    viewModel: OpenSourceLicensesViewModel = koinViewModel(),
) {
    val urlLauncher = rememberExternalUrlLauncher()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                OpenSourceLicensesEvent.NavigateBack -> onNavigateBack()
                is OpenSourceLicensesEvent.OpenExternalUrl -> urlLauncher.launch(event.url)
            }
        }
    }

    OpenSourceLicensesScreen(onAction = viewModel::onAction)
}

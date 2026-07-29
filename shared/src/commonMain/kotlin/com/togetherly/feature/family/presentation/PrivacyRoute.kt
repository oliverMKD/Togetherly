package com.togetherly.feature.family.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.togetherly.feature.family.model.PrivacyEvent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PrivacyRoute(
    onNavigateBack: () -> Unit,
    viewModel: PrivacyViewModel = koinViewModel(),
) {
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PrivacyEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    PrivacyScreen(onAction = viewModel::onAction)
}

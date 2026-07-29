package com.togetherly.feature.reminder.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.core.media.rememberAppSettingsLauncher
import com.togetherly.core.notification.rememberNotificationPermissionController
import com.togetherly.feature.reminder.model.ReminderAction
import com.togetherly.feature.reminder.model.ReminderEvent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReminderRoute(
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ReminderViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionController = rememberNotificationPermissionController { result ->
        viewModel.onAction(ReminderAction.PermissionResultReceived(result))
    }
    val settingsLauncher = rememberAppSettingsLauncher()

    LaunchedEffect(viewModel) { viewModel.onScreenStarted() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ReminderEvent.RequestNotificationPermission -> permissionController.request()
                ReminderEvent.OpenSystemSettings -> settingsLauncher.launch()
                ReminderEvent.SaveCompleted -> onSaved()
                ReminderEvent.NavigatedBackWithoutSaving -> onNavigateBack()
            }
        }
    }

    ReminderScreen(state = state, onAction = viewModel::onAction)
}

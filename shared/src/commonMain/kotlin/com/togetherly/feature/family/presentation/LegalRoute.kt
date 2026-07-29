package com.togetherly.feature.family.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.togetherly.core.net.rememberExternalUrlLauncher
import com.togetherly.feature.family.model.LegalEvent
import org.koin.compose.viewmodel.koinViewModel

/**
 * Only this Route ever calls [com.togetherly.core.net.ExternalUrlLauncher] — [LegalScreen] itself
 * only emits [com.togetherly.feature.family.model.LegalAction], never a URL directly, same
 * boundary [ReminderRoute][com.togetherly.feature.reminder.presentation.ReminderRoute] keeps
 * around `rememberAppSettingsLauncher`.
 */
@Composable
fun LegalRoute(
    onNavigateBack: () -> Unit,
    onOpenOpenSourceLicenses: () -> Unit,
    viewModel: LegalViewModel = koinViewModel(),
) {
    val urlLauncher = rememberExternalUrlLauncher()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                LegalEvent.NavigateBack -> onNavigateBack()
                is LegalEvent.OpenExternalUrl -> urlLauncher.launch(event.url)
                LegalEvent.OpenOpenSourceLicenses -> onOpenOpenSourceLicenses()
            }
        }
    }

    LegalScreen(onAction = viewModel::onAction)
}

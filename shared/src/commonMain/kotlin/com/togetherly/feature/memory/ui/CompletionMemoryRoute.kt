package com.togetherly.feature.memory.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.core.media.rememberAppSettingsLauncher
import com.togetherly.core.media.rememberMicrophonePermissionRequester
import com.togetherly.core.media.rememberPhotoPickerLauncher
import com.togetherly.core.result.DataResult
import com.togetherly.data.media.PrivateMediaStorage
import com.togetherly.domain.completion.CompletionId
import com.togetherly.feature.memory.model.PhotoPreviewReference
import com.togetherly.feature.memory.presentation.CompletionMemoryAction
import com.togetherly.feature.memory.presentation.CompletionMemoryEvent
import com.togetherly.feature.memory.presentation.CompletionMemoryViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Picker/permission launching happens only here, at the Route boundary, never inside
 * [CompletionMemoryViewModel] or [CompletionMemoryScreen] — see [rememberPhotoPickerLauncher]/
 * [rememberMicrophonePermissionRequester]'s own KDoc for why. Both results are forwarded back into
 * the ViewModel as ordinary actions, so the ViewModel never sees a platform picker/permission type.
 */
@Composable
fun CompletionMemoryRoute(
    completionId: CompletionId,
    onNavigateBack: () -> Unit,
    onMemorySaved: (CompletionId) -> Unit,
    onMemorySkipped: () -> Unit,
    viewModel: CompletionMemoryViewModel = koinViewModel(key = completionId.value) { parametersOf(completionId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val mediaStorage = koinInject<PrivateMediaStorage>()

    val photoPickerLauncher = rememberPhotoPickerLauncher { result ->
        viewModel.onAction(CompletionMemoryAction.PhotoImportCompleted(result))
    }
    val microphonePermissionRequester = rememberMicrophonePermissionRequester { result ->
        viewModel.onAction(CompletionMemoryAction.MicrophonePermissionResultReceived(result))
    }
    val appSettingsLauncher = rememberAppSettingsLauncher()

    LaunchedEffect(viewModel) {
        viewModel.onAction(CompletionMemoryAction.ScreenStarted)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CompletionMemoryEvent.LaunchPhotoPicker -> photoPickerLauncher.launch()
                CompletionMemoryEvent.RequestMicrophonePermission -> microphonePermissionRequester.request()
                is CompletionMemoryEvent.MemorySaved -> onMemorySaved(event.completionId)
                CompletionMemoryEvent.MemorySkipped -> onMemorySkipped()
                CompletionMemoryEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    CompletionMemoryScreen(
        state = state,
        onAction = viewModel::onAction,
        loadPhotoBytes = { reference ->
            val result = when (reference) {
                is PhotoPreviewReference.Pending -> mediaStorage.openPendingPhoto(reference.reference)
                is PhotoPreviewReference.Committed -> mediaStorage.openPhoto(reference.reference)
            }
            (result as? DataResult.Success)?.value?.bytes
        },
        onOpenSettings = appSettingsLauncher::launch,
    )
}

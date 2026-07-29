package com.togetherly.feature.journey.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.core.result.DataResult
import com.togetherly.data.media.PrivateMediaStorage
import com.togetherly.feature.journey.presentation.JourneyAction
import com.togetherly.feature.journey.presentation.JourneyEvent
import com.togetherly.feature.journey.presentation.JourneyViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * [DisposableEffect] stops any active playback the instant this Route leaves composition — the
 * "leaving Journey stops playback" requirement — by reusing the ordinary
 * [JourneyAction.StopVoiceClicked] action rather than a separate `ViewModel.onCleared()` override;
 * see [JourneyViewModel]'s own KDoc for why `onCleared()` isn't a safe place to launch a suspend
 * stop call ([androidx.lifecycle.ViewModel.viewModelScope] is already cancelled by the time it
 * runs).
 */
@Composable
fun JourneyRoute(
    onGoToToday: () -> Unit,
    viewModel: JourneyViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val mediaStorage = koinInject<PrivateMediaStorage>()

    LaunchedEffect(viewModel) {
        viewModel.onAction(JourneyAction.ScreenStarted)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                JourneyEvent.NavigateToToday -> onGoToToday()
            }
        }
    }

    DisposableEffect(viewModel) {
        onDispose { viewModel.onAction(JourneyAction.StopVoiceClicked) }
    }

    JourneyScreen(
        state = state,
        onAction = viewModel::onAction,
        loadPhotoBytes = { reference ->
            (mediaStorage.openPhotoThumbnail(reference) as? DataResult.Success)?.value?.bytes
        },
    )
}

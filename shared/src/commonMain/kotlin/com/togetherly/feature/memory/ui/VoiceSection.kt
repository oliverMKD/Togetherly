package com.togetherly.feature.memory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.button.TogetherlySecondaryButton
import com.togetherly.designsystem.component.button.TogetherlyTextButton
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.memory.model.CompletionMemoryUiState
import com.togetherly.feature.memory.presentation.CompletionMemoryAction
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.memory_voice_cancel_action
import togetherly.shared.generated.resources.memory_voice_heading
import togetherly.shared.generated.resources.memory_voice_max_duration_note
import togetherly.shared.generated.resources.memory_voice_pause_action
import togetherly.shared.generated.resources.memory_voice_play_action
import togetherly.shared.generated.resources.memory_voice_recording_label
import togetherly.shared.generated.resources.memory_voice_record_action
import togetherly.shared.generated.resources.memory_voice_remove_action
import togetherly.shared.generated.resources.memory_voice_rerecord_action
import togetherly.shared.generated.resources.memory_voice_stop_action

@Composable
internal fun VoiceSection(
    state: CompletionMemoryUiState,
    onAction: (CompletionMemoryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
        Text(
            text = stringResource(Res.string.memory_voice_heading),
            style = MaterialTheme.togetherlyTypography.titleM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )

        when {
            state.isRecording -> RecordingControls(state = state, onAction = onAction)
            state.voice != null -> RecordedVoiceControls(state = state, onAction = onAction)
            else -> {
                TogetherlySecondaryButton(
                    label = stringResource(Res.string.memory_voice_record_action),
                    onClick = { onAction(CompletionMemoryAction.RecordVoiceClicked) },
                    enabled = !state.isSaving,
                )
                Text(
                    text = stringResource(Res.string.memory_voice_max_duration_note),
                    style = MaterialTheme.togetherlyTypography.bodyS,
                    color = MaterialTheme.togetherlyColors.foregroundSecondary,
                )
            }
        }
    }
}

@Composable
private fun RecordingControls(state: CompletionMemoryUiState, onAction: (CompletionMemoryAction) -> Unit) {
    Text(
        text = stringResource(Res.string.memory_voice_recording_label, state.recordingElapsed ?: "0:00"),
        style = MaterialTheme.togetherlyTypography.bodyM,
        color = MaterialTheme.togetherlyColors.foregroundPrimary,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s)) {
        TogetherlyTextButton(
            label = stringResource(Res.string.memory_voice_stop_action),
            onClick = { onAction(CompletionMemoryAction.StopRecordingClicked) },
        )
        TogetherlyTextButton(
            label = stringResource(Res.string.memory_voice_cancel_action),
            onClick = { onAction(CompletionMemoryAction.CancelRecordingClicked) },
        )
    }
}

@Composable
private fun RecordedVoiceControls(state: CompletionMemoryUiState, onAction: (CompletionMemoryAction) -> Unit) {
    val voice = state.voice ?: return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s)) {
        TogetherlyIconButton(
            icon = { Text(if (state.isPlayingVoice) "❚❚" else "▶", style = MaterialTheme.togetherlyTypography.titleM) },
            contentDescription = stringResource(
                if (state.isPlayingVoice) Res.string.memory_voice_pause_action else Res.string.memory_voice_play_action,
            ),
            onClick = {
                onAction(if (state.isPlayingVoice) CompletionMemoryAction.PauseVoiceClicked else CompletionMemoryAction.PlayVoiceClicked)
            },
            enabled = !state.isSaving,
        )
        Text(
            text = voice.durationLabel,
            style = MaterialTheme.togetherlyTypography.bodyM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s)) {
        TogetherlyTextButton(
            label = stringResource(Res.string.memory_voice_rerecord_action),
            onClick = { onAction(CompletionMemoryAction.RecordVoiceClicked) },
            enabled = !state.isSaving,
        )
        TogetherlyTextButton(
            label = stringResource(Res.string.memory_voice_remove_action),
            onClick = { onAction(CompletionMemoryAction.RemoveVoiceClicked) },
            enabled = !state.isSaving,
        )
    }
}

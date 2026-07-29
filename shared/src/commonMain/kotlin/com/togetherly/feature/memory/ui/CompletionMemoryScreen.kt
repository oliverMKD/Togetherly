package com.togetherly.feature.memory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.button.TogetherlyPrimaryButton
import com.togetherly.designsystem.component.button.TogetherlyTextButton
import com.togetherly.designsystem.component.feedback.TogetherlyInlineError
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.memory.model.CompletionMemoryUiState
import com.togetherly.feature.memory.model.PhotoPreviewReference
import com.togetherly.feature.memory.presentation.CompletionMemoryAction
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.memory_back_content_description
import togetherly.shared.generated.resources.memory_neutral_quest_title
import togetherly.shared.generated.resources.memory_privacy_reassurance
import togetherly.shared.generated.resources.memory_save_action
import togetherly.shared.generated.resources.memory_screen_title
import togetherly.shared.generated.resources.memory_skip_action
import togetherly.shared.generated.resources.memory_voice_open_settings_action

/**
 * Stateless: every value comes from [state], every intent goes out through [onAction] — see
 * [com.togetherly.feature.completion.ui.CompletionCelebrationScreen]'s own KDoc for the same
 * convention. [loadPhotoBytes] is the one exception to "everything through state/onAction": it's a
 * suspend function, not UI state, supplied by the Route (backed by
 * [com.togetherly.data.media.PrivateMediaStorage]) purely so [PhotoSection] can decode a private
 * photo for preview without this screen ever resolving a dependency itself.
 */
@Composable
internal fun CompletionMemoryScreen(
    state: CompletionMemoryUiState,
    onAction: (CompletionMemoryAction) -> Unit,
    loadPhotoBytes: suspend (PhotoPreviewReference) -> ByteArray?,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        topBar = {
            TogetherlyTopBar(
                title = stringResource(Res.string.memory_screen_title),
                navigationIcon = {
                    TogetherlyIconButton(
                        icon = { Text("‹", style = MaterialTheme.togetherlyTypography.headlineM) },
                        contentDescription = stringResource(Res.string.memory_back_content_description),
                        onClick = { onAction(CompletionMemoryAction.BackClicked) },
                        enabled = !state.isSaving,
                    )
                },
            )
        },
        bottomBar = {
            CompletionMemoryActionsBar(state = state, onAction = onAction)
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.togetherlySpacing.m),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l),
        ) {
            state.questTitle?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.togetherlyTypography.titleL,
                    color = MaterialTheme.togetherlyColors.foregroundPrimary,
                )
            } ?: Text(
                text = stringResource(Res.string.memory_neutral_quest_title),
                style = MaterialTheme.togetherlyTypography.titleL,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )

            ReactionsSection(state = state, onAction = onAction)
            if (state.allowTextNotes) {
                NoteSection(state = state, onAction = onAction)
            }
            if (state.allowPhotos) {
                PhotoSection(state = state, onAction = onAction, loadPhotoBytes = loadPhotoBytes)
            }
            if (state.allowVoiceMemories) {
                VoiceSection(state = state, onAction = onAction)
            }
            state.mediaError?.let { error -> TogetherlyInlineError(message = error.asString()) }
            if (state.microphonePermissionPermanentlyDenied) {
                TogetherlyTextButton(
                    label = stringResource(Res.string.memory_voice_open_settings_action),
                    onClick = onOpenSettings,
                )
            }

            Text(
                text = stringResource(Res.string.memory_privacy_reassurance),
                style = MaterialTheme.togetherlyTypography.bodyS,
                color = MaterialTheme.togetherlyColors.foregroundSecondary,
            )

            state.saveError?.let { error -> TogetherlyInlineError(message = error.asString()) }
        }
    }

    if (state.showDiscardConfirmation) {
        DiscardConfirmationDialog(onAction = onAction)
    }
}

@Composable
private fun CompletionMemoryActionsBar(
    state: CompletionMemoryUiState,
    onAction: (CompletionMemoryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s),
    ) {
        TogetherlyPrimaryButton(
            label = stringResource(Res.string.memory_save_action),
            onClick = { onAction(CompletionMemoryAction.SaveClicked) },
            enabled = !state.isRecording,
            loading = state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        TogetherlyTextButton(
            label = stringResource(Res.string.memory_skip_action),
            onClick = { onAction(CompletionMemoryAction.SkipClicked) },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

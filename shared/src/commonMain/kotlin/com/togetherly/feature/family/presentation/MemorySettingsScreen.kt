package com.togetherly.feature.family.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.button.TogetherlyPrimaryButton
import com.togetherly.designsystem.component.button.TogetherlyTextButton
import com.togetherly.designsystem.component.feedback.TogetherlyInlineError
import com.togetherly.designsystem.component.feedback.TogetherlyLoadingIndicator
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.component.selection.TogetherlyChipFlowRow
import com.togetherly.designsystem.component.selection.TogetherlyChoiceChip
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.family.model.MemorySettingsAction
import com.togetherly.feature.family.model.MemorySettingsUiState
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.family_discard_dialog_body
import togetherly.shared.generated.resources.family_discard_dialog_cancel_action
import togetherly.shared.generated.resources.family_discard_dialog_confirm_action
import togetherly.shared.generated.resources.family_discard_dialog_title
import togetherly.shared.generated.resources.memory_settings_explanation
import togetherly.shared.generated.resources.memory_settings_manage_memories_action
import togetherly.shared.generated.resources.memory_settings_off
import togetherly.shared.generated.resources.memory_settings_on
import togetherly.shared.generated.resources.memory_settings_photos_title
import togetherly.shared.generated.resources.memory_settings_preserved_explanation
import togetherly.shared.generated.resources.memory_settings_prompt_title
import togetherly.shared.generated.resources.memory_settings_save_action
import togetherly.shared.generated.resources.memory_settings_text_notes_title
import togetherly.shared.generated.resources.memory_settings_title
import togetherly.shared.generated.resources.memory_settings_voice_title

@Composable
internal fun MemorySettingsScreen(
    state: MemorySettingsUiState,
    onAction: (MemorySettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        topBar = {
            TogetherlyTopBar(
                title = stringResource(Res.string.memory_settings_title),
                navigationIcon = {
                    TogetherlyIconButton(
                        icon = { Text("‹", style = MaterialTheme.togetherlyTypography.headlineM) },
                        contentDescription = "Back",
                        onClick = { onAction(MemorySettingsAction.BackClicked) },
                    )
                },
            )
        },
    ) {
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TogetherlyLoadingIndicator()
            }
            else -> MemorySettingsContent(state = state, onAction = onAction)
        }
    }

    if (state.showDiscardDialog) {
        MemorySettingsDiscardDialog(onAction = onAction)
    }
}

@Composable
private fun MemorySettingsContent(
    state: MemorySettingsUiState,
    onAction: (MemorySettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l),
    ) {
        Text(
            text = stringResource(Res.string.memory_settings_explanation),
            style = MaterialTheme.togetherlyTypography.bodyM,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )

        if (state.error != null) {
            TogetherlyInlineError(message = state.error.asString())
        }

        ToggleRow(
            title = stringResource(Res.string.memory_settings_photos_title),
            value = state.allowPhotos,
            onValueChanged = { onAction(MemorySettingsAction.AllowPhotosChanged(it)) },
        )
        ToggleRow(
            title = stringResource(Res.string.memory_settings_voice_title),
            value = state.allowVoiceMemories,
            onValueChanged = { onAction(MemorySettingsAction.AllowVoiceMemoriesChanged(it)) },
        )
        ToggleRow(
            title = stringResource(Res.string.memory_settings_text_notes_title),
            value = state.allowTextNotes,
            onValueChanged = { onAction(MemorySettingsAction.AllowTextNotesChanged(it)) },
        )

        Text(
            text = stringResource(Res.string.memory_settings_preserved_explanation),
            style = MaterialTheme.togetherlyTypography.bodyS,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )
        TogetherlyTextButton(
            label = stringResource(Res.string.memory_settings_manage_memories_action),
            onClick = { onAction(MemorySettingsAction.ManageMemoriesClicked) },
        )

        ToggleRow(
            title = stringResource(Res.string.memory_settings_prompt_title),
            value = state.showMemoryPromptAfterQuests,
            onValueChanged = { onAction(MemorySettingsAction.ShowMemoryPromptAfterQuestsChanged(it)) },
        )

        TogetherlyPrimaryButton(
            label = stringResource(Res.string.memory_settings_save_action),
            onClick = { onAction(MemorySettingsAction.SaveClicked) },
            modifier = Modifier.fillMaxWidth(),
            loading = state.isSaving,
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    value: Boolean,
    onValueChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.togetherlyTypography.titleM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )
        TogetherlyChipFlowRow(modifier = Modifier.fillMaxWidth()) {
            TogetherlyChoiceChip(
                label = stringResource(Res.string.memory_settings_on),
                selected = value,
                onClick = { onValueChanged(true) },
            )
            TogetherlyChoiceChip(
                label = stringResource(Res.string.memory_settings_off),
                selected = !value,
                onClick = { onValueChanged(false) },
            )
        }
    }
}

@Composable
private fun MemorySettingsDiscardDialog(onAction: (MemorySettingsAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(MemorySettingsAction.DismissDiscardDialog) },
        title = { Text(stringResource(Res.string.family_discard_dialog_title), style = MaterialTheme.togetherlyTypography.titleL) },
        text = { Text(stringResource(Res.string.family_discard_dialog_body), style = MaterialTheme.togetherlyTypography.bodyM) },
        confirmButton = {
            TextButton(onClick = { onAction(MemorySettingsAction.DiscardConfirmed) }) {
                Text(stringResource(Res.string.family_discard_dialog_confirm_action), color = MaterialTheme.togetherlyColors.error)
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(MemorySettingsAction.DismissDiscardDialog) }) {
                Text(stringResource(Res.string.family_discard_dialog_cancel_action))
            }
        },
    )
}

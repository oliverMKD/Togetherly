package com.togetherly.feature.family.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.feature.family.model.MemorySettingsUiState

private val ALL_ENABLED_STATE = MemorySettingsUiState(
    isLoading = false,
    allowPhotos = true,
    allowVoiceMemories = true,
    allowTextNotes = true,
    showMemoryPromptAfterQuests = true,
)

@Composable
private fun MemorySettingsPreview(state: MemorySettingsUiState) {
    TogetherlyTheme {
        MemorySettingsScreen(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun MemorySettingsAllEnabledPreview() {
    MemorySettingsPreview(ALL_ENABLED_STATE)
}

@Preview
@Composable
private fun MemorySettingsPhotosDisabledPreview() {
    MemorySettingsPreview(ALL_ENABLED_STATE.copy(allowPhotos = false))
}

@Preview
@Composable
private fun MemorySettingsVoiceDisabledPreview() {
    MemorySettingsPreview(ALL_ENABLED_STATE.copy(allowVoiceMemories = false))
}

@Preview
@Composable
private fun MemorySettingsAllOptionalCaptureDisabledPreview() {
    MemorySettingsPreview(
        ALL_ENABLED_STATE.copy(allowPhotos = false, allowVoiceMemories = false, allowTextNotes = false),
    )
}

@Preview
@Composable
private fun MemorySettingsLoadingPreview() {
    MemorySettingsPreview(MemorySettingsUiState(isLoading = true))
}

@Preview
@Composable
private fun MemorySettingsUnsavedChangesPreview() {
    MemorySettingsPreview(ALL_ENABLED_STATE.copy(hasUnsavedChanges = true, showDiscardDialog = true))
}

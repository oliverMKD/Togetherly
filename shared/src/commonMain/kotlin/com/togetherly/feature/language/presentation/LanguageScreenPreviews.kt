package com.togetherly.feature.language.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.feature.language.model.LanguageUiState

@Preview
@Composable
private fun LanguageSystemDefaultPreview() {
    TogetherlyTheme {
        LanguageScreen(state = LanguageUiState(selectedCode = null), onAction = {})
    }
}

@Preview
@Composable
private fun LanguageMacedonianSelectedPreview() {
    TogetherlyTheme {
        LanguageScreen(state = LanguageUiState(selectedCode = "mk"), onAction = {})
    }
}

@Preview
@Composable
private fun LanguageRestartRequiredPreview() {
    TogetherlyTheme {
        LanguageScreen(state = LanguageUiState(selectedCode = "de", showRestartPrompt = true), onAction = {})
    }
}

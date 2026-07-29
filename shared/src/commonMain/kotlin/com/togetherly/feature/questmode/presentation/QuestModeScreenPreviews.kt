package com.togetherly.feature.questmode.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.feature.questmode.preview.abandonConfirmationQuestModeUiState
import com.togetherly.feature.questmode.preview.completingQuestModeUiState
import com.togetherly.feature.questmode.preview.errorQuestModeUiState
import com.togetherly.feature.questmode.preview.exitConfirmationQuestModeUiState
import com.togetherly.feature.questmode.preview.finishedTimerQuestModeUiState
import com.togetherly.feature.questmode.preview.loadingQuestModeUiState
import com.togetherly.feature.questmode.preview.phoneDownQuestModeUiState
import com.togetherly.feature.questmode.preview.runningTimerQuestModeUiState
import com.togetherly.feature.questmode.preview.untimedQuestModeUiState
import com.togetherly.feature.questmode.preview.withHintsQuestModeUiState
import com.togetherly.feature.questmode.preview.withSafetyNoteQuestModeUiState

@Composable
private fun QuestModePreview(state: QuestModeUiState, darkTheme: Boolean = false) {
    TogetherlyTheme(darkTheme = darkTheme) {
        QuestModeScreen(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun QuestModeLoadingPreview() {
    QuestModePreview(loadingQuestModeUiState())
}

@Preview
@Composable
private fun QuestModeUntimedPreview() {
    QuestModePreview(untimedQuestModeUiState())
}

@Preview
@Composable
private fun QuestModeRunningTimerPreview() {
    QuestModePreview(runningTimerQuestModeUiState())
}

@Preview
@Composable
private fun QuestModeFinishedTimerPreview() {
    QuestModePreview(finishedTimerQuestModeUiState())
}

@Preview
@Composable
private fun QuestModeWithHintsPreview() {
    QuestModePreview(withHintsQuestModeUiState())
}

@Preview
@Composable
private fun QuestModeWithSafetyNotePreview() {
    QuestModePreview(withSafetyNoteQuestModeUiState())
}

@Preview
@Composable
private fun QuestModePhoneDownPreview() {
    QuestModePreview(phoneDownQuestModeUiState())
}

@Preview
@Composable
private fun QuestModeExitConfirmationPreview() {
    QuestModePreview(exitConfirmationQuestModeUiState())
}

@Preview
@Composable
private fun QuestModeAbandonConfirmationPreview() {
    QuestModePreview(abandonConfirmationQuestModeUiState())
}

@Preview
@Composable
private fun QuestModeCompletingPreview() {
    QuestModePreview(completingQuestModeUiState())
}

@Preview
@Composable
private fun QuestModeErrorPreview() {
    QuestModePreview(errorQuestModeUiState())
}

@Preview
@Composable
private fun QuestModeDarkPreview() {
    QuestModePreview(runningTimerQuestModeUiState(), darkTheme = true)
}

@Preview(fontScale = 2f)
@Composable
private fun QuestModeLargeFontPreview() {
    QuestModePreview(runningTimerQuestModeUiState())
}

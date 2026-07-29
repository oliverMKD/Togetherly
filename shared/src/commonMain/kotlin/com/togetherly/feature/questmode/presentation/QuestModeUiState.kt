package com.togetherly.feature.questmode.presentation

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText
import com.togetherly.feature.questmode.model.QuestModeContentUi

/**
 * [Content.showExitConfirmation]/[Content.showAbandonConfirmation] are two separate `Boolean`s
 * rather than one sealed "dialog" state deliberately: they represent two different dialogs shown
 * at two different points in the same flow (exit first, then — only if the family explicitly
 * chooses to abandon from within it — a second, more destructive confirmation), never
 * simultaneously and never in a way one could contradict the other, so a sealed substate would add
 * a case for a combination ("both showing") that can never actually happen.
 */
@Immutable
sealed interface QuestModeUiState {

    data object Loading : QuestModeUiState

    data class Content(
        val quest: QuestModeContentUi,
        val phoneDown: Boolean = false,
        val hintsExpanded: Boolean = false,
        val showExitConfirmation: Boolean = false,
        val showAbandonConfirmation: Boolean = false,
        val isCompleting: Boolean = false,
        val error: UiText? = null,
    ) : QuestModeUiState

    data class Error(
        val message: UiText,
        val canRetry: Boolean,
        val canClose: Boolean,
    ) : QuestModeUiState
}

package com.togetherly.feature.questdetail.model

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText

/**
 * [activeSessionConflict] gates the "another quest is still in progress" confirmation — not part
 * of the task's own literal field list, added the same way Today's own
 * [com.togetherly.feature.today.presentation.TodayUiState.rerollConfirmationVisible] was: a
 * confirmation needs somewhere to live, and it is presentation-only dialog-visibility state, never
 * persisted or domain-meaningful on its own.
 *
 * [locked] (Step 11.6) is [com.togetherly.domain.quest.QuestAccess.Premium] content the current
 * family isn't entitled to — the quest stays fully visible/browsable (title, summary,
 * instructions, materials) even while locked; only starting it is blocked, replaced by an "unlock"
 * action that opens the Family Plus paywall (see [com.togetherly.feature.questdetail.presentation.QuestDetailAction.UnlockClicked]).
 * Never mutated by a purchase alone — only a live [com.togetherly.domain.purchase.repository.EntitlementRepository]
 * update recomputes it (see [com.togetherly.feature.questdetail.presentation.QuestDetailViewModel.onScreenStarted]).
 */
@Immutable
sealed interface QuestDetailUiState {

    data object Loading : QuestDetailUiState

    data class Content(
        val quest: QuestDetailUi,
        val isSaved: Boolean,
        val isStarting: Boolean,
        val startError: UiText? = null,
        val activeSessionConflict: Boolean = false,
        val locked: Boolean = false,
    ) : QuestDetailUiState

    data class Error(
        val message: UiText,
    ) : QuestDetailUiState
}

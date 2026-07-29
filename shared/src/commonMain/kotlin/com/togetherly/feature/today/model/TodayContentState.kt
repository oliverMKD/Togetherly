package com.togetherly.feature.today.model

import com.togetherly.core.ui.UiText

/**
 * Today's content phase as one sealed type — never three unrelated `Boolean`s (`isLoading`,
 * `isError`, `hasQuest`) a screen has to reconcile itself. [Mystery]/[Revealed] both carry the same
 * [QuestCardUi]; only Today's presentation layer decides whether to render it hidden or shown (see
 * [com.togetherly.feature.today.presentation.TodayUiState]'s own KDoc for why reveal is
 * presentation-only state, never persisted).
 */
sealed interface TodayContentState {

    data object Loading : TodayContentState

    data class Mystery(
        val quest: QuestCardUi,
    ) : TodayContentState

    data class Revealed(
        val quest: QuestCardUi,
    ) : TodayContentState

    /**
     * Today's daily quest has already been completed for the current local day (Step 9.6) — a
     * distinct state, not a flag layered onto [Revealed], since a completed quest's primary action
     * is deliberately different (no Start; see [com.togetherly.feature.today.presentation.TodayViewModel]'s
     * own KDoc for how this is detected) and it must never silently reopen the — by now cleared —
     * active session.
     */
    data class Completed(
        val quest: QuestCardUi,
    ) : TodayContentState

    data class Error(
        val message: UiText,
        val retryAvailable: Boolean,
    ) : TodayContentState
}

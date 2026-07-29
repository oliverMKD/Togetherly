package com.togetherly.feature.completion.model

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText
import com.togetherly.feature.today.model.QuestCategoryUi

/**
 * [quest] is nullable inside [Content] — a completion is shown as soon as it's persisted even if
 * its quest content can no longer be resolved from the catalogue (mirrors
 * [com.togetherly.domain.journey.JourneyEntry.quest]'s own nullability, same reason: "a family's
 * memory is never hidden just because the quest content behind it is gone"). The screen never
 * invents a title when this is `null` — it falls back to neutral copy instead.
 */
@Immutable
sealed interface CompletionCelebrationUiState {

    data object Loading : CompletionCelebrationUiState

    /**
     * [showAddMemoryPrompt] (Step 13.5) reflects [com.togetherly.domain.family.MemoryPreferences.showMemoryPromptAfterQuests],
     * loaded once before this state is ever shown — defaults `true` so a state built before that
     * load resolves still offers the prompt, matching [com.togetherly.domain.family.MemoryPreferences.defaults].
     * When `false`, [com.togetherly.feature.completion.ui.CompletionCelebrationScreen] shows a plain
     * "Continue" action instead of "Add a memory" — this screen must always have *some* way
     * forward, never a dead end.
     */
    data class Content(
        val quest: CompletionCelebrationQuestUi?,
        val completedAtDisplay: String,
        val showAddMemoryPrompt: Boolean = true,
    ) : CompletionCelebrationUiState

    data class Error(
        val message: UiText,
    ) : CompletionCelebrationUiState
}

@Immutable
data class CompletionCelebrationQuestUi(
    val title: String,
    val category: QuestCategoryUi,
)

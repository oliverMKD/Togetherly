package com.togetherly.feature.questmode.preview

import com.togetherly.core.ui.UiText
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.quest.QuestId
import com.togetherly.feature.questmode.model.InstructionStepUi
import com.togetherly.feature.questmode.model.QuestModeContentUi
import com.togetherly.feature.questmode.model.QuestTimerUi
import com.togetherly.feature.questmode.presentation.QuestModeUiState
import com.togetherly.feature.today.model.QuestCategoryUi
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/**
 * A living catalogue of Quest Mode's states, for Composable previews — never a substitute for
 * [com.togetherly.feature.questmode.presentation.QuestModeViewModelTest] (behavior) or the
 * instrumented UI tests (semantics/interaction). Every fixture is fictional sample data.
 */

fun sampleContentUi(
    timer: QuestTimerUi = QuestTimerUi.Hidden,
    hasHints: Boolean = true,
    hasSafetyNote: Boolean = false,
) = QuestModeContentUi(
    completionId = CompletionId("preview-completion"),
    questId = QuestId("preview-quest"),
    title = "Backyard Scavenger Hunt",
    category = QuestCategoryUi.DISCOVER,
    instructions = persistentListOf(
        InstructionStepUi(1, "Hide five small objects in the yard."),
        InstructionStepUi(2, "Give clues to find each one."),
        InstructionStepUi(3, "Celebrate when every object is found."),
    ),
    hints = if (hasHints) listOf("Try the garden.", "Look under things.").toPersistentList() else persistentListOf(),
    safetyNote = if (hasSafetyNote) "Adult supervision required near the street." else null,
    timer = timer,
    phoneDownSupported = true,
    keepScreenOnRequested = timer != QuestTimerUi.Hidden,
)

fun loadingQuestModeUiState() = QuestModeUiState.Loading

fun untimedQuestModeUiState() = QuestModeUiState.Content(quest = sampleContentUi(timer = QuestTimerUi.Hidden))

fun runningTimerQuestModeUiState() = QuestModeUiState.Content(
    quest = sampleContentUi(timer = QuestTimerUi.Running(displayTime = "9:05", progress = 0.1f)),
)

fun finishedTimerQuestModeUiState() = QuestModeUiState.Content(quest = sampleContentUi(timer = QuestTimerUi.Finished))

fun withHintsQuestModeUiState() = QuestModeUiState.Content(quest = sampleContentUi(hasHints = true), hintsExpanded = true)

fun withSafetyNoteQuestModeUiState() = QuestModeUiState.Content(quest = sampleContentUi(hasSafetyNote = true))

fun phoneDownQuestModeUiState() = QuestModeUiState.Content(
    quest = sampleContentUi(timer = QuestTimerUi.Running(displayTime = "4:12", progress = 0.6f)),
    phoneDown = true,
)

fun exitConfirmationQuestModeUiState() = QuestModeUiState.Content(quest = sampleContentUi(), showExitConfirmation = true)

fun abandonConfirmationQuestModeUiState() = QuestModeUiState.Content(quest = sampleContentUi(), showAbandonConfirmation = true)

fun completingQuestModeUiState() = QuestModeUiState.Content(quest = sampleContentUi(), isCompleting = true)

fun errorQuestModeUiState() = QuestModeUiState.Error(
    message = UiText.Dynamic("Something went wrong. Please try again."),
    canRetry = true,
    canClose = true,
)

package com.togetherly.feature.questmode.model

import androidx.compose.runtime.Immutable
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.quest.QuestId
import com.togetherly.feature.today.model.QuestCategoryUi
import kotlinx.collections.immutable.PersistentList

/**
 * Never [com.togetherly.domain.quest.FamilyQuest] — a Composable only ever sees this.
 * [phoneDownSupported] is a UI-facing capability flag, not the phone-down state itself (that's
 * [com.togetherly.feature.questmode.presentation.QuestModeUiState.Content.phoneDown]) — an untimed
 * quest may still support a distraction-free phone-down state (see Step 9.5's own spec), so
 * "phone-down is available" and "a timer exists" are deliberately independent.
 *
 * [keepScreenOnRequested] mirrors [com.togetherly.domain.quest.QuestTimer.keepScreenOn] — the
 * product's *request*, not whether the screen is actually being kept on right now (phone-down mode
 * and the timer already finishing both override it; see
 * [com.togetherly.core.feedback.KeepScreenOnEffect]'s own call site in
 * [com.togetherly.feature.questmode.presentation.QuestModeRoute] for where those overrides apply).
 */
@Immutable
data class QuestModeContentUi(
    val completionId: CompletionId,
    val questId: QuestId,
    val title: String,
    val category: QuestCategoryUi,
    val instructions: PersistentList<InstructionStepUi>,
    val hints: PersistentList<String>,
    val safetyNote: String?,
    val timer: QuestTimerUi,
    val phoneDownSupported: Boolean,
    val keepScreenOnRequested: Boolean,
)

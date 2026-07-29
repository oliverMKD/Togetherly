package com.togetherly.feature.questdetail.model

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText
import com.togetherly.domain.quest.QuestId
import com.togetherly.feature.today.model.QuestCategoryUi

/**
 * Never the full [com.togetherly.domain.quest.FamilyQuest] — a Composable only ever sees this.
 * [instructions]/[hints] are genuinely *absent* (empty lists), never merely hidden by the screen,
 * whenever the quest was mapped while locked (Step 12.6) — see
 * [com.togetherly.feature.questdetail.mapper.toDetailUi]'s own KDoc for why this matters more than
 * a UI-only visibility toggle ("Locked instructions are not accidentally exposed through
 * semantics"). [materials]/[safetyNote] stay populated even when locked — required materials "at a
 * high level" and safety information are both things a free family should be able to read before
 * deciding whether to unlock, per this feature's own task spec.
 *
 * [category] reuses Today's [QuestCategoryUi] rather than a duplicate detail-only enum — the same
 * category needs the same label/color mapping regardless of which screen shows it.
 */
@Immutable
data class QuestDetailUi(
    val id: QuestId,
    val title: String,
    val summary: String,
    val category: QuestCategoryUi,
    val durationLabel: UiText,
    val locationLabel: UiText,
    val preparationLabel: UiText,
    val energyLabel: UiText,
    val ageBandLabels: List<UiText>,
    val isPremium: Boolean,
    val packTitle: String?,
    val instructions: List<String>,
    val materials: List<String>,
    val hints: List<String>,
    val safetyNote: String?,
)

package com.togetherly.feature.explore.model

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText
import com.togetherly.domain.quest.QuestId
import com.togetherly.feature.today.model.QuestCategoryUi

/**
 * What an Explore quest card actually renders — deliberately not
 * [com.togetherly.feature.today.model.QuestCardUi]: that type has no [isPremium]/[locked] fields
 * (Today never shows locked content, so it never needed them), and is `internal` to Today's own
 * package. Explore reuses the *mapper functions* that build labels/colors
 * ([com.togetherly.feature.today.mapper.label], [com.togetherly.feature.today.presentation.color])
 * rather than the card type itself — see [com.togetherly.feature.explore.mapper.toExploreQuestUi].
 *
 * No completion-state field: per-quest completion history across the whole catalogue (as opposed
 * to Today's single "quest chosen for today") has no existing use case to source it from, and
 * adding one is out of this step's scope — see this feature's own final report for that decision.
 */
@Immutable
data class ExploreQuestUiModel(
    val id: QuestId,
    val title: String,
    val summary: String,
    val category: QuestCategoryUi,
    val durationLabel: UiText,
    val energyLabel: UiText,
    val locationLabel: UiText,
    val isSaved: Boolean,
    val isPremium: Boolean,
    val locked: Boolean,
)

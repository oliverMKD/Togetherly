package com.togetherly.feature.journey.model

import androidx.compose.runtime.Immutable
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.journey.StarPosition
import com.togetherly.domain.journey.StarVisualVariant
import com.togetherly.feature.today.model.QuestCategoryUi

/**
 * [position]/[visualVariant] are reused directly from [com.togetherly.domain.journey.JourneyStar] —
 * both are already plain, UI-framework-free values (normalized floats, a small enum), so mapping
 * them onto a UI-only equivalent would just be a rename with no benefit. [category] is mapped onto
 * [QuestCategoryUi] rather than the domain enum, matching every other Ui model in this app.
 */
@Immutable
data class JourneyStarUi(
    val completionId: CompletionId,
    val position: StarPosition,
    val visualVariant: StarVisualVariant,
    val category: QuestCategoryUi?,
    val hasNote: Boolean,
    val hasPhoto: Boolean,
    val hasVoice: Boolean,
)

package com.togetherly.domain.quest

import com.togetherly.domain.validation.requireNonNegative
import com.togetherly.domain.validation.requireNotEmpty
import com.togetherly.domain.validation.requireUniqueValues

data class QuestPack(
    val id: QuestPackId,
    val title: QuestTitle,
    val description: QuestSummary,
    val category: QuestCategory?,
    val access: QuestAccess,
    val questIds: List<QuestId>,
    val artworkKey: ArtworkKey,
    val sortOrder: Int,
) {
    init {
        requireNotEmpty(questIds)
        requireUniqueValues(questIds)
        requireNonNegative(sortOrder)
    }
}

package com.togetherly.domain.daily

import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.validation.requireNonNegative
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * [localDate] is the daily identity key — one selection exists per family-local date.
 * [selectedAt] is the precise moment selection happened and is not itself the identity.
 *
 * The first automatic selection for a date uses [selectionIndex] 0; each reroll for that same
 * date increases it. That sequencing is a property of the *sequence* of selections, not of a
 * single instance, so it's a caller/orchestration responsibility rather than something this
 * model can check on its own — only non-negativity is enforced here.
 */
data class DailyQuest(
    val questId: QuestId,
    val localDate: LocalDate,
    val selectionIndex: Int,
    val selectedAt: Instant,
    val source: DailyQuestSource,
    val context: QuestContext,
) {
    init {
        requireNonNegative(selectionIndex)
    }
}

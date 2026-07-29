package com.togetherly.domain.journey

import com.togetherly.domain.completion.QuestCompletion
import com.togetherly.domain.quest.FamilyQuest
import kotlin.time.Instant

/**
 * [quest] is null when the catalogue entry can't be resolved — content removed, catalogue data
 * unavailable, or historical content that no longer exists. The completion itself must remain
 * visible regardless: a family's memory is never hidden just because the quest content behind
 * it is gone.
 */
data class JourneyEntry(
    val completion: QuestCompletion,
    val quest: FamilyQuest?,
) {
    val completedAt: Instant
        get() = completion.completedAt
}

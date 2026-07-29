package com.togetherly.domain.completion

import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.validation.requirePositive
import kotlin.time.Instant

/**
 * Represents an activity that started but has not been completed yet.
 */
data class ActiveQuestSession(
    val completionId: CompletionId,
    val familyId: FamilyId,
    val questId: QuestId,
    val questVersion: Int,
    val startedAt: Instant,
) {
    init {
        requirePositive(questVersion)
    }
}

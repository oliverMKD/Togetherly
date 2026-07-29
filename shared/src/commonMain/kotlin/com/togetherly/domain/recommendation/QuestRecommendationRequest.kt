package com.togetherly.domain.recommendation

import com.togetherly.domain.daily.QuestContext
import com.togetherly.domain.family.FamilyProfile
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.validation.requireNonNegative
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * [availableQuests] may be empty (an empty catalogue is a valid, if unfortunate, input — see
 * [NoRecommendationReason.EMPTY_CATALOGUE]) and [history] may be entirely empty (a brand-new
 * family). [context] fields are independently nullable — see [QuestContext]'s own KDoc.
 *
 * [localDate] and [selectionIndex] feed the deterministic tie-break key (see
 * [DeterministicQuestRecommendationPolicy]'s own KDoc) — they are never used to compute elapsed
 * time. [now] is the one and only source of "the current moment" the policy is allowed to use (for
 * cooldown/dismissal-window math): the policy itself never calls the system clock, so callers that
 * need that math supply it explicitly here instead.
 */
data class QuestRecommendationRequest(
    val familyProfile: FamilyProfile,
    val context: QuestContext,
    val availableQuests: List<FamilyQuest>,
    val history: RecommendationHistory,
    val localDate: LocalDate,
    val selectionIndex: Int,
    val now: Instant,
) {
    init {
        requireNonNegative(selectionIndex)
    }
}

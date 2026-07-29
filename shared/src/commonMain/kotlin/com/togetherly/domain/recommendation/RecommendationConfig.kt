package com.togetherly.domain.recommendation

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Every score weight [DeterministicQuestRecommendationPolicy] uses, plus [dismissalCooldown] — one
 * object so none of these values are scattered as magic numbers through the scoring/filtering
 * code, and so a test (or a future tuning pass) can construct a [RecommendationConfig] with
 * different values without touching the policy's own logic.
 *
 * Weight-to-[RecommendationReason] mapping:
 * - [matchesFamilyInterest] → [RecommendationReason.MatchesFamilyInterest]
 * - [matchesPreferredDuration] → [RecommendationReason.MatchesPreferredDuration]
 * - [addsCategoryVariety] → [RecommendationReason.AddsCategoryVariety]
 * - [newToFamily] → [RecommendationReason.NewToFamily]
 * - [matchesLocationPreference] → [RecommendationReason.MatchesLocationPreference]
 * - [matchesPreparationPreference] → [RecommendationReason.MatchesPreparationPreference]
 * - [matchesPreferredEnergy] → [RecommendationReason.MatchesPreferredEnergy]
 * - [matchesRequestedContext] → [RecommendationReason.MatchesRequestedContext]
 * - [recentCategoryRepeatPenalty] → no reason (a penalty, not a positive factor to explain)
 *
 * [dismissalCooldown] is *not* the same window a caller might use when querying
 * [com.togetherly.domain.daily.repository.DailyQuestRepository.getRecentDismissals] — that query
 * window should be at least this long (see [com.togetherly.domain.daily.usecase.GetOrSelectDailyQuest]),
 * but the *behavioral* cutoff (is this specific dismissal still active) is always this value,
 * checked against [QuestRecommendationRequest.now].
 */
data class RecommendationConfig(
    val matchesFamilyInterest: Int = 30,
    val matchesPreferredDuration: Int = 25,
    val addsCategoryVariety: Int = 20,
    val newToFamily: Int = 15,
    val matchesLocationPreference: Int = 10,
    val matchesPreparationPreference: Int = 10,
    val matchesPreferredEnergy: Int = 10,
    val matchesRequestedContext: Int = 10,
    val recentCategoryRepeatPenalty: Int = -15,
    val dismissalCooldown: Duration = 30.days,
) {
    companion object {
        val DEFAULT = RecommendationConfig()
    }
}

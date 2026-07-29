package com.togetherly.domain.recommendation

import com.togetherly.domain.quest.FamilyQuest

/**
 * Every positive factor that contributed to [QuestRecommendationResult.Success.score] — used to
 * explain a recommendation (e.g. in a future "why this quest" UI), never a user-facing string
 * itself (see that type's own KDoc for why). [MatchesLocationPreference] and
 * [MatchesPreparationPreference] exist alongside [MatchesRequestedContext] because the *normal*
 * (non-explicit) family preference for those two dimensions is scored separately from an
 * *explicit* per-request ask — see [RecommendationConfig]'s own KDoc for the full scoring
 * breakdown these reasons correspond to.
 *
 * [LeastRecentlyCompleted] carries no independent score weight of its own — it's the positive
 * framing of the [RecommendationConfig.recentCategoryRepeatPenalty] penalty *not* applying: a
 * candidate that has been completed before, but not as one of the two most recent completions,
 * gets this reason attached purely for explainability. A never-completed candidate gets
 * [NewToFamily] instead, never both.
 */
sealed interface RecommendationReason {
    data object MatchesFamilyInterest : RecommendationReason
    data object MatchesPreferredDuration : RecommendationReason
    data object MatchesRequestedContext : RecommendationReason
    data object NewToFamily : RecommendationReason
    data object AddsCategoryVariety : RecommendationReason
    data object LeastRecentlyCompleted : RecommendationReason
    data object MatchesLocationPreference : RecommendationReason
    data object MatchesPreparationPreference : RecommendationReason

    /**
     * (Step 13.3) Scored the same way as [MatchesLocationPreference] — a normal, non-explicit
     * family preference bonus, only when [com.togetherly.domain.family.FamilyProfile.preferredEnergyLevels]
     * is non-empty and contains the quest's own [com.togetherly.domain.quest.EnergyLevel]. An empty
     * preference set (the default — "no energy preference set yet") never contributes this reason
     * for any quest, and — unlike preparation — energy is never a hard filter, so it can never make
     * every recommendation impossible.
     */
    data object MatchesPreferredEnergy : RecommendationReason
}

/**
 * Why no quest could be recommended — always one specific, typed reason, matching whichever stage
 * of [DeterministicQuestRecommendationPolicy]'s filtering pipeline emptied the candidate pool
 * first. Never a user-facing string; a presentation layer maps this to copy, the same way
 * [com.togetherly.core.error.AppError] maps to [com.togetherly.core.ui.UiText].
 */
enum class NoRecommendationReason {
    EMPTY_CATALOGUE,
    NO_AGE_COMPATIBLE_QUEST,
    NO_CONTEXT_COMPATIBLE_QUEST,
    ALL_CANDIDATES_IN_COOLDOWN,
}

sealed interface QuestRecommendationResult {

    data class Success(
        val quest: FamilyQuest,
        val score: Int,
        val reasons: List<RecommendationReason>,
    ) : QuestRecommendationResult

    data class NoMatch(
        val reason: NoRecommendationReason,
    ) : QuestRecommendationResult
}

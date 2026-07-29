package com.togetherly.domain.recommendation

/**
 * The contract a quest-recommendation implementation must satisfy — pure, deterministic, and
 * synchronous: no I/O, no coroutines, no system clock (see [QuestRecommendationRequest.now]'s own
 * KDoc). [DeterministicQuestRecommendationPolicy] is the production implementation.
 */
interface QuestRecommendationPolicy {
    fun recommend(
        request: QuestRecommendationRequest,
    ): QuestRecommendationResult
}

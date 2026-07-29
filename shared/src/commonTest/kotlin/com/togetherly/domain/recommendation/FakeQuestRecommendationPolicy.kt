package com.togetherly.domain.recommendation

class FakeQuestRecommendationPolicy(
    var result: QuestRecommendationResult = QuestRecommendationResult.NoMatch(NoRecommendationReason.EMPTY_CATALOGUE),
) : QuestRecommendationPolicy {

    val requests: MutableList<QuestRecommendationRequest> = mutableListOf()

    override fun recommend(request: QuestRecommendationRequest): QuestRecommendationResult {
        requests += request
        return result
    }
}

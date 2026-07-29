package com.togetherly.domain.recommendation

import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError

/**
 * Shared by [com.togetherly.domain.daily.usecase.GetOrSelectDailyQuest],
 * [com.togetherly.domain.daily.usecase.SelectDailyQuestForContext] and
 * [com.togetherly.domain.daily.usecase.RerollDailyQuest] so a no-match reason maps onto the same
 * typed [AppError] regardless of which use case hit it. Deliberately stops at a typed
 * [AppError.Content] — presentation-layer copy selection per reason (Step 8.5) belongs in a UI
 * mapper, never here.
 */
fun NoRecommendationReason.toAppError(): AppError.Content = AppError.Content(
    when (this) {
        NoRecommendationReason.EMPTY_CATALOGUE -> ContentError.NO_QUEST_AVAILABLE
        NoRecommendationReason.NO_AGE_COMPATIBLE_QUEST -> ContentError.NO_AGE_COMPATIBLE_QUEST
        NoRecommendationReason.NO_CONTEXT_COMPATIBLE_QUEST -> ContentError.NO_CONTEXT_COMPATIBLE_QUEST
        NoRecommendationReason.ALL_CANDIDATES_IN_COOLDOWN -> ContentError.ALL_CANDIDATES_IN_COOLDOWN
    },
)

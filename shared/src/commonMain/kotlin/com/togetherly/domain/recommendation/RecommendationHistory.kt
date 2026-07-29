package com.togetherly.domain.recommendation

import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import kotlin.time.Instant

/**
 * One past occurrence (a completion or a dismissal) reduced to exactly what the recommendation
 * policy needs — never a full [com.togetherly.domain.completion.QuestCompletion] or
 * [com.togetherly.domain.daily.DismissedQuest]. [category] is nullable because
 * [com.togetherly.domain.completion.QuestCompletion] only carries a [QuestId] — resolving its
 * category requires looking the quest up in the catalogue, which may no longer contain it (a
 * retired quest); the coordinating use case does that lookup once when building history, not the
 * policy.
 */
data class QuestHistoryEntry(
    val questId: QuestId,
    val category: QuestCategory?,
    val occurredAt: Instant,
)

/**
 * Everything the policy needs to know about the past, pre-computed by the coordinating use case
 * (see [com.togetherly.domain.daily.usecase.GetOrSelectDailyQuest] for where [recentlyCompleted]/
 * [recentlyDismissed] are built from [com.togetherly.domain.completion.repository.CompletionRepository]/
 * [com.togetherly.domain.daily.repository.DailyQuestRepository] and category is resolved against
 * the catalogue). All three fields may be empty — a brand-new family has no history at all.
 *
 * [categoryCompletionCounts] is supplied separately from [recentlyCompleted] rather than derived
 * from it inside the policy: how far back "the history used to count category variety" reaches is
 * a use-case-level decision (today, the same full completion list backs both), not something the
 * policy should assume.
 */
data class RecommendationHistory(
    val recentlyCompleted: List<QuestHistoryEntry>,
    val recentlyDismissed: List<QuestHistoryEntry>,
    val categoryCompletionCounts: Map<QuestCategory, Int>,
) {
    companion object {
        val EMPTY = RecommendationHistory(
            recentlyCompleted = emptyList(),
            recentlyDismissed = emptyList(),
            categoryCompletionCounts = emptyMap(),
        )
    }
}

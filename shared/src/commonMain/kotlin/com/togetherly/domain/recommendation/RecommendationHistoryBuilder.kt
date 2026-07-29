package com.togetherly.domain.recommendation

import com.togetherly.core.datetime.AppClock
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.repository.CompletionRepository
import com.togetherly.domain.daily.repository.DailyQuestRepository
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId

/**
 * Builds a [RecommendationHistory] from the real repositories — the one place completion/dismissal
 * category resolution against the catalogue happens (see [QuestHistoryEntry]'s own KDoc for why
 * that lookup can't live inside the policy itself), shared by
 * [com.togetherly.domain.daily.usecase.GetOrSelectDailyQuest] and
 * [com.togetherly.domain.daily.usecase.RerollDailyQuest] so the same logic isn't duplicated
 * between them.
 *
 * The [DailyQuestRepository.getRecentDismissals] query window is [RecommendationConfig.dismissalCooldown]
 * itself — the same value the policy uses to decide whether a given dismissal is still active —
 * so there is exactly one place `30.days` (or whatever it's configured to) is defined, never a
 * second, independently-drifting window at the query layer.
 */
class RecommendationHistoryBuilder(
    private val completionRepository: CompletionRepository,
    private val dailyQuestRepository: DailyQuestRepository,
    private val clock: AppClock,
    private val config: RecommendationConfig,
) {
    suspend fun build(catalogue: List<FamilyQuest>): DataResult<RecommendationHistory> {
        val completionsResult = completionRepository.getCompletions()
        if (completionsResult is DataResult.Error) return completionsResult
        val completions = (completionsResult as DataResult.Success).value

        val dismissalsResult = dailyQuestRepository.getRecentDismissals(
            since = clock.now() - config.dismissalCooldown,
        )
        if (dismissalsResult is DataResult.Error) return dismissalsResult
        val dismissals = (dismissalsResult as DataResult.Success).value

        fun categoryOf(questId: QuestId): QuestCategory? = catalogue.find { it.id == questId }?.category

        val recentlyCompleted = completions.map { completion ->
            QuestHistoryEntry(
                questId = completion.questId,
                category = categoryOf(completion.questId),
                occurredAt = completion.completedAt,
            )
        }
        val recentlyDismissed = dismissals.map { dismissal ->
            QuestHistoryEntry(
                questId = dismissal.questId,
                category = categoryOf(dismissal.questId),
                occurredAt = dismissal.dismissedAt,
            )
        }
        val categoryCompletionCounts = recentlyCompleted
            .mapNotNull { it.category }
            .groupingBy { it }
            .eachCount()

        return DataResult.Success(
            RecommendationHistory(
                recentlyCompleted = recentlyCompleted,
                recentlyDismissed = recentlyDismissed,
                categoryCompletionCounts = categoryCompletionCounts,
            ),
        )
    }
}

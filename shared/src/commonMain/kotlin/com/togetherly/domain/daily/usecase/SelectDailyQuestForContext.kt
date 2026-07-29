package com.togetherly.domain.daily.usecase

import com.togetherly.core.datetime.AppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.daily.DailyQuest
import com.togetherly.domain.daily.DailyQuestSource
import com.togetherly.domain.daily.DismissedQuest
import com.togetherly.domain.daily.QuestContext
import com.togetherly.domain.daily.RerollAllowancePolicy
import com.togetherly.domain.daily.repository.DailyQuestRepository
import com.togetherly.domain.daily.repository.DailyQuestTransaction
import com.togetherly.domain.family.repository.FamilyRepository
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.EntitlementRepository
import com.togetherly.domain.quest.repository.QuestRepository
import com.togetherly.domain.recommendation.QuestRecommendationPolicy
import com.togetherly.domain.recommendation.QuestRecommendationRequest
import com.togetherly.domain.recommendation.QuestRecommendationResult
import com.togetherly.domain.recommendation.RecommendationHistoryBuilder
import com.togetherly.domain.recommendation.toAppError
import com.togetherly.domain.validation.DomainValidationException
import kotlinx.datetime.TimeZone

/**
 * The explicit, confirmation-gated counterpart to [GetOrSelectDailyQuest] noticing a context
 * mismatch on its own — this only runs when the user taps "Find a quest" after editing filter
 * chips, never as a side effect of every chip tap (filter chips only ever mutate presentation-layer
 * draft state; see the Today feature's own filter-draft model). If [context] is unchanged from
 * today's current selection, this is a no-op that returns the current quest — no policy call, no
 * dismissal, no write.
 *
 * Does not consume [RerollAllowance][com.togetherly.domain.daily.RerollAllowance] — the resulting
 * [DailyQuest.selectionIndex] deliberately stays the same as the quest it replaced, since applying
 * a filter is a distinct action from rerolling, and only rerolling counts against the free-tier
 * limit (see [RerollAllowancePolicy]'s own KDoc for why [DailyQuest.selectionIndex] is what that
 * policy reads).
 *
 * The dismiss-and-replace write reuses [DailyQuestTransaction] — the same atomic boundary
 * [RerollDailyQuest] uses — because both operations are the same thing at the storage layer:
 * record the outgoing quest as dismissed and persist a new selection together, or not at all.
 */
class SelectDailyQuestForContext(
    private val familyRepository: FamilyRepository,
    private val questRepository: QuestRepository,
    private val dailyQuestRepository: DailyQuestRepository,
    private val dailyQuestTransaction: DailyQuestTransaction,
    private val recommendationPolicy: QuestRecommendationPolicy,
    private val recommendationHistoryBuilder: RecommendationHistoryBuilder,
    private val rerollAllowancePolicy: RerollAllowancePolicy,
    private val entitlementRepository: EntitlementRepository,
    private val questAccessPolicy: QuestAccessPolicy,
    private val clock: AppClock,
) {
    suspend operator fun invoke(
        context: QuestContext,
        timeZone: TimeZone,
    ): DataResult<ResolvedDailyQuest> {
        val today = clock.today(timeZone)
        val hasFamilyPlus = hasFamilyPlus()

        val currentResult = dailyQuestRepository.getToday(today)
        if (currentResult is DataResult.Error) return currentResult
        val current = (currentResult as DataResult.Success).value
            ?: return DataResult.Error(AppError.Validation(ValidationError.NO_DAILY_SELECTION))

        if (current.context == context) {
            return resolveCurrent(current, hasFamilyPlus)
        }

        val profileResult = familyRepository.getProfile()
        if (profileResult is DataResult.Error) return profileResult
        val profile = (profileResult as DataResult.Success).value
            ?: return DataResult.Error(AppError.Validation(ValidationError.MISSING_FAMILY_PROFILE))

        val questsResult = questRepository.getAllQuests()
        if (questsResult is DataResult.Error) return questsResult
        val quests = (questsResult as DataResult.Success).value

        val historyResult = recommendationHistoryBuilder.build(quests)
        if (historyResult is DataResult.Error) return historyResult
        val history = (historyResult as DataResult.Success).value

        val recommendation = recommendationPolicy.recommend(
            QuestRecommendationRequest(
                familyProfile = profile,
                context = context,
                availableQuests = quests,
                history = history,
                localDate = today,
                selectionIndex = current.selectionIndex,
                now = clock.now(),
            ),
        )

        val selectedQuest = when (recommendation) {
            is QuestRecommendationResult.Success -> recommendation.quest
            is QuestRecommendationResult.NoMatch -> return DataResult.Error(recommendation.reason.toAppError())
        }

        val dismissal = try {
            DismissedQuest(questId = current.questId, dismissedAt = clock.now(), localDate = today)
        } catch (e: DomainValidationException) {
            return DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))
        }

        val replacement = try {
            DailyQuest(
                questId = selectedQuest.id,
                localDate = today,
                selectionIndex = current.selectionIndex,
                selectedAt = clock.now(),
                source = DailyQuestSource.CONTEXTUAL,
                context = context,
            )
        } catch (e: DomainValidationException) {
            return DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))
        }

        val replaceResult = dailyQuestTransaction.replaceWithReroll(dismissal, replacement)
        if (replaceResult is DataResult.Error) return replaceResult

        return DataResult.Success(
            ResolvedDailyQuest(
                dailyQuest = replacement,
                quest = selectedQuest,
                rerollAllowance = rerollAllowancePolicy.allowance(
                    selectionIndex = replacement.selectionIndex,
                    hasFamilyPlus = hasFamilyPlus,
                ),
            ),
        )
    }

    private suspend fun resolveCurrent(dailyQuest: DailyQuest, hasFamilyPlus: Boolean): DataResult<ResolvedDailyQuest> {
        val questResult = questRepository.getQuest(dailyQuest.questId)
        if (questResult is DataResult.Error) return questResult
        val quest = (questResult as DataResult.Success).value
            ?: return DataResult.Error(AppError.Content(ContentError.QUEST_NOT_FOUND))
        return DataResult.Success(
            ResolvedDailyQuest(
                dailyQuest = dailyQuest,
                quest = quest,
                rerollAllowance = rerollAllowancePolicy.allowance(
                    selectionIndex = dailyQuest.selectionIndex,
                    hasFamilyPlus = hasFamilyPlus,
                ),
            ),
        )
    }

    private suspend fun hasFamilyPlus(): Boolean {
        val access = entitlementRepository.getAccess()
        if (access !is DataResult.Success) return false
        return questAccessPolicy.isFamilyPlusActive(access.value, clock.now())
    }
}

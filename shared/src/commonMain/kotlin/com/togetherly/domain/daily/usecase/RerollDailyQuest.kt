package com.togetherly.domain.daily.usecase

import com.togetherly.core.datetime.AppClock
import com.togetherly.core.error.AppError
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
import com.togetherly.domain.recommendation.QuestHistoryEntry
import com.togetherly.domain.recommendation.QuestRecommendationPolicy
import com.togetherly.domain.recommendation.QuestRecommendationRequest
import com.togetherly.domain.recommendation.QuestRecommendationResult
import com.togetherly.domain.recommendation.RecommendationHistoryBuilder
import com.togetherly.domain.recommendation.toAppError
import com.togetherly.domain.validation.DomainValidationException
import kotlinx.datetime.TimeZone

private val EMPTY_CONTEXT = QuestContext(null, null, null, null, null)

/**
 * No reroll limit is enforced by this class alone — it delegates entirely to
 * [rerollAllowancePolicy] and returns [ValidationError.REROLL_LIMIT_REACHED] once the allowance is
 * exhausted, rather than hardcoding a count here (see [RerollAllowancePolicy]'s own KDoc for why
 * that has to stay injectable).
 *
 * The current quest's dismissal is **not** recorded until a replacement has actually been chosen:
 * recommending first and writing only once a replacement exists is what guarantees "if
 * recommendation fails, the current quest is left completely untouched and no reroll is consumed."
 * To still exclude the current quest from the candidate pool *before* it's actually dismissed, a
 * synthetic [QuestHistoryEntry] for it (dated `now`) is added to the built history's
 * `recentlyDismissed` just for this recommendation request — the policy then excludes it via its
 * normal dismissal-cooldown filter, with nothing written to storage unless a replacement is found.
 * Once one is, the real dismissal and the new selection are written together via
 * [DailyQuestTransaction], atomically.
 */
class RerollDailyQuest(
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
        timeZone: TimeZone,
        context: QuestContext = EMPTY_CONTEXT,
    ): DataResult<ResolvedDailyQuest> {
        val today = clock.today(timeZone)
        val hasFamilyPlus = hasFamilyPlus()

        val currentResult = dailyQuestRepository.getToday(today)
        if (currentResult is DataResult.Error) return currentResult
        val current = (currentResult as DataResult.Success).value
            ?: return DataResult.Error(AppError.Validation(ValidationError.NO_DAILY_SELECTION))

        val allowance = rerollAllowancePolicy.allowance(selectionIndex = current.selectionIndex, hasFamilyPlus = hasFamilyPlus)
        if (!allowance.canReroll) {
            return DataResult.Error(AppError.Validation(ValidationError.REROLL_LIMIT_REACHED))
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
        val historyExcludingCurrent = history.copy(
            recentlyDismissed = history.recentlyDismissed + QuestHistoryEntry(
                questId = current.questId,
                category = quests.find { it.id == current.questId }?.category,
                occurredAt = clock.now(),
            ),
        )

        val nextSelectionIndex = current.selectionIndex + 1

        val recommendation = recommendationPolicy.recommend(
            QuestRecommendationRequest(
                familyProfile = profile,
                context = context,
                availableQuests = quests,
                history = historyExcludingCurrent,
                localDate = today,
                selectionIndex = nextSelectionIndex,
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
                selectionIndex = nextSelectionIndex,
                selectedAt = clock.now(),
                source = DailyQuestSource.REROLL,
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
                    selectionIndex = nextSelectionIndex,
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

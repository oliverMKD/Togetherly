package com.togetherly.domain.daily.usecase

import com.togetherly.core.datetime.AppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.daily.DailyQuest
import com.togetherly.domain.daily.DailyQuestSource
import com.togetherly.domain.daily.QuestContext
import com.togetherly.domain.daily.repository.DailyQuestRepository
import com.togetherly.domain.family.repository.FamilyRepository
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.EntitlementRepository
import com.togetherly.domain.quest.repository.QuestRepository
import com.togetherly.domain.recommendation.QuestRecommendationPolicy
import com.togetherly.domain.recommendation.QuestRecommendationRequest
import com.togetherly.domain.recommendation.QuestRecommendationResult
import com.togetherly.domain.recommendation.RecommendationHistoryBuilder
import com.togetherly.domain.recommendation.toAppError
import com.togetherly.domain.daily.RerollAllowancePolicy
import com.togetherly.domain.validation.DomainValidationException
import kotlinx.datetime.TimeZone

/**
 * Never hides a failure behind fallback sample data — every branch either returns the resolved
 * selection or a typed [com.togetherly.core.error.AppError].
 *
 * Never replaces an existing selection just because [context] differs from whatever produced it —
 * [context] is only consulted the moment a *new* selection is made (no selection exists yet for
 * today). Deliberately re-selecting because of a changed context is
 * [SelectDailyQuestForContext]'s job, not this one's — see its own KDoc for why that has to be an
 * explicit, separate call rather than a side effect of this one noticing a mismatch.
 *
 * [RecommendationHistoryBuilder] stands in for a direct [com.togetherly.domain.completion.repository.CompletionRepository]
 * dependency here — it already wraps that repository (plus [DailyQuestRepository]) to build
 * [com.togetherly.domain.recommendation.RecommendationHistory] once, shared with
 * [RerollDailyQuest], instead of duplicating that assembly in both use cases.
 *
 * [entitlementRepository]/[questAccessPolicy] resolve `hasFamilyPlus` for [rerollAllowancePolicy]
 * fresh on every call — never cached on this class — so a family's entitlement change between one
 * call and the next is always reflected.
 */
class GetOrSelectDailyQuest(
    private val familyRepository: FamilyRepository,
    private val questRepository: QuestRepository,
    private val dailyQuestRepository: DailyQuestRepository,
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

        val existingResult = dailyQuestRepository.getToday(today)
        if (existingResult is DataResult.Error) return existingResult
        val existing = (existingResult as DataResult.Success).value
        if (existing != null) {
            return resolveQuest(existing, hasFamilyPlus)
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
                selectionIndex = 0,
                now = clock.now(),
            ),
        )

        val selectedQuest = when (recommendation) {
            is QuestRecommendationResult.Success -> recommendation.quest
            is QuestRecommendationResult.NoMatch -> return DataResult.Error(recommendation.reason.toAppError())
        }

        val dailyQuest = try {
            DailyQuest(
                questId = selectedQuest.id,
                localDate = today,
                selectionIndex = 0,
                selectedAt = clock.now(),
                source = if (context == EMPTY_CONTEXT) DailyQuestSource.AUTOMATIC else DailyQuestSource.CONTEXTUAL,
                context = context,
            )
        } catch (e: DomainValidationException) {
            return DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))
        }

        val saveResult = dailyQuestRepository.saveDailyQuest(dailyQuest)
        if (saveResult is DataResult.Error) return saveResult

        return DataResult.Success(
            ResolvedDailyQuest(
                dailyQuest = dailyQuest,
                quest = selectedQuest,
                rerollAllowance = rerollAllowancePolicy.allowance(selectionIndex = 0, hasFamilyPlus = hasFamilyPlus),
            ),
        )
    }

    private suspend fun resolveQuest(dailyQuest: DailyQuest, hasFamilyPlus: Boolean): DataResult<ResolvedDailyQuest> {
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

    private companion object {
        val EMPTY_CONTEXT = QuestContext(null, null, null, null, null)
    }
}

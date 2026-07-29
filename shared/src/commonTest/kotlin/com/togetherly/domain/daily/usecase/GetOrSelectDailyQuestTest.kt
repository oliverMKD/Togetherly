package com.togetherly.domain.daily.usecase

import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.daily.DailyQuest
import com.togetherly.domain.daily.DailyQuestSource
import com.togetherly.domain.daily.FakeRerollAllowancePolicy
import com.togetherly.domain.daily.QuestContext
import com.togetherly.domain.daily.RerollAllowance
import com.togetherly.domain.daily.repository.FakeDailyQuestRepository
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.family.FamilyProfile
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.family.repository.FakeFamilyRepository
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.validFamilyQuest
import com.togetherly.domain.recommendation.FakeQuestRecommendationPolicy
import com.togetherly.domain.recommendation.NoRecommendationReason
import com.togetherly.domain.recommendation.QuestRecommendationResult
import com.togetherly.domain.recommendation.RecommendationConfig
import com.togetherly.domain.recommendation.RecommendationHistoryBuilder
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")
private val TODAY = LocalDate(2026, 6, 15)
private val EMPTY_CONTEXT = QuestContext(null, null, null, null, null)

private fun validProfile() = FamilyProfile(
    id = FamilyId("family-1"),
    displayName = null,
    childAgeBands = setOf(AgeBand.AGE_6_TO_8),
    interests = setOf(QuestCategory.CREATE),
    preferredDurations = setOf(DurationBand.TEN_MINUTES),
    locationPreference = LocationPreference.BOTH,
    preparationPreference = PreparationPreference.SIMPLE_MATERIALS,
    reminderPreference = null,
    createdAt = NOW,
    updatedAt = NOW,
)

private fun historyBuilder(
    dailyQuestRepository: FakeDailyQuestRepository,
    completionRepository: FakeCompletionRepository = FakeCompletionRepository(),
    clock: TestAppClock = TestAppClock(NOW),
) = RecommendationHistoryBuilder(
    completionRepository = completionRepository,
    dailyQuestRepository = dailyQuestRepository,
    clock = clock,
    config = RecommendationConfig.DEFAULT,
)

class GetOrSelectDailyQuestTest {

    @Test
    fun existingDailySelectionIsReused() = runTest {
        val familyRepository = FakeFamilyRepository()
        val questRepository = FakeQuestRepository()
        val dailyQuestRepository = FakeDailyQuestRepository()
        val policy = FakeQuestRecommendationPolicy()
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        questRepository.setQuests(listOf(quest))
        val existingDaily = DailyQuest(
            questId = quest.id,
            localDate = TODAY,
            selectionIndex = 0,
            selectedAt = NOW,
            source = DailyQuestSource.AUTOMATIC,
            context = EMPTY_CONTEXT,
        )
        dailyQuestRepository.saveDailyQuest(existingDaily)
        val useCase = GetOrSelectDailyQuest(
            familyRepository, questRepository, dailyQuestRepository, policy,
            historyBuilder(dailyQuestRepository), FakeRerollAllowancePolicy(), FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)), QuestAccessPolicy(), TestAppClock(NOW),
        )

        val result = useCase(context = EMPTY_CONTEXT, timeZone = TimeZone.UTC)

        val resolved = (result as DataResult.Success).value
        assertEquals(quest, resolved.quest)
        assertEquals(existingDaily, resolved.dailyQuest)
        assertEquals(0, policy.requests.size)
    }

    @Test
    fun existingQuestIsResolvedFromCatalogue() = runTest {
        val familyRepository = FakeFamilyRepository()
        val questRepository = FakeQuestRepository()
        val dailyQuestRepository = FakeDailyQuestRepository()
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        questRepository.setQuests(listOf(quest))
        val existingDaily = DailyQuest(
            questId = quest.id,
            localDate = TODAY,
            selectionIndex = 2,
            selectedAt = NOW,
            source = DailyQuestSource.REROLL,
            context = EMPTY_CONTEXT,
        )
        dailyQuestRepository.saveDailyQuest(existingDaily)
        val allowancePolicy = FakeRerollAllowancePolicy(result = RerollAllowance(used = 2, maximum = 1))
        val useCase = GetOrSelectDailyQuest(
            familyRepository, questRepository, dailyQuestRepository, FakeQuestRecommendationPolicy(),
            historyBuilder(dailyQuestRepository), allowancePolicy, FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)), QuestAccessPolicy(), TestAppClock(NOW),
        )

        val result = useCase(context = EMPTY_CONTEXT, timeZone = TimeZone.UTC)

        val resolved = (result as DataResult.Success).value
        assertEquals(quest, resolved.quest)
        assertEquals(RerollAllowance(used = 2, maximum = 1), resolved.rerollAllowance)
        assertEquals(listOf(2 to false), allowancePolicy.calls)
    }

    @Test
    fun missingPersistedQuestReturnsTypedContentError() = runTest {
        val familyRepository = FakeFamilyRepository()
        val questRepository = FakeQuestRepository()
        val dailyQuestRepository = FakeDailyQuestRepository()
        dailyQuestRepository.saveDailyQuest(
            DailyQuest(
                questId = QuestId("does-not-exist"),
                localDate = TODAY,
                selectionIndex = 0,
                selectedAt = NOW,
                source = DailyQuestSource.AUTOMATIC,
                context = EMPTY_CONTEXT,
            ),
        )
        val useCase = GetOrSelectDailyQuest(
            familyRepository, questRepository, dailyQuestRepository, FakeQuestRecommendationPolicy(),
            historyBuilder(dailyQuestRepository), FakeRerollAllowancePolicy(), FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)), QuestAccessPolicy(), TestAppClock(NOW),
        )

        val result = useCase(context = EMPTY_CONTEXT, timeZone = TimeZone.UTC)

        assertEquals(DataResult.Error(AppError.Content(ContentError.QUEST_NOT_FOUND)), result)
    }

    @Test
    fun missingFamilyProfileReturnsTypedError() = runTest {
        val familyRepository = FakeFamilyRepository()
        val questRepository = FakeQuestRepository()
        val dailyQuestRepository = FakeDailyQuestRepository()
        val policy = FakeQuestRecommendationPolicy()
        val useCase = GetOrSelectDailyQuest(
            familyRepository, questRepository, dailyQuestRepository, policy,
            historyBuilder(dailyQuestRepository), FakeRerollAllowancePolicy(), FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)), QuestAccessPolicy(), TestAppClock(NOW),
        )

        val result = useCase(context = EMPTY_CONTEXT, timeZone = TimeZone.UTC)

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.MISSING_FAMILY_PROFILE)), result)
    }

    @Test
    fun newDailySelectionIsSavedWithIndexZero() = runTest {
        val familyRepository = FakeFamilyRepository()
        familyRepository.saveProfile(validProfile())
        val questRepository = FakeQuestRepository()
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        questRepository.setQuests(listOf(quest))
        val dailyQuestRepository = FakeDailyQuestRepository()
        val policy = FakeQuestRecommendationPolicy(result = QuestRecommendationResult.Success(quest, score = 10, reasons = emptyList()))
        val allowancePolicy = FakeRerollAllowancePolicy()
        val useCase = GetOrSelectDailyQuest(
            familyRepository, questRepository, dailyQuestRepository, policy,
            historyBuilder(dailyQuestRepository), allowancePolicy, FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)), QuestAccessPolicy(), TestAppClock(NOW),
        )

        val result = useCase(context = EMPTY_CONTEXT, timeZone = TimeZone.UTC)

        val resolved = (result as DataResult.Success).value
        assertEquals(quest, resolved.quest)
        assertEquals(0, resolved.dailyQuest.selectionIndex)
        assertEquals(DataResult.Success(resolved.dailyQuest), dailyQuestRepository.getToday(TODAY))
        assertEquals(listOf(0 to false), allowancePolicy.calls)
    }

    @Test
    fun recommendationFailureIsMappedToTypedError() = runTest {
        val familyRepository = FakeFamilyRepository()
        familyRepository.saveProfile(validProfile())
        val questRepository = FakeQuestRepository()
        val dailyQuestRepository = FakeDailyQuestRepository()
        val policy = FakeQuestRecommendationPolicy(
            result = QuestRecommendationResult.NoMatch(NoRecommendationReason.NO_CONTEXT_COMPATIBLE_QUEST),
        )
        val useCase = GetOrSelectDailyQuest(
            familyRepository, questRepository, dailyQuestRepository, policy,
            historyBuilder(dailyQuestRepository), FakeRerollAllowancePolicy(), FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)), QuestAccessPolicy(), TestAppClock(NOW),
        )

        val result = useCase(context = EMPTY_CONTEXT, timeZone = TimeZone.UTC)

        assertEquals(DataResult.Error(AppError.Content(ContentError.NO_CONTEXT_COMPATIBLE_QUEST)), result)
    }

    @Test
    fun historyIncludesCompletionsAndDismissalsSeenByThePolicy() = runTest {
        val familyRepository = FakeFamilyRepository()
        familyRepository.saveProfile(validProfile())
        val questRepository = FakeQuestRepository()
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val completedQuest = validFamilyQuest(id = QuestId("quest-2"))
        questRepository.setQuests(listOf(quest, completedQuest))
        val dailyQuestRepository = FakeDailyQuestRepository()
        dailyQuestRepository.recordDismissal(
            com.togetherly.domain.daily.DismissedQuest(questId = QuestId("quest-3"), dismissedAt = NOW, localDate = TODAY),
        )
        val completionRepository = FakeCompletionRepository()
        completionRepository.saveCompletion(
            com.togetherly.domain.completion.validQuestCompletion(questId = completedQuest.id, completedAt = NOW),
        )
        val policy = FakeQuestRecommendationPolicy(result = QuestRecommendationResult.Success(quest, score = 10, reasons = emptyList()))
        val useCase = GetOrSelectDailyQuest(
            familyRepository, questRepository, dailyQuestRepository, policy,
            historyBuilder(dailyQuestRepository, completionRepository), FakeRerollAllowancePolicy(), FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)), QuestAccessPolicy(), TestAppClock(NOW),
        )

        useCase(context = EMPTY_CONTEXT, timeZone = TimeZone.UTC)

        val seenHistory = policy.requests.single().history
        assertEquals(listOf(QuestId("quest-3")), seenHistory.recentlyDismissed.map { it.questId })
        assertEquals(listOf(completedQuest.id), seenHistory.recentlyCompleted.map { it.questId })
    }
}

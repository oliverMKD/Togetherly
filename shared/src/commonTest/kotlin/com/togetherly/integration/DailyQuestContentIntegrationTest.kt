package com.togetherly.integration

import com.togetherly.content.realBundledQuestRepository
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.daily.DailyQuest
import com.togetherly.domain.daily.DailyQuestSource
import com.togetherly.domain.daily.DismissedQuest
import com.togetherly.domain.daily.FakeRerollAllowancePolicy
import com.togetherly.domain.daily.QuestContext
import com.togetherly.domain.daily.repository.FakeDailyQuestRepository
import com.togetherly.domain.daily.repository.FakeDailyQuestTransaction
import com.togetherly.domain.daily.usecase.GetOrSelectDailyQuest
import com.togetherly.domain.daily.usecase.RerollDailyQuest
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.repository.FakeFamilyRepository
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.recommendation.FakeQuestRecommendationPolicy
import com.togetherly.domain.recommendation.QuestRecommendationResult
import com.togetherly.domain.recommendation.RecommendationConfig
import com.togetherly.domain.recommendation.RecommendationHistoryBuilder
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val EMPTY_CONTEXT = QuestContext(null, null, null, null, null)

private fun historyBuilder(
    dailyQuestRepository: FakeDailyQuestRepository,
    clock: TestAppClock,
) = RecommendationHistoryBuilder(
    completionRepository = FakeCompletionRepository(),
    dailyQuestRepository = dailyQuestRepository,
    clock = clock,
    config = RecommendationConfig.DEFAULT,
)

/**
 * Exercises the daily-selection use cases against the real bundled repository (21 real quests)
 * with fake family/daily-quest repositories. [FakeQuestRecommendationPolicy] stands in for
 * [com.togetherly.domain.recommendation.DeterministicQuestRecommendationPolicy] here — tests that
 * concern age-band eligibility or dismissal-awareness verify the *plumbing* a real policy relies
 * on, not the policy's own scoring logic, which is covered by its dedicated unit tests.
 */
class DailyQuestContentIntegrationTest {

    @Test
    fun aValidFamilyProfileCanReceiveAQuest() = runTest {
        val familyRepository = FakeFamilyRepository()
        familyRepository.saveProfile(testFamilyProfile())
        val questRepository = realBundledQuestRepository(testDispatchers())
        val realQuest = (questRepository.getAllQuests() as DataResult.Success).value.first()
        val policy = FakeQuestRecommendationPolicy(
            QuestRecommendationResult.Success(realQuest, score = 10, reasons = emptyList()),
        )
        val dailyQuestRepository = FakeDailyQuestRepository()
        val clock = TestAppClock(FIXED_NOW)
        val useCase = GetOrSelectDailyQuest(
            familyRepository,
            questRepository,
            dailyQuestRepository,
            policy,
            historyBuilder(dailyQuestRepository, clock),
            FakeRerollAllowancePolicy(),
            FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), FIXED_NOW)),
            QuestAccessPolicy(),
            clock,
        )

        val result = useCase(context = EMPTY_CONTEXT, timeZone = TimeZone.UTC)

        assertEquals(realQuest, (result as DataResult.Success).value.quest)
    }

    @Test
    fun everySelectedQuestSupportsAllFamilyAgeBands() = runTest {
        val familyAgeBands = setOf(AgeBand.AGE_6_TO_8, AgeBand.AGE_9_TO_11)
        val familyRepository = FakeFamilyRepository()
        familyRepository.saveProfile(testFamilyProfile(childAgeBands = familyAgeBands))
        val questRepository = realBundledQuestRepository(testDispatchers())
        val allQuests = (questRepository.getAllQuests() as DataResult.Success).value
        val eligibleQuest = allQuests.first { it.ageBands.containsAll(familyAgeBands) }
        val policy = FakeQuestRecommendationPolicy(
            QuestRecommendationResult.Success(eligibleQuest, score = 10, reasons = emptyList()),
        )
        val dailyQuestRepository = FakeDailyQuestRepository()
        val clock = TestAppClock(FIXED_NOW)
        val useCase = GetOrSelectDailyQuest(
            familyRepository,
            questRepository,
            dailyQuestRepository,
            policy,
            historyBuilder(dailyQuestRepository, clock),
            FakeRerollAllowancePolicy(),
            FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), FIXED_NOW)),
            QuestAccessPolicy(),
            clock,
        )

        val result = useCase(context = EMPTY_CONTEXT, timeZone = TimeZone.UTC)

        val selection = (result as DataResult.Success).value
        assertTrue(selection.quest.ageBands.containsAll(familyAgeBands))
    }

    @Test
    fun existingDailySelectionResolvesBackToItsQuest() = runTest {
        val questRepository = realBundledQuestRepository(testDispatchers())
        val realQuest = (questRepository.getAllQuests() as DataResult.Success).value.first()
        val clock = TestAppClock(FIXED_NOW)
        val today = clock.today(TimeZone.UTC)
        val dailyQuestRepository = FakeDailyQuestRepository()
        dailyQuestRepository.saveDailyQuest(
            DailyQuest(
                questId = realQuest.id,
                localDate = today,
                selectionIndex = 0,
                selectedAt = FIXED_NOW,
                source = DailyQuestSource.AUTOMATIC,
                context = EMPTY_CONTEXT,
            ),
        )
        val useCase = GetOrSelectDailyQuest(
            FakeFamilyRepository(),
            questRepository,
            dailyQuestRepository,
            FakeQuestRecommendationPolicy(),
            historyBuilder(dailyQuestRepository, clock),
            FakeRerollAllowancePolicy(),
            FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), FIXED_NOW)),
            QuestAccessPolicy(),
            clock,
        )

        val result = useCase(context = EMPTY_CONTEXT, timeZone = TimeZone.UTC)

        assertEquals(realQuest, (result as DataResult.Success).value.quest)
    }

    @Test
    fun missingQuestReferenceProducesATypedContentError() = runTest {
        val questRepository = realBundledQuestRepository(testDispatchers())
        val clock = TestAppClock(FIXED_NOW)
        val dailyQuestRepository = FakeDailyQuestRepository()
        dailyQuestRepository.saveDailyQuest(
            DailyQuest(
                questId = QuestId("does-not-exist"),
                localDate = clock.today(TimeZone.UTC),
                selectionIndex = 0,
                selectedAt = FIXED_NOW,
                source = DailyQuestSource.AUTOMATIC,
                context = EMPTY_CONTEXT,
            ),
        )
        val useCase = GetOrSelectDailyQuest(
            FakeFamilyRepository(),
            questRepository,
            dailyQuestRepository,
            FakeQuestRecommendationPolicy(),
            historyBuilder(dailyQuestRepository, clock),
            FakeRerollAllowancePolicy(),
            FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), FIXED_NOW)),
            QuestAccessPolicy(),
            clock,
        )

        val result = useCase(context = EMPTY_CONTEXT, timeZone = TimeZone.UTC)

        assertEquals(DataResult.Error(AppError.Content(ContentError.QUEST_NOT_FOUND)), result)
    }

    @Test
    fun rerollCanSelectAnotherEligibleQuest() = runTest {
        val familyRepository = FakeFamilyRepository()
        familyRepository.saveProfile(testFamilyProfile())
        val questRepository = realBundledQuestRepository(testDispatchers())
        val allQuests = (questRepository.getAllQuests() as DataResult.Success).value
        val current = allQuests[0]
        val replacement = allQuests[1]
        val clock = TestAppClock(FIXED_NOW)
        val dailyQuestRepository = FakeDailyQuestRepository()
        dailyQuestRepository.saveDailyQuest(
            DailyQuest(
                questId = current.id,
                localDate = clock.today(TimeZone.UTC),
                selectionIndex = 0,
                selectedAt = FIXED_NOW,
                source = DailyQuestSource.AUTOMATIC,
                context = EMPTY_CONTEXT,
            ),
        )
        val policy = FakeQuestRecommendationPolicy(
            QuestRecommendationResult.Success(replacement, score = 10, reasons = emptyList()),
        )
        val transaction = FakeDailyQuestTransaction(dailyQuestRepository)
        val useCase = RerollDailyQuest(
            familyRepository, questRepository, dailyQuestRepository, transaction, policy,
            historyBuilder(dailyQuestRepository, clock), FakeRerollAllowancePolicy(),
            FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), FIXED_NOW)), QuestAccessPolicy(), clock,
        )

        val result = useCase(TimeZone.UTC)

        val selection = (result as DataResult.Success).value
        assertEquals(replacement, selection.quest)
        assertEquals(1, selection.dailyQuest.selectionIndex)
    }

    @Test
    fun recentDismissalIsPassedToTheRecommendationPolicy() = runTest {
        val familyRepository = FakeFamilyRepository()
        familyRepository.saveProfile(testFamilyProfile())
        val questRepository = realBundledQuestRepository(testDispatchers())
        val allQuests = (questRepository.getAllQuests() as DataResult.Success).value
        val clock = TestAppClock(FIXED_NOW)
        val dailyQuestRepository = FakeDailyQuestRepository()
        val dismissedQuestId = allQuests[0].id
        dailyQuestRepository.recordDismissal(
            DismissedQuest(questId = dismissedQuestId, dismissedAt = FIXED_NOW, localDate = clock.today(TimeZone.UTC)),
        )
        val policy = FakeQuestRecommendationPolicy(
            QuestRecommendationResult.Success(allQuests[1], score = 10, reasons = emptyList()),
        )
        val useCase = GetOrSelectDailyQuest(
            familyRepository, questRepository, dailyQuestRepository, policy,
            historyBuilder(dailyQuestRepository, clock), FakeRerollAllowancePolicy(),
            FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), FIXED_NOW)), QuestAccessPolicy(), clock,
        )

        useCase(context = EMPTY_CONTEXT, timeZone = TimeZone.UTC)

        assertTrue(policy.requests.single().history.recentlyDismissed.any { it.questId == dismissedQuestId })
    }
}

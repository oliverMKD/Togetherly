package com.togetherly.integration

import com.togetherly.content.realBundledQuestRepository
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.id.SequentialIdGenerator
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.QuestCompletion
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.repository.FakeQuestSessionTransaction
import com.togetherly.domain.completion.usecase.CompleteQuest
import com.togetherly.domain.completion.usecase.StartQuest
import com.togetherly.domain.family.repository.FakeFamilyRepository
import com.togetherly.domain.journey.JourneyEntry
import com.togetherly.domain.journey.repository.FakeJourneyRepository
import com.togetherly.domain.journey.usecase.GetJourneySummary
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.QuestRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * No production [com.togetherly.domain.journey.repository.JourneyRepository] exists yet — its own
 * KDoc says a real implementation is expected to join [com.togetherly.domain.completion.repository.CompletionRepository]
 * reads with [QuestRepository] reads in memory. These tests perform that join explicitly against
 * the real bundled repository to verify the *contract* such an implementation must satisfy,
 * feeding the result into [FakeJourneyRepository].
 */
class CompletionJourneyContentIntegrationTest {

    private suspend fun startAndCompleteRealQuest(
        questRepository: QuestRepository,
        completionRepository: FakeCompletionRepository,
        clock: TestAppClock,
    ): Pair<FamilyQuest, QuestCompletion> {
        val familyRepository = FakeFamilyRepository()
        familyRepository.saveProfile(testFamilyProfile())
        val realQuest = (questRepository.getAllQuests() as DataResult.Success).value.first()
        val questSessionTransaction = FakeQuestSessionTransaction(completionRepository)
        StartQuest(
            familyRepository, questRepository, questSessionTransaction,
            FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), clock.now())),
            QuestAccessPolicy(), clock, SequentialIdGenerator(),
        )(realQuest.id)
        val completion = (CompleteQuest(completionRepository, questSessionTransaction, clock)() as DataResult.Success).value
        return realQuest to completion
    }

    @Test
    fun aRealQuestCanBeCompleted() = runTest {
        val questRepository = realBundledQuestRepository(testDispatchers())
        val completionRepository = FakeCompletionRepository()
        val (_, completion) = startAndCompleteRealQuest(questRepository, completionRepository, TestAppClock(FIXED_NOW))

        val stored = (completionRepository.getCompletions() as DataResult.Success).value
        assertTrue(stored.any { it.id == completion.id })
        assertEquals(null, (completionRepository.getActiveSession() as DataResult.Success).value)
    }

    @Test
    fun completionPreservesQuestIdAndVersion() = runTest {
        val questRepository = realBundledQuestRepository(testDispatchers())
        val (realQuest, completion) = startAndCompleteRealQuest(questRepository, FakeCompletionRepository(), TestAppClock(FIXED_NOW))

        assertEquals(realQuest.id, completion.questId)
        assertEquals(realQuest.version, completion.questVersion)
    }

    @Test
    fun journeyResolvesCompletionToCatalogueContent() = runTest {
        val questRepository = realBundledQuestRepository(testDispatchers())
        val (realQuest, completion) = startAndCompleteRealQuest(questRepository, FakeCompletionRepository(), TestAppClock(FIXED_NOW))

        val resolvedQuest = (questRepository.getQuest(completion.questId) as DataResult.Success).value
        val journeyRepository = FakeJourneyRepository().apply { setEntries(listOf(JourneyEntry(completion, resolvedQuest))) }

        val summary = (GetJourneySummary(journeyRepository)(TimeZone.UTC) as DataResult.Success).value

        assertEquals(1, summary.totalCompletions)
        assertEquals(1, summary.completionsByCategory[realQuest.category])
    }

    @Test
    fun journeyRetainsCompletionWhenContentResolutionFails() = runTest {
        val questRepository = realBundledQuestRepository(testDispatchers())
        val (_, completion) = startAndCompleteRealQuest(questRepository, FakeCompletionRepository(), TestAppClock(FIXED_NOW))

        // Simulate content that can no longer be resolved (e.g. removed from a later catalogue).
        val unresolvedQuest = (questRepository.getQuest(QuestId("no-longer-in-catalogue")) as DataResult.Success).value
        assertNull(unresolvedQuest)

        val journeyRepository = FakeJourneyRepository().apply {
            setEntries(listOf(JourneyEntry(completion, unresolvedQuest)))
        }

        val summary = (GetJourneySummary(journeyRepository)(TimeZone.UTC) as DataResult.Success).value

        assertEquals(1, summary.totalCompletions)
        assertTrue(summary.completionsByCategory.isEmpty())
    }
}

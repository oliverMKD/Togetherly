package com.togetherly.integration

import com.togetherly.content.realBundledQuestRepository
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.id.SequentialIdGenerator
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.ActiveQuestSession
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.repository.FakeQuestSessionTransaction
import com.togetherly.domain.completion.usecase.StartQuest
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import com.togetherly.domain.family.repository.FakeFamilyRepository
import com.togetherly.domain.quest.QuestId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StartQuestContentIntegrationTest {

    private suspend fun familyRepositoryWithProfile(): FakeFamilyRepository =
        FakeFamilyRepository().apply { saveProfile(testFamilyProfile()) }

    @Test
    fun aRealCatalogueQuestCanStartASession() = runTest {
        val questRepository = realBundledQuestRepository(testDispatchers())
        val realQuest = (questRepository.getAllQuests() as DataResult.Success).value.first()
        val completionRepository = FakeCompletionRepository()
        val useCase = StartQuest(
            familyRepositoryWithProfile(),
            questRepository,
            FakeQuestSessionTransaction(completionRepository),
            FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), FIXED_NOW)),
            QuestAccessPolicy(),
            TestAppClock(FIXED_NOW),
            SequentialIdGenerator(),
        )

        val result = useCase(realQuest.id)

        val session = assertIs<DataResult.Success<ActiveQuestSession>>(result).value
        assertEquals(realQuest.id, session.questId)
    }

    @Test
    fun questVersionIsCopiedIntoTheActiveSession() = runTest {
        val questRepository = realBundledQuestRepository(testDispatchers())
        val realQuest = (questRepository.getAllQuests() as DataResult.Success).value.first()
        val completionRepository = FakeCompletionRepository()
        val useCase = StartQuest(
            familyRepositoryWithProfile(),
            questRepository,
            FakeQuestSessionTransaction(completionRepository),
            FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), FIXED_NOW)),
            QuestAccessPolicy(),
            TestAppClock(FIXED_NOW),
            SequentialIdGenerator(),
        )

        val result = useCase(realQuest.id)

        val session = assertIs<DataResult.Success<ActiveQuestSession>>(result).value
        assertEquals(realQuest.version, session.questVersion)
    }

    @Test
    fun missingQuestReturnsATypedError() = runTest {
        val questRepository = realBundledQuestRepository(testDispatchers())
        val completionRepository = FakeCompletionRepository()
        val useCase = StartQuest(
            familyRepositoryWithProfile(),
            questRepository,
            FakeQuestSessionTransaction(completionRepository),
            FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), FIXED_NOW)),
            QuestAccessPolicy(),
            TestAppClock(FIXED_NOW),
            SequentialIdGenerator(),
        )

        val result = useCase(QuestId("does-not-exist"))

        assertEquals(DataResult.Error(AppError.Content(ContentError.QUEST_NOT_FOUND)), result)
    }
}

package com.togetherly.integration

import com.togetherly.content.realBundledQuestRepository
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.saved.repository.FakeSavedQuestRepository
import com.togetherly.domain.saved.usecase.SetQuestSaved
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SavedQuestContentIntegrationTest {

    @Test
    fun aRealCatalogueQuestCanBeSaved() = runTest {
        val questRepository = realBundledQuestRepository(testDispatchers())
        val realQuest = (questRepository.getAllQuests() as DataResult.Success).value.first()
        val savedRepository = FakeSavedQuestRepository()
        val useCase = SetQuestSaved(savedRepository, questRepository, TestAppClock(FIXED_NOW))

        val result = useCase(realQuest.id, saved = true)

        assertEquals(DataResult.Success(true), result)
        val saved = (savedRepository.getSavedQuests() as DataResult.Success).value
        assertTrue(saved.any { it.questId == realQuest.id })
    }

    @Test
    fun anUnknownQuestCannotBeSaved() = runTest {
        val questRepository = realBundledQuestRepository(testDispatchers())
        val savedRepository = FakeSavedQuestRepository()
        val useCase = SetQuestSaved(savedRepository, questRepository, TestAppClock(FIXED_NOW))

        val result = useCase(QuestId("does-not-exist"), saved = true)

        assertEquals(DataResult.Error(AppError.Content(ContentError.QUEST_NOT_FOUND)), result)
        assertTrue((savedRepository.getSavedQuests() as DataResult.Success).value.isEmpty())
    }

    @Test
    fun savedQuestResolvesToItsDomainQuest() = runTest {
        val questRepository = realBundledQuestRepository(testDispatchers())
        val realQuest = (questRepository.getAllQuests() as DataResult.Success).value.first()
        val savedRepository = FakeSavedQuestRepository()
        SetQuestSaved(savedRepository, questRepository, TestAppClock(FIXED_NOW))(realQuest.id, saved = true)

        val savedQuest = (savedRepository.getSavedQuests() as DataResult.Success).value.single()
        val resolved = (questRepository.getQuest(savedQuest.questId) as DataResult.Success).value

        assertEquals(realQuest, resolved)
    }
}

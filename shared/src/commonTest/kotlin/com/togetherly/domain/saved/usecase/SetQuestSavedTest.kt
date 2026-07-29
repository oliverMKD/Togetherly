package com.togetherly.domain.saved.usecase

import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.validFamilyQuest
import com.togetherly.domain.saved.repository.FakeSavedQuestRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")

class SetQuestSavedTest {

    @Test
    fun existingQuestCanBeSaved() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }
        val savedQuestRepository = FakeSavedQuestRepository()
        val useCase = SetQuestSaved(savedQuestRepository, questRepository, TestAppClock(NOW))

        val result = useCase(questId = quest.id, saved = true)

        assertEquals(DataResult.Success(true), result)
        assertEquals(DataResult.Success(true), savedQuestRepository.isSaved(quest.id))
    }

    @Test
    fun missingQuestCannotBeSaved() = runTest {
        val questRepository = FakeQuestRepository()
        val savedQuestRepository = FakeSavedQuestRepository()
        val useCase = SetQuestSaved(savedQuestRepository, questRepository, TestAppClock(NOW))

        val result = useCase(questId = QuestId("missing"), saved = true)

        assertEquals(DataResult.Error(AppError.Content(ContentError.QUEST_NOT_FOUND)), result)
    }

    @Test
    fun desiredStateOperationsAreIdempotent() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }
        val savedQuestRepository = FakeSavedQuestRepository()
        val useCase = SetQuestSaved(savedQuestRepository, questRepository, TestAppClock(NOW))

        useCase(questId = quest.id, saved = true)
        useCase(questId = quest.id, saved = true)
        val savedList = (savedQuestRepository.getSavedQuests() as DataResult.Success).value
        assertEquals(1, savedList.size)

        val removedOnce = useCase(questId = quest.id, saved = false)
        val removedTwice = useCase(questId = quest.id, saved = false)
        assertEquals(DataResult.Success(false), removedOnce)
        assertEquals(DataResult.Success(false), removedTwice)
    }

    @Test
    fun clockSuppliesSavedAt() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }
        val savedQuestRepository = FakeSavedQuestRepository()
        val useCase = SetQuestSaved(savedQuestRepository, questRepository, TestAppClock(NOW))

        useCase(questId = quest.id, saved = true)

        val savedList = (savedQuestRepository.getSavedQuests() as DataResult.Success).value
        assertEquals(NOW, savedList.single().savedAt)
    }
}

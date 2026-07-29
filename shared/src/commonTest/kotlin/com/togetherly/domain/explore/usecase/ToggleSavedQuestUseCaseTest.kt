package com.togetherly.domain.explore.usecase

import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.result.DataResult
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.validFamilyQuest
import com.togetherly.domain.saved.repository.FakeSavedQuestRepository
import com.togetherly.domain.saved.usecase.SetQuestSaved
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")

class ToggleSavedQuestUseCaseTest {

    private fun useCase(questRepository: FakeQuestRepository, savedQuestRepository: FakeSavedQuestRepository) =
        ToggleSavedQuestUseCase(savedQuestRepository, SetQuestSaved(savedQuestRepository, questRepository, TestAppClock(NOW)))

    @Test
    fun togglingAnUnsavedQuestSavesIt() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }
        val savedQuestRepository = FakeSavedQuestRepository()

        val result = useCase(questRepository, savedQuestRepository)(quest.id)

        assertEquals(DataResult.Success(true), result)
        assertEquals(DataResult.Success(true), savedQuestRepository.isSaved(quest.id))
    }

    @Test
    fun togglingAnAlreadySavedQuestRemovesIt() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }
        val savedQuestRepository = FakeSavedQuestRepository()
        val toggle = useCase(questRepository, savedQuestRepository)
        toggle(quest.id)

        val result = toggle(quest.id)

        assertEquals(DataResult.Success(false), result)
        assertEquals(DataResult.Success(false), savedQuestRepository.isSaved(quest.id))
    }
}

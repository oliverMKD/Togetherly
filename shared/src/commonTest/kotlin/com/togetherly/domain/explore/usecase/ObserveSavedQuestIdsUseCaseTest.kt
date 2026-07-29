package com.togetherly.domain.explore.usecase

import app.cash.turbine.test
import com.togetherly.core.result.DataResult
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.saved.SavedQuest
import com.togetherly.domain.saved.repository.FakeSavedQuestRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ObserveSavedQuestIdsUseCaseTest {

    @Test
    fun mapsSavedQuestsDownToTheirBareIds() = runTest {
        val savedQuestRepository = FakeSavedQuestRepository()
        val now = Instant.parse("2026-06-15T08:00:00Z")
        savedQuestRepository.save(SavedQuest(QuestId("quest-1"), now))
        savedQuestRepository.save(SavedQuest(QuestId("quest-2"), now))
        val useCase = ObserveSavedQuestIdsUseCase(savedQuestRepository)

        useCase().test {
            assertEquals(DataResult.Success(setOf(QuestId("quest-1"), QuestId("quest-2"))), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun noSavedQuestsIsAnEmptySet() = runTest {
        val savedQuestRepository = FakeSavedQuestRepository()
        val useCase = ObserveSavedQuestIdsUseCase(savedQuestRepository)

        useCase().test {
            assertEquals(DataResult.Success(emptySet()), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

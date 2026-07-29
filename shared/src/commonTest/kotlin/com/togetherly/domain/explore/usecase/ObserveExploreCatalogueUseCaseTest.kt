package com.togetherly.domain.explore.usecase

import app.cash.turbine.test
import com.togetherly.core.result.DataResult
import com.togetherly.domain.explore.ExploreCatalogue
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.validFamilyQuest
import com.togetherly.domain.quest.validQuestPack
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveExploreCatalogueUseCaseTest {

    @Test
    fun pairsCurrentQuestsAndPacksIntoOneCatalogue() = runTest {
        val questRepository = FakeQuestRepository()
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val pack = validQuestPack(questIds = listOf(quest.id))
        questRepository.setQuests(listOf(quest))
        questRepository.setPacks(listOf(pack))
        val useCase = ObserveExploreCatalogueUseCase(questRepository)

        useCase().test {
            assertEquals(DataResult.Success(ExploreCatalogue(quests = listOf(quest), packs = listOf(pack))), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyCatalogueIsAValidPairing() = runTest {
        val questRepository = FakeQuestRepository()
        val useCase = ObserveExploreCatalogueUseCase(questRepository)

        useCase().test {
            assertEquals(DataResult.Success(ExploreCatalogue(quests = emptyList(), packs = emptyList())), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

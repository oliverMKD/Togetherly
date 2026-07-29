package com.togetherly.content.repository

import com.togetherly.content.loader.FakeQuestCatalogueLoader
import com.togetherly.content.mapper.QuestCatalogue
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.quest.ArtworkKey
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPack
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.domain.quest.QuestSummary
import com.togetherly.domain.quest.QuestTitle
import com.togetherly.domain.quest.validFamilyQuest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private fun testPack(id: String, sortOrder: Int, questIds: List<QuestId>) = QuestPack(
    id = QuestPackId(id),
    title = QuestTitle("Pack $id"),
    description = QuestSummary("A quest pack."),
    category = null,
    access = QuestAccess.Free,
    questIds = questIds,
    artworkKey = ArtworkKey("packs/$id"),
    sortOrder = sortOrder,
)

class BundledQuestRepositoryTest {

    private val quest1 = validFamilyQuest(id = QuestId("quest-1"), packId = QuestPackId("pack-a"))
    private val quest2 = validFamilyQuest(
        id = QuestId("quest-2"),
        title = QuestTitle("Secret Kindness Notes"),
        packId = QuestPackId("pack-b"),
    )
    private val packA = testPack(id = "pack-a", sortOrder = 1, questIds = listOf(QuestId("quest-1")))
    private val packB = testPack(id = "pack-b", sortOrder = 0, questIds = listOf(QuestId("quest-2")))

    private val catalogue = QuestCatalogue(
        schemaVersion = 1,
        catalogueVersion = 1,
        locale = "en",
        packs = listOf(packA, packB),
        quests = listOf(quest1, quest2),
    )

    @Test
    fun returnsAQuestById() = runTest {
        val repository = repositoryWith(DataResult.Success(catalogue))

        val result = repository.getQuest(QuestId("quest-1"))

        assertEquals(DataResult.Success(quest1), result)
    }

    @Test
    fun missingQuestReturnsSuccessNull() = runTest {
        val repository = repositoryWith(DataResult.Success(catalogue))

        val result = repository.getQuest(QuestId("missing"))

        assertEquals(DataResult.Success(null), result)
    }

    @Test
    fun packsAreSortedBySortOrder() = runTest {
        val repository = repositoryWith(DataResult.Success(catalogue))

        val result = repository.getPacks()

        assertEquals(DataResult.Success(listOf(packB, packA)), result)
    }

    @Test
    fun missingPackReturnsSuccessNull() = runTest {
        val repository = repositoryWith(DataResult.Success(catalogue))

        val result = repository.getPack(QuestPackId("missing"))

        assertEquals(DataResult.Success(null), result)
    }

    @Test
    fun onlyDomainModelsAreReturned() = runTest {
        // QuestRepository's own signature only exposes FamilyQuest/QuestPack, and content DTOs
        // are `internal` to content.model — no DTO type is even importable here. This test
        // documents that guarantee by asserting on real domain-typed fields.
        val repository = repositoryWith(DataResult.Success(catalogue))

        val quest = (repository.getQuest(QuestId("quest-1")) as DataResult.Success).value

        assertEquals(QuestTitle("Backyard Scavenger Hunt"), quest?.title)
    }

    @Test
    fun observeAllQuestsEmitsTheLoadedCatalogue() = runTest {
        val repository = repositoryWith(DataResult.Success(catalogue))

        val flow = repository.observeAllQuests()
        advanceUntilIdle()

        assertEquals(DataResult.Success(listOf(quest1, quest2)), flow.first())
    }

    @Test
    fun observePacksEmitsPacksSortedBySortOrder() = runTest {
        val repository = repositoryWith(DataResult.Success(catalogue))

        val flow = repository.observePacks()
        advanceUntilIdle()

        assertEquals(DataResult.Success(listOf(packB, packA)), flow.first())
    }

    @Test
    fun secondCallDoesNotReloadTheCatalogue() = runTest {
        val loader = FakeQuestCatalogueLoader(DataResult.Success(catalogue))
        val repository = BundledQuestRepository(loader, TestAppDispatchers(StandardTestDispatcher(testScheduler)))

        repository.getAllQuests()
        repository.getAllQuests()

        assertEquals(1, loader.loadCount)
    }

    @Test
    fun observingMultipleStreamsDoesNotTriggerDuplicateLoads() = runTest {
        val loader = FakeQuestCatalogueLoader(DataResult.Success(catalogue))
        val repository = BundledQuestRepository(loader, TestAppDispatchers(StandardTestDispatcher(testScheduler)))

        repository.observeAllQuests()
        repository.observePacks()
        repository.observeQuest(QuestId("quest-1"))
        advanceUntilIdle()
        repository.getAllQuests()
        repository.getPacks()

        assertEquals(1, loader.loadCount)
    }

    @Test
    fun failedLoadCanBeRetried() = runTest {
        val loader = FakeQuestCatalogueLoader(DataResult.Error(AppError.Storage(StorageError.READ_FAILED)))
        val repository = BundledQuestRepository(loader, TestAppDispatchers(StandardTestDispatcher(testScheduler)))

        assertIs<DataResult.Error>(repository.getAllQuests())

        loader.setResult(DataResult.Success(catalogue))
        val retried = repository.getAllQuests()

        assertEquals(DataResult.Success(listOf(quest1, quest2)), retried)
        assertEquals(2, loader.loadCount)
    }

    private fun TestScope.repositoryWith(
        result: DataResult<QuestCatalogue>,
    ) = BundledQuestRepository(
        catalogueLoader = FakeQuestCatalogueLoader(result),
        dispatchers = TestAppDispatchers(StandardTestDispatcher(testScheduler)),
    )
}

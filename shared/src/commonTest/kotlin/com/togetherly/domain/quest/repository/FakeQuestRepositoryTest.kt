package com.togetherly.domain.quest.repository

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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private fun testPack(
    id: String,
    sortOrder: Int,
) = QuestPack(
    id = QuestPackId(id),
    title = QuestTitle("Pack $id"),
    description = QuestSummary("A quest pack."),
    category = null,
    access = QuestAccess.Free,
    questIds = listOf(QuestId("quest-1")),
    artworkKey = ArtworkKey("packs/$id"),
    sortOrder = sortOrder,
)

class FakeQuestRepositoryTest {

    @Test
    fun observesTheConfiguredCatalogue() = runTest {
        val repository = FakeQuestRepository()
        val quest = validFamilyQuest()

        repository.setQuests(listOf(quest))

        assertEquals(DataResult.Success(listOf(quest)), repository.observeAllQuests().first())
        assertEquals(DataResult.Success(listOf(quest)), repository.getAllQuests())
    }

    @Test
    fun returnsAQuestById() = runTest {
        val repository = FakeQuestRepository()
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        repository.setQuests(listOf(quest))

        val result = repository.getQuest(QuestId("quest-1"))

        assertEquals(DataResult.Success(quest), result)
    }

    @Test
    fun missingQuestReturnsSuccessNull() = runTest {
        val repository = FakeQuestRepository()

        val result = repository.getQuest(QuestId("missing"))

        assertEquals(DataResult.Success(null), result)
    }

    @Test
    fun returnsPacksOrderedConsistently() = runTest {
        val repository = FakeQuestRepository()
        val second = testPack(id = "pack-b", sortOrder = 1)
        val first = testPack(id = "pack-a", sortOrder = 0)

        repository.setPacks(listOf(second, first))

        assertEquals(DataResult.Success(listOf(first, second)), repository.getPacks())
        assertEquals(DataResult.Success(listOf(first, second)), repository.observePacks().first())
    }

    @Test
    fun configuredReadErrorIsPreserved() = runTest {
        val repository = FakeQuestRepository()
        val error = AppError.Storage(StorageError.READ_FAILED)
        repository.setNextError(error)

        val result = repository.getAllQuests()

        assertEquals(DataResult.Error(error), result)
    }

    @Test
    fun domainObjectsAreReturnedWithoutTransformation() = runTest {
        val repository = FakeQuestRepository()
        val quest = validFamilyQuest()
        repository.setQuests(listOf(quest))

        val result = repository.getAllQuests()

        val returned = (result as DataResult.Success).value.single()
        assertEquals(quest, returned)
        assertEquals(quest.title, returned.title)
    }
}

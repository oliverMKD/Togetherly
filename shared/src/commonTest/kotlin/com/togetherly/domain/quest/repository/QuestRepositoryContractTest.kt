package com.togetherly.domain.quest.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.quest.ArtworkKey
import com.togetherly.domain.quest.FamilyQuest
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

/**
 * A shared behavioral contract every [QuestRepository] implementation must satisfy, run against
 * both [FakeQuestRepository] and [com.togetherly.content.repository.BundledQuestRepository] by
 * their respective concrete subclasses. Only covers what the *interface* promises (ordering,
 * missing-value behavior, observation, error propagation) — never resource-loading specifics
 * (malformed JSON, missing files, etc.) that are outside this contract and belong to the content
 * pipeline's own tests instead.
 */
internal abstract class QuestRepositoryContractTest {

    abstract fun repository(quests: List<FamilyQuest>, packs: List<QuestPack>): QuestRepository

    abstract fun repositoryWithError(error: AppError): QuestRepository

    private val questA = validFamilyQuest(id = QuestId("quest-a"), packId = QuestPackId("pack-a"))
    private val questB = validFamilyQuest(
        id = QuestId("quest-b"),
        title = QuestTitle("Quest B"),
        packId = QuestPackId("pack-b"),
    )
    private val packA = contractTestPack(id = "pack-a", sortOrder = 1, questIds = listOf(QuestId("quest-a")))
    private val packB = contractTestPack(id = "pack-b", sortOrder = 0, questIds = listOf(QuestId("quest-b")))

    @Test
    fun getAllQuestsReturnsQuestsInDeterministicSourceOrder() = runTest {
        val repository = repository(listOf(questA, questB), listOf(packA, packB))

        assertEquals(DataResult.Success(listOf(questA, questB)), repository.getAllQuests())
    }

    @Test
    fun getQuestReturnsSuccessNullWhenMissing() = runTest {
        val repository = repository(listOf(questA), listOf(packA))

        assertEquals(DataResult.Success(null), repository.getQuest(QuestId("missing")))
    }

    @Test
    fun getPacksReturnsPacksSortedBySortOrder() = runTest {
        val repository = repository(listOf(questA, questB), listOf(packA, packB))

        assertEquals(DataResult.Success(listOf(packB, packA)), repository.getPacks())
    }

    @Test
    fun getPackReturnsSuccessNullWhenMissing() = runTest {
        val repository = repository(listOf(questA), listOf(packA))

        assertEquals(DataResult.Success(null), repository.getPack(QuestPackId("missing")))
    }

    @Test
    fun observeAllQuestsEmitsTheConfiguredQuests() = runTest {
        val repository = repository(listOf(questA, questB), listOf(packA, packB))

        assertEquals(DataResult.Success(listOf(questA, questB)), repository.observeAllQuests().first())
    }

    @Test
    fun observeQuestEmitsTheMatchingQuest() = runTest {
        val repository = repository(listOf(questA, questB), listOf(packA, packB))

        assertEquals(DataResult.Success(questB), repository.observeQuest(QuestId("quest-b")).first())
    }

    @Test
    fun observePacksEmitsPacksSortedBySortOrder() = runTest {
        val repository = repository(listOf(questA, questB), listOf(packA, packB))

        assertEquals(DataResult.Success(listOf(packB, packA)), repository.observePacks().first())
    }

    @Test
    fun getAllQuestsPropagatesTheUnderlyingError() = runTest {
        val error = AppError.Storage(StorageError.READ_FAILED)
        val repository = repositoryWithError(error)

        assertEquals(DataResult.Error(error), repository.getAllQuests())
    }

    @Test
    fun getPacksPropagatesTheUnderlyingError() = runTest {
        val error = AppError.Storage(StorageError.READ_FAILED)
        val repository = repositoryWithError(error)

        assertEquals(DataResult.Error(error), repository.getPacks())
    }
}

internal fun contractTestPack(
    id: String,
    sortOrder: Int,
    questIds: List<QuestId>,
) = QuestPack(
    id = QuestPackId(id),
    title = QuestTitle("Pack $id"),
    description = QuestSummary("A quest pack."),
    category = null,
    access = QuestAccess.Free,
    questIds = questIds,
    artworkKey = ArtworkKey("packs/$id"),
    sortOrder = sortOrder,
)

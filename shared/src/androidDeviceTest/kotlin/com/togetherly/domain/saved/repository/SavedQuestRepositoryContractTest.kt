package com.togetherly.domain.saved.repository

import com.togetherly.core.result.DataResult
import com.togetherly.data.testSavedQuest
import com.togetherly.domain.quest.QuestId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * A shared behavioral contract every [SavedQuestRepository] implementation must satisfy, run
 * against both [FakeSavedQuestRepository] and [com.togetherly.data.saved.RoomSavedQuestRepository]
 * by their respective concrete subclasses.
 */
internal abstract class SavedQuestRepositoryContractTest {

    abstract fun repository(): SavedQuestRepository

    @Test
    fun emptyByDefault() = runTest {
        val repository = repository()

        assertEquals(DataResult.Success(emptyList()), repository.getSavedQuests())
        assertEquals(DataResult.Success(false), repository.isSaved(QuestId("quest-1")))
    }

    @Test
    fun savedQuestsAreNewestFirst() = runTest {
        val repository = repository()
        val earlier = testSavedQuest(questId = QuestId("quest-1"), savedAt = Instant.fromEpochMilliseconds(1_000L))
        val later = testSavedQuest(questId = QuestId("quest-2"), savedAt = Instant.fromEpochMilliseconds(2_000L))

        repository.save(earlier)
        repository.save(later)

        assertEquals(DataResult.Success(listOf(later, earlier)), repository.getSavedQuests())
        assertEquals(DataResult.Success(listOf(later, earlier)), repository.observeSavedQuests().first())
    }

    @Test
    fun savingTheSameQuestTwiceIsIdempotentAndPreservesTheOriginalTimestamp() = runTest {
        val repository = repository()
        val original = testSavedQuest(questId = QuestId("quest-1"), savedAt = Instant.fromEpochMilliseconds(1_000L))
        repository.save(original)

        val resaved = testSavedQuest(questId = QuestId("quest-1"), savedAt = Instant.fromEpochMilliseconds(9_000L))
        repository.save(resaved)

        val result = repository.getSavedQuests()
        assertEquals(DataResult.Success(listOf(original)), result)
    }

    @Test
    fun isSavedReflectsCurrentState() = runTest {
        val repository = repository()
        repository.save(testSavedQuest(questId = QuestId("quest-1")))

        assertEquals(DataResult.Success(true), repository.isSaved(QuestId("quest-1")))
        assertEquals(DataResult.Success(false), repository.isSaved(QuestId("quest-2")))
    }

    @Test
    fun removingAnUnknownQuestSucceeds() = runTest {
        val repository = repository()

        assertEquals(DataResult.Success(Unit), repository.remove(QuestId("never-saved")))
    }

    @Test
    fun removingASavedQuestUpdatesTheList() = runTest {
        val repository = repository()
        repository.save(testSavedQuest(questId = QuestId("quest-1"), savedAt = Instant.fromEpochMilliseconds(1_000L)))
        val kept = testSavedQuest(questId = QuestId("quest-2"), savedAt = Instant.fromEpochMilliseconds(2_000L))
        repository.save(kept)

        repository.remove(QuestId("quest-1"))

        assertEquals(DataResult.Success(listOf(kept)), repository.getSavedQuests())
    }

    @Test
    fun distinctTimestampsAreNotConfusedWithReordering() = runTest {
        val repository = repository()
        val a = testSavedQuest(questId = QuestId("quest-a"), savedAt = Instant.fromEpochMilliseconds(1_000L))
        val b = testSavedQuest(questId = QuestId("quest-b"), savedAt = a.savedAt + 1.hours)

        repository.save(a)
        repository.save(b)

        assertEquals(DataResult.Success(listOf(b, a)), repository.getSavedQuests())
    }
}

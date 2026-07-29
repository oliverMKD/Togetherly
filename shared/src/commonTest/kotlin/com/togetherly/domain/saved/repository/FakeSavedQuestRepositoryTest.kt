package com.togetherly.domain.saved.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.saved.SavedQuest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private val SAVED_AT = Instant.parse("2026-06-15T08:00:00Z")

class FakeSavedQuestRepositoryTest {

    @Test
    fun initiallyEmpty() = runTest {
        val repository = FakeSavedQuestRepository()

        assertEquals(DataResult.Success(emptyList()), repository.getSavedQuests())
        assertEquals(DataResult.Success(emptyList()), repository.observeSavedQuests().first())
    }

    @Test
    fun savingEmitsTheSavedQuest() = runTest {
        val repository = FakeSavedQuestRepository()
        val saved = SavedQuest(QuestId("quest-1"), SAVED_AT)

        repository.save(saved)

        assertEquals(DataResult.Success(listOf(saved)), repository.observeSavedQuests().first())
    }

    @Test
    fun savingTwiceIsIdempotent() = runTest {
        val repository = FakeSavedQuestRepository()
        val saved = SavedQuest(QuestId("quest-1"), SAVED_AT)

        repository.save(saved)
        repository.save(saved)

        assertEquals(DataResult.Success(listOf(saved)), repository.getSavedQuests())
    }

    @Test
    fun isSavedReturnsTheCorrectValue() = runTest {
        val repository = FakeSavedQuestRepository()
        repository.save(SavedQuest(QuestId("quest-1"), SAVED_AT))

        assertEquals(DataResult.Success(true), repository.isSaved(QuestId("quest-1")))
        assertEquals(DataResult.Success(false), repository.isSaved(QuestId("quest-2")))
    }

    @Test
    fun removingEmitsTheNewList() = runTest {
        val repository = FakeSavedQuestRepository()
        repository.save(SavedQuest(QuestId("quest-1"), SAVED_AT))

        repository.remove(QuestId("quest-1"))

        assertEquals(DataResult.Success(emptyList()), repository.observeSavedQuests().first())
    }

    @Test
    fun removingUnknownQuestIsIdempotent() = runTest {
        val repository = FakeSavedQuestRepository()

        val result = repository.remove(QuestId("unknown"))

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(DataResult.Success(emptyList()), repository.getSavedQuests())
    }

    @Test
    fun newestSavedQuestAppearsFirst() = runTest {
        val repository = FakeSavedQuestRepository()
        val earlier = SavedQuest(QuestId("quest-1"), SAVED_AT)
        val later = SavedQuest(QuestId("quest-2"), SAVED_AT + 1.hours)

        repository.save(earlier)
        repository.save(later)

        assertEquals(DataResult.Success(listOf(later, earlier)), repository.getSavedQuests())
    }

    @Test
    fun configuredErrorIsReturned() = runTest {
        val repository = FakeSavedQuestRepository()
        val error = AppError.Storage(StorageError.WRITE_FAILED)
        repository.setNextError(error)

        val result = repository.save(SavedQuest(QuestId("quest-1"), SAVED_AT))

        assertEquals(DataResult.Error(error), result)
    }
}

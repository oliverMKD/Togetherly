package com.togetherly.domain.journey.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.validQuestCompletion
import com.togetherly.domain.journey.JourneyEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeJourneyRepositoryTest {

    @Test
    fun emitsConfiguredEntries() = runTest {
        val repository = FakeJourneyRepository()
        val entry = JourneyEntry(validQuestCompletion(id = CompletionId("completion-1")), quest = null)

        repository.setEntries(listOf(entry))

        assertEquals(DataResult.Success(listOf(entry)), repository.observeJourney().first())
        assertEquals(DataResult.Success(listOf(entry)), repository.getJourney())
    }

    @Test
    fun preservesEntriesWithMissingQuests() = runTest {
        val repository = FakeJourneyRepository()
        val entry = JourneyEntry(validQuestCompletion(id = CompletionId("completion-1")), quest = null)

        repository.setEntries(listOf(entry))

        val result = repository.getJourney()
        val returned = (result as DataResult.Success).value.single()
        assertEquals(null, returned.quest)
        assertEquals(entry.completion, returned.completion)
    }

    @Test
    fun configuredErrorIsEmitted() = runTest {
        val repository = FakeJourneyRepository()
        val error = AppError.Storage(StorageError.READ_FAILED)

        repository.setError(error)

        assertEquals(DataResult.Error(error), repository.getJourney())
        assertEquals(DataResult.Error(error), repository.observeJourney().first())
    }
}

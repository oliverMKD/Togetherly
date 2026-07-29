package com.togetherly.domain.completion.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.FamilyReaction
import com.togetherly.domain.completion.validActiveQuestSession
import com.togetherly.domain.completion.validQuestCompletion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private val COMPLETED_AT = Instant.parse("2026-06-15T10:00:00Z")

class FakeCompletionRepositoryTest {

    @Test
    fun initiallyHasNoActiveSession() = runTest {
        val repository = FakeCompletionRepository()

        assertEquals(DataResult.Success(null), repository.getActiveSession())
        assertEquals(DataResult.Success(null), repository.observeActiveSession().first())
    }

    @Test
    fun savingEmitsActiveSession() = runTest {
        val repository = FakeCompletionRepository()
        val session = validActiveQuestSession()

        repository.saveActiveSession(session)

        assertEquals(DataResult.Success(session), repository.observeActiveSession().first())
    }

    @Test
    fun replacingActiveSessionWorks() = runTest {
        val repository = FakeCompletionRepository()
        val first = validActiveQuestSession(completionId = CompletionId("completion-1"))
        val second = validActiveQuestSession(completionId = CompletionId("completion-2"))

        repository.saveActiveSession(first)
        repository.saveActiveSession(second)

        assertEquals(DataResult.Success(second), repository.getActiveSession())
    }

    @Test
    fun clearingIsIdempotent() = runTest {
        val repository = FakeCompletionRepository()

        val firstClear = repository.clearActiveSession()
        val secondClear = repository.clearActiveSession()

        assertEquals(DataResult.Success(Unit), firstClear)
        assertEquals(DataResult.Success(Unit), secondClear)
        assertEquals(DataResult.Success(null), repository.getActiveSession())
    }

    @Test
    fun savingCompletionEmitsNewestFirstList() = runTest {
        val repository = FakeCompletionRepository()
        val earlier = validQuestCompletion(id = CompletionId("completion-1"), completedAt = COMPLETED_AT)
        val later = validQuestCompletion(id = CompletionId("completion-2"), completedAt = COMPLETED_AT + 1.hours)

        repository.saveCompletion(earlier)
        repository.saveCompletion(later)

        assertEquals(DataResult.Success(listOf(later, earlier)), repository.observeCompletions().first())
    }

    @Test
    fun replacingSameCompletionIdDoesNotDuplicateIt() = runTest {
        val repository = FakeCompletionRepository()
        val original = validQuestCompletion(id = CompletionId("completion-1"), completedAt = COMPLETED_AT)
        val updated = original.copy(reactions = setOf(FamilyReaction.HAPPY))

        repository.saveCompletion(original)
        repository.saveCompletion(updated)

        val result = repository.getCompletions()
        val completions = (result as DataResult.Success).value
        assertEquals(1, completions.size)
        assertEquals(updated, completions.single())
    }

    @Test
    fun deletingCompletionUpdatesObservers() = runTest {
        val repository = FakeCompletionRepository()
        val completion = validQuestCompletion(id = CompletionId("completion-1"))
        repository.saveCompletion(completion)

        repository.deleteCompletion(CompletionId("completion-1"))

        assertEquals(DataResult.Success(emptyList()), repository.observeCompletions().first())
    }

    @Test
    fun deletingUnknownCompletionIsIdempotent() = runTest {
        val repository = FakeCompletionRepository()

        val result = repository.deleteCompletion(CompletionId("unknown"))

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(DataResult.Success(emptyList()), repository.getCompletions())
    }

    @Test
    fun configuredFailuresArePreserved() = runTest {
        val repository = FakeCompletionRepository()
        val error = AppError.Storage(StorageError.WRITE_FAILED)
        repository.setNextError(error)

        val result = repository.saveCompletion(validQuestCompletion())

        assertEquals(DataResult.Error(error), result)
    }
}

package com.togetherly.domain.completion.repository

import com.togetherly.core.result.DataResult
import com.togetherly.data.testActiveQuestSession
import com.togetherly.data.testPhotoMedia
import com.togetherly.data.testQuestCompletion
import com.togetherly.data.testVoiceMedia
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.FamilyReaction
import com.togetherly.domain.completion.MemoryNote
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * A shared behavioral contract every [CompletionRepository] implementation must satisfy, run
 * against both [FakeCompletionRepository] and [com.togetherly.data.completion.RoomCompletionRepository]
 * by their respective concrete subclasses. Corrupted-storage behavior is Room-specific — a fake
 * can't produce a genuinely corrupted row — and is verified only in the Room subclass.
 */
internal abstract class CompletionRepositoryContractTest {

    abstract fun repository(): CompletionRepository

    @Test
    fun missingActiveSessionReturnsSuccessNull() = runTest {
        assertEquals(DataResult.Success(null), repository().getActiveSession())
    }

    @Test
    fun savedActiveSessionIsReturnedByGetActiveSession() = runTest {
        val repository = repository()
        val session = testActiveQuestSession()

        repository.saveActiveSession(session)

        assertEquals(DataResult.Success(session), repository.getActiveSession())
    }

    @Test
    fun observeActiveSessionEmitsTheCurrentState() = runTest {
        val repository = repository()
        val session = testActiveQuestSession()

        repository.saveActiveSession(session)

        assertEquals(DataResult.Success(session), repository.observeActiveSession().first())
    }

    @Test
    fun savingASecondActiveSessionReplacesTheFirst() = runTest {
        val repository = repository()
        repository.saveActiveSession(testActiveQuestSession(completionId = CompletionId("completion-a")))

        val replacement = testActiveQuestSession(completionId = CompletionId("completion-b"))
        repository.saveActiveSession(replacement)

        assertEquals(DataResult.Success(replacement), repository.getActiveSession())
    }

    @Test
    fun clearActiveSessionIsIdempotent() = runTest {
        val repository = repository()

        assertEquals(DataResult.Success(Unit), repository.clearActiveSession())
        assertEquals(DataResult.Success(Unit), repository.clearActiveSession())
        assertEquals(DataResult.Success(null), repository.getActiveSession())
    }

    @Test
    fun clearActiveSessionRemovesTheSession() = runTest {
        val repository = repository()
        repository.saveActiveSession(testActiveQuestSession())

        repository.clearActiveSession()

        assertEquals(DataResult.Success(null), repository.getActiveSession())
    }

    @Test
    fun missingCompletionReturnsSuccessNull() = runTest {
        assertEquals(DataResult.Success(null), repository().getCompletion(CompletionId("completion-1")))
    }

    @Test
    fun savedMinimalCompletionIsReturnedByGetCompletion() = runTest {
        val repository = repository()
        val completion = testQuestCompletion()

        repository.saveCompletion(completion)

        assertEquals(DataResult.Success(completion), repository.getCompletion(completion.id))
    }

    @Test
    fun savingACompletionWithANoteRoundTrips() = runTest {
        val repository = repository()
        val completion = testQuestCompletion(note = MemoryNote("What a wonderful afternoon."))

        repository.saveCompletion(completion)

        assertEquals(DataResult.Success(completion), repository.getCompletion(completion.id))
    }

    @Test
    fun savingACompletionWithReactionsRoundTrips() = runTest {
        val repository = repository()
        val completion = testQuestCompletion(reactions = setOf(FamilyReaction.HAPPY, FamilyReaction.SILLY))

        repository.saveCompletion(completion)

        assertEquals(DataResult.Success(completion), repository.getCompletion(completion.id))
    }

    @Test
    fun savingACompletionWithPhotoMediaRoundTrips() = runTest {
        val repository = repository()
        val completion = testQuestCompletion(media = listOf(testPhotoMedia()))

        repository.saveCompletion(completion)

        assertEquals(DataResult.Success(completion), repository.getCompletion(completion.id))
    }

    @Test
    fun savingACompletionWithVoiceMediaRoundTrips() = runTest {
        val repository = repository()
        val completion = testQuestCompletion(media = listOf(testVoiceMedia()))

        repository.saveCompletion(completion)

        assertEquals(DataResult.Success(completion), repository.getCompletion(completion.id))
    }

    @Test
    fun savingAgainWithTheSameIdReplacesRatherThanDuplicates() = runTest {
        val repository = repository()
        val original = testQuestCompletion(
            note = MemoryNote("First note"),
            reactions = setOf(FamilyReaction.HAPPY),
            media = listOf(testPhotoMedia()),
        )
        repository.saveCompletion(original)

        val replacement = original.copy(
            note = MemoryNote("Second note"),
            reactions = setOf(FamilyReaction.CALM),
            media = listOf(testVoiceMedia()),
        )
        repository.saveCompletion(replacement)

        assertEquals(DataResult.Success(replacement), repository.getCompletion(original.id))
        assertEquals(DataResult.Success(listOf(replacement)), repository.getCompletions())
    }

    @Test
    fun deletingACompletionRemovesIt() = runTest {
        val repository = repository()
        val completion = testQuestCompletion()
        repository.saveCompletion(completion)

        repository.deleteCompletion(completion.id)

        assertEquals(DataResult.Success(null), repository.getCompletion(completion.id))
    }

    @Test
    fun deletingAnUnknownCompletionSucceeds() = runTest {
        assertEquals(DataResult.Success(Unit), repository().deleteCompletion(CompletionId("never-existed")))
    }

    @Test
    fun completionsAreOrderedNewestFirst() = runTest {
        val repository = repository()
        val older = testQuestCompletion(
            id = CompletionId("completion-old"),
            completedAt = kotlin.time.Instant.fromEpochMilliseconds(1_000L),
        )
        val newer = testQuestCompletion(
            id = CompletionId("completion-new"),
            completedAt = kotlin.time.Instant.fromEpochMilliseconds(2_000L),
        )

        repository.saveCompletion(older)
        repository.saveCompletion(newer)

        assertEquals(DataResult.Success(listOf(newer, older)), repository.getCompletions())
    }

    @Test
    fun observeCompletionsEmitsAfterASaveAndADelete() = runTest {
        val repository = repository()
        val completion = testQuestCompletion()

        repository.saveCompletion(completion)
        assertEquals(DataResult.Success(listOf(completion)), repository.observeCompletions().first())

        repository.deleteCompletion(completion.id)
        assertEquals(DataResult.Success(emptyList()), repository.observeCompletions().first())
    }
}

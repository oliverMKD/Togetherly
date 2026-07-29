package com.togetherly.domain.completion.usecase

import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.ActiveQuestSession
import com.togetherly.domain.completion.FamilyReaction
import com.togetherly.domain.completion.QuestCompletion
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.repository.FakeQuestSessionTransaction
import com.togetherly.domain.completion.repository.QuestSessionTransaction
import com.togetherly.domain.completion.validActiveQuestSession
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")

class CompleteQuestTest {

    @Test
    fun completingSavesCompletionBeforeClearingSession() = runTest {
        val session = validActiveQuestSession(startedAt = NOW)
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(session) }
        val useCase = CompleteQuest(completionRepository, FakeQuestSessionTransaction(completionRepository), TestAppClock(NOW))

        val result = useCase(reactions = setOf(FamilyReaction.HAPPY))

        val completion = (result as DataResult.Success).value
        assertEquals(session.completionId, completion.id)
        assertEquals(listOf(completion), (completionRepository.getCompletions() as DataResult.Success).value)
        assertEquals(DataResult.Success(null), completionRepository.getActiveSession())
    }

    @Test
    fun failedCompletionSavePreservesActiveSession() = runTest {
        val session = validActiveQuestSession(startedAt = NOW)
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(session) }
        val error = AppError.Storage(StorageError.WRITE_FAILED)
        completionRepository.setSaveCompletionError(error)
        val useCase = CompleteQuest(completionRepository, FakeQuestSessionTransaction(completionRepository), TestAppClock(NOW))

        val result = useCase()

        assertEquals(DataResult.Error(error), result)
        assertEquals(DataResult.Success(session), completionRepository.getActiveSession())
    }

    @Test
    fun noActiveSessionReturnsTypedError() = runTest {
        val completionRepository = FakeCompletionRepository()
        val useCase = CompleteQuest(completionRepository, FakeQuestSessionTransaction(completionRepository), TestAppClock(NOW))

        val result = useCase()

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.NO_ACTIVE_SESSION)), result)
    }

    @Test
    fun completionTimestampComesFromAppClock() = runTest {
        val session = validActiveQuestSession(startedAt = NOW)
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(session) }
        val useCase = CompleteQuest(completionRepository, FakeQuestSessionTransaction(completionRepository), TestAppClock(NOW))

        val result = useCase()

        val completion = (result as DataResult.Success).value
        assertEquals(NOW, completion.completedAt)
    }

    /**
     * A real "the active session changed between this use case's own read and its eventual
     * commit" race can't be reproduced against a single-threaded fake — there is no suspension
     * point between [CompleteQuest]'s [com.togetherly.domain.completion.repository.CompletionRepository.getActiveSession]
     * read and its [QuestSessionTransaction.completeActiveSession] call for another caller to act
     * in. That race, and the transactional re-check that closes it, are verified for real against
     * genuine concurrent Room transactions in `RoomQuestSessionTransactionTest`
     * (`completeActiveSessionReturnsMismatchWhenADifferentSessionIsActive`,
     * `twoSimultaneousStartAttemptsOnlyOneSucceeds`). What *is* a unit-testable use-case-level
     * concern is narrower: that [CompleteQuest] faithfully surfaces whatever
     * [QuestSessionTransaction.completeActiveSession] returns, rather than swallowing or
     * reinterpreting a mismatch — this stubs the transaction directly to prove exactly that.
     */
    @Test
    fun aQuestSessionTransactionMismatchIsSurfacedAsATypedError() = runTest {
        val session = validActiveQuestSession(startedAt = NOW)
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(session) }
        val mismatchError = AppError.Validation(ValidationError.ACTIVE_SESSION_MISMATCH)
        val questSessionTransaction = object : QuestSessionTransaction {
            override suspend fun startActiveSession(session: ActiveQuestSession): DataResult<Unit> =
                DataResult.Success(Unit)

            override suspend fun completeActiveSession(completion: QuestCompletion): DataResult<Unit> =
                DataResult.Error(mismatchError)

            override suspend fun replaceActiveSession(session: ActiveQuestSession): DataResult<Unit> =
                DataResult.Success(Unit)
        }
        val useCase = CompleteQuest(completionRepository, questSessionTransaction, TestAppClock(NOW))

        val result = useCase()

        assertEquals(DataResult.Error(mismatchError), result)
        // CompleteQuest never clears the active session itself — only the (here, stubbed-out)
        // transaction does, and it never ran its own clear because it returned an error.
        assertEquals(DataResult.Success(session), completionRepository.getActiveSession())
    }
}

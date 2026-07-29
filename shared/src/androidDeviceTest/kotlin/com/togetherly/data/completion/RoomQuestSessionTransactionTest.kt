package com.togetherly.data.completion

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.data.local.completion.CompletionDao
import com.togetherly.data.local.completion.CompletionReactionEntity
import com.togetherly.data.local.completion.MemoryMediaEntity
import com.togetherly.data.local.completion.QuestCompletionEntity
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.data.local.database.buildTogetherlyDatabase
import com.togetherly.data.local.mapper.ActiveQuestSessionMapper
import com.togetherly.data.local.mapper.QuestCompletionMapper
import com.togetherly.data.testActiveQuestSession
import com.togetherly.data.testPhotoMedia
import com.togetherly.data.testQuestCompletion
import com.togetherly.data.testVoiceMedia
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.FamilyReaction
import com.togetherly.domain.completion.repository.QuestSessionTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
internal class RoomQuestSessionTransactionTest {

    private lateinit var database: TogetherlyDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = buildTogetherlyDatabase(Room.inMemoryDatabaseBuilder(context, TogetherlyDatabase::class.java))
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun transaction(
        completionDao: CompletionDao = database.completionDao(),
        dispatchers: TestAppDispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
    ): QuestSessionTransaction = RoomQuestSessionTransaction(
        completionDao = completionDao,
        activeQuestSessionMapper = ActiveQuestSessionMapper(),
        completionMapper = QuestCompletionMapper(),
        database = database,
        dispatchers = dispatchers,
    )

    @Test
    fun startSucceedsWhenNoSessionExists() = runTest {
        val session = testActiveQuestSession()

        val result = transaction().startActiveSession(session)

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(session.completionId.value, database.completionDao().getActiveSession(0)?.completionId)
    }

    @Test
    fun startReturnsTypedConflictWhenAnotherSessionExists() = runTest {
        val transaction = transaction()
        transaction.startActiveSession(testActiveQuestSession(completionId = CompletionId("completion-a")))

        val result = transaction.startActiveSession(testActiveQuestSession(completionId = CompletionId("completion-b")))

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.ACTIVE_SESSION_CONFLICT)), result)
        assertEquals("completion-a", database.completionDao().getActiveSession(0)?.completionId)
    }

    @Test
    fun startingTheSameSessionTwiceIsIdempotent() = runTest {
        val transaction = transaction()
        val session = testActiveQuestSession()
        transaction.startActiveSession(session)

        val result = transaction.startActiveSession(session)

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(session.completionId.value, database.completionDao().getActiveSession(0)?.completionId)
    }

    @Test
    fun completeActiveSessionSavesCompletionAndClearsSessionAtomically() = runTest {
        val transaction = transaction()
        val session = testActiveQuestSession(completionId = CompletionId("completion-1"))
        transaction.startActiveSession(session)
        val completion = testQuestCompletion(id = CompletionId("completion-1"))

        val result = transaction.completeActiveSession(completion)

        assertEquals(DataResult.Success(Unit), result)
        assertNull(database.completionDao().getActiveSession(0))
        assertEquals("completion-1", database.completionDao().getCompletion("completion-1")?.completion?.id)
    }

    @Test
    fun completeActiveSessionReturnsMismatchWhenNoSessionIsActive() = runTest {
        val completion = testQuestCompletion(id = CompletionId("completion-1"))

        val result = transaction().completeActiveSession(completion)

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.ACTIVE_SESSION_MISMATCH)), result)
        assertNull(database.completionDao().getCompletion("completion-1"))
    }

    @Test
    fun completeActiveSessionReturnsMismatchWhenADifferentSessionIsActive() = runTest {
        val transaction = transaction()
        transaction.startActiveSession(testActiveQuestSession(completionId = CompletionId("completion-current")))
        val staleCompletion = testQuestCompletion(id = CompletionId("completion-stale"))

        val result = transaction.completeActiveSession(staleCompletion)

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.ACTIVE_SESSION_MISMATCH)), result)
        assertEquals("completion-current", database.completionDao().getActiveSession(0)?.completionId)
        assertNull(database.completionDao().getCompletion("completion-stale"))
    }

    @Test
    fun replaceActiveSessionOverwritesAnExistingSession() = runTest {
        val transaction = transaction()
        transaction.startActiveSession(testActiveQuestSession(completionId = CompletionId("completion-a")))

        val result = transaction.replaceActiveSession(testActiveQuestSession(completionId = CompletionId("completion-b")))

        assertEquals(DataResult.Success(Unit), result)
        assertEquals("completion-b", database.completionDao().getActiveSession(0)?.completionId)
    }

    @Test
    fun replaceActiveSessionSucceedsWithNoPriorSession() = runTest {
        val result = transaction().replaceActiveSession(testActiveQuestSession(completionId = CompletionId("completion-a")))

        assertEquals(DataResult.Success(Unit), result)
        assertEquals("completion-a", database.completionDao().getActiveSession(0)?.completionId)
    }

    /**
     * Two [startActiveSession][QuestSessionTransaction.startActiveSession] calls launched at the
     * same time, on a *real* multithreaded dispatcher rather than a test-virtual one — genuine
     * concurrency, not just interleaved suspension points. SQLite allows one writer at a time and
     * Room's writer connection serializes every `immediateTransaction` onto it, so exactly one of
     * the two check-then-insert transactions runs to completion before the other's check even
     * starts; the outcome is deterministic every run, never a race that occasionally lets both
     * "win".
     */
    @Test
    fun twoSimultaneousStartAttemptsOnlyOneSucceeds() = runTest {
        val transaction = transaction(dispatchers = TestAppDispatchers(Dispatchers.Default))
        val sessionA = testActiveQuestSession(completionId = CompletionId("completion-a"))
        val sessionB = testActiveQuestSession(completionId = CompletionId("completion-b"))

        val results = coroutineScope {
            val a = async { transaction.startActiveSession(sessionA) }
            val b = async { transaction.startActiveSession(sessionB) }
            listOf(a.await(), b.await())
        }

        assertEquals(1, results.count { it == DataResult.Success(Unit) })
        assertEquals(
            1,
            results.count { it == DataResult.Error(AppError.Validation(ValidationError.ACTIVE_SESSION_CONFLICT)) },
        )
    }

    /**
     * Forces the completion-save step to throw inside an otherwise-real transaction. The whole
     * `immediateTransaction` block rolls back on any exception, so the active session this test
     * started beforehand is left exactly as it was — the same rollback guarantee that would cover
     * a hypothetical failure in the later clear-active-session step too, since either failure
     * aborts the same single transaction.
     */
    @Test
    fun aFailureWhileSavingTheCompletionLeavesTheActiveSessionUntouched() = runTest {
        val session = testActiveQuestSession(completionId = CompletionId("completion-1"))
        transaction().startActiveSession(session)

        val failingTransaction = transaction(completionDao = FailingReplaceCompletionDao(database.completionDao()))
        val completion = testQuestCompletion(id = CompletionId("completion-1"))

        val result = failingTransaction.completeActiveSession(completion)

        assert(result is DataResult.Error) { "expected a storage error, got $result" }
        assertEquals("completion-1", database.completionDao().getActiveSession(0)?.completionId)
        assertNull(database.completionDao().getCompletion("completion-1"))
    }

    /**
     * A completion with several reactions and both media items is the meaningful case: if
     * [CompletionDao.replaceCompletion]'s three writes (primary row, reactions, media) were ever
     * individually visible to an observer instead of committing as one unit, this completion
     * would momentarily appear with only *some* of its reactions/media — a "partially written
     * completion" in exactly the sense the transactional boundary exists to prevent. The
     * [com.togetherly.data.local.completion.CompletionDao.observeCompletion] query is itself
     * `@Transaction`-annotated and only re-runs after the whole write transaction commits, so the
     * one post-write emission here is always the complete completion, never a partial one.
     *
     * This deliberately does *not* assert anything about the relative ordering between this flow
     * and [com.togetherly.data.local.completion.CompletionDao.observeActiveSession]'s own flow:
     * Room invalidates and re-queries each observed query independently, so two *separate* flows
     * touched by the same transaction are not guaranteed to emit their updated results in the
     * same tick, even though the underlying write was atomic — confirmed empirically while writing
     * this test. That is a property of combining independent `Flow`s, not a persistence-layer
     * consistency bug: [completeActiveSessionSavesCompletionAndClearsSessionAtomically] already
     * proves both changes are actually applied together by reading both directly after the call.
     */
    @Test
    fun observersNeverSeeAPartiallyWrittenCompletion() = runTest {
        val transaction = transaction()
        val session = testActiveQuestSession(completionId = CompletionId("completion-1"))
        transaction.startActiveSession(session)
        val completion = testQuestCompletion(
            id = CompletionId("completion-1"),
            reactions = setOf(FamilyReaction.HAPPY, FamilyReaction.SILLY, FamilyReaction.LOVED_IT),
            media = listOf(testPhotoMedia(), testVoiceMedia()),
        )

        database.completionDao().observeCompletion("completion-1").test {
            assertNull(awaitItem())

            transaction.completeActiveSession(completion)

            val saved = awaitItem()
            assertEquals(3, saved?.reactions?.size)
            assertEquals(2, saved?.media?.size)
        }
    }

    private class FailingReplaceCompletionDao(
        private val delegate: CompletionDao,
    ) : CompletionDao by delegate {
        override suspend fun replaceCompletion(
            completion: QuestCompletionEntity,
            reactions: List<CompletionReactionEntity>,
            media: List<MemoryMediaEntity>,
        ) {
            throw IllegalStateException("simulated completion save failure")
        }
    }
}

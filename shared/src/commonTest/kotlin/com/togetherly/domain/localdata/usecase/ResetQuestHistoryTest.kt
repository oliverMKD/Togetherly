package com.togetherly.domain.localdata.usecase

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.repository.FakePrivateMediaCommitter
import com.togetherly.domain.completion.validQuestCompletion
import com.togetherly.domain.family.repository.FakeQuestHistoryCleaner
import com.togetherly.domain.family.repository.QuestHistoryCleaner
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private val TEST_PHOTO: MemoryMedia = MemoryMedia.Photo(id = MemoryMediaId("photo-1"), localReference = MediaReference("ref-photo"))

class ResetQuestHistoryTest {

    private fun useCase(
        completionRepository: FakeCompletionRepository = FakeCompletionRepository(),
        questHistoryCleaner: FakeQuestHistoryCleaner = FakeQuestHistoryCleaner(),
        mediaCommitter: FakePrivateMediaCommitter = FakePrivateMediaCommitter(),
    ) = ResetQuestHistory(completionRepository, questHistoryCleaner, mediaCommitter)

    @Test
    fun deletesEveryCompletionsMediaFilesAndResetsQuestHistory() = runTest {
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(validQuestCompletion(media = listOf(TEST_PHOTO)))
        }
        val questHistoryCleaner = FakeQuestHistoryCleaner()
        val mediaCommitter = FakePrivateMediaCommitter()

        val result = useCase(completionRepository, questHistoryCleaner, mediaCommitter)()

        assertEquals(DataResult.Success(0), result)
        assertEquals(1, questHistoryCleaner.resetCallCount)
        assertEquals(listOf(TEST_PHOTO), mediaCommitter.deleteCommittedCalls)
    }

    @Test
    fun resettingDatabaseFailureShortCircuitsBeforeAnyFileDeletion() = runTest {
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(validQuestCompletion(media = listOf(TEST_PHOTO)))
        }
        val questHistoryCleaner = FakeQuestHistoryCleaner().apply { setNextError(AppError.Storage(StorageError.DELETE_FAILED)) }
        val mediaCommitter = FakePrivateMediaCommitter()

        val result = useCase(completionRepository, questHistoryCleaner, mediaCommitter)()

        assertIs<DataResult.Error>(result)
        assertTrue(mediaCommitter.deleteCommittedCalls.isEmpty())
    }

    @Test
    fun aFailedFileDeletionIsCountedButStillReportsOverallSuccess() = runTest {
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(validQuestCompletion(media = listOf(TEST_PHOTO)))
        }
        val mediaCommitter = FakePrivateMediaCommitter().apply {
            setDeleteCommittedError(AppError.Storage(StorageError.DELETE_FAILED))
        }

        val result = useCase(completionRepository, mediaCommitter = mediaCommitter)()

        assertEquals(DataResult.Success(1), result)
    }

    @Test
    fun withNoCompletionsStillSucceedsWithZeroFailures() = runTest {
        assertEquals(DataResult.Success(0), useCase()())
    }

    @Test
    fun concurrentInvocationIsRejected() = runTest {
        val slowCleaner = object : QuestHistoryCleaner {
            override suspend fun resetQuestHistory(): DataResult<Unit> {
                delay(50)
                return DataResult.Success(Unit)
            }
        }
        val resetQuestHistory = ResetQuestHistory(FakeCompletionRepository(), slowCleaner, FakePrivateMediaCommitter())

        val results = listOf(
            async { resetQuestHistory() },
            async { resetQuestHistory() },
        ).awaitAll()

        assertEquals(1, results.count { it is DataResult.Error })
        val rejected = results.first { it is DataResult.Error } as DataResult.Error
        assertEquals(AppError.Validation(ValidationError.INVALID_STATE), rejected.error)
    }
}

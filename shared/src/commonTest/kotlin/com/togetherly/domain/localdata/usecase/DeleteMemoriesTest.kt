package com.togetherly.domain.localdata.usecase

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.repository.FakeMemoryCleaner
import com.togetherly.domain.completion.repository.FakePrivateMediaCommitter
import com.togetherly.domain.completion.repository.MemoryCleaner
import com.togetherly.domain.completion.validQuestCompletion
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private val TEST_PHOTO: MemoryMedia = MemoryMedia.Photo(id = MemoryMediaId("photo-1"), localReference = MediaReference("ref-photo"))

class DeleteMemoriesTest {

    private fun useCase(
        completionRepository: FakeCompletionRepository = FakeCompletionRepository(),
        memoryCleaner: FakeMemoryCleaner = FakeMemoryCleaner(),
        mediaCommitter: FakePrivateMediaCommitter = FakePrivateMediaCommitter(),
    ) = DeleteMemories(completionRepository, memoryCleaner, mediaCommitter)

    @Test
    fun deletesEveryCompletionsMediaFilesAndClearsMemoryContent() = runTest {
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(validQuestCompletion(media = listOf(TEST_PHOTO)))
        }
        val memoryCleaner = FakeMemoryCleaner()
        val mediaCommitter = FakePrivateMediaCommitter()

        val result = useCase(completionRepository, memoryCleaner, mediaCommitter)()

        assertEquals(DataResult.Success(0), result)
        assertEquals(1, memoryCleaner.clearCallCount)
        assertEquals(listOf(TEST_PHOTO), mediaCommitter.deleteCommittedCalls)
    }

    @Test
    fun clearingDatabaseFailureShortCircuitsBeforeAnyFileDeletion() = runTest {
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(validQuestCompletion(media = listOf(TEST_PHOTO)))
        }
        val memoryCleaner = FakeMemoryCleaner().apply { setNextError(AppError.Storage(StorageError.DELETE_FAILED)) }
        val mediaCommitter = FakePrivateMediaCommitter()

        val result = useCase(completionRepository, memoryCleaner, mediaCommitter)()

        assertIs<DataResult.Error>(result)
        assertTrue(mediaCommitter.deleteCommittedCalls.isEmpty(), "No file should be touched once the database step fails")
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
        val result = useCase()()

        assertEquals(DataResult.Success(0), result)
    }

    @Test
    fun concurrentInvocationIsRejected() = runTest {
        val slowMemoryCleaner = object : MemoryCleaner {
            override suspend fun clearAllMemoryContent(): DataResult<Unit> {
                delay(50)
                return DataResult.Success(Unit)
            }
        }
        val deleteMemories = DeleteMemories(FakeCompletionRepository(), slowMemoryCleaner, FakePrivateMediaCommitter())

        val results = listOf(
            async { deleteMemories() },
            async { deleteMemories() },
        ).awaitAll()

        val errorCount = results.count { it is DataResult.Error }
        assertEquals(1, errorCount, "Exactly one concurrent call must be rejected")
        val rejected = results.first { it is DataResult.Error } as DataResult.Error
        assertEquals(AppError.Validation(ValidationError.INVALID_STATE), rejected.error)
    }
}

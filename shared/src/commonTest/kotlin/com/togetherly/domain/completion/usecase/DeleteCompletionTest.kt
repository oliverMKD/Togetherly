package com.togetherly.domain.completion.usecase

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.repository.FakePrivateMediaCommitter
import com.togetherly.domain.completion.validQuestCompletion
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class DeleteCompletionTest {

    @Test
    fun deletesTheCompletionRecord() = runTest {
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(validQuestCompletion(id = CompletionId("c1")))
        }
        val mediaCommitter = FakePrivateMediaCommitter()
        val useCase = DeleteCompletion(completionRepository, mediaCommitter)

        val result = useCase(CompletionId("c1"))

        assertTrue(result is DataResult.Success)
        assertEquals(null, (completionRepository.getCompletion(CompletionId("c1")) as DataResult.Success).value)
    }

    @Test
    fun deletesCommittedPhotoAndVoiceFiles() = runTest {
        val photo = MemoryMedia.Photo(MemoryMediaId("media-1"), MediaReference("completions/c1/photo-1.jpg"))
        val voice = MemoryMedia.Voice(MemoryMediaId("media-2"), MediaReference("completions/c1/voice-1.m4a"), 8.seconds)
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(validQuestCompletion(id = CompletionId("c1"), media = listOf(photo, voice)))
        }
        val mediaCommitter = FakePrivateMediaCommitter()
        val useCase = DeleteCompletion(completionRepository, mediaCommitter)

        useCase(CompletionId("c1"))

        assertEquals(listOf(photo, voice), mediaCommitter.deleteCommittedCalls)
    }

    @Test
    fun completionWithoutMemoryDeletesCleanlyWithNoMediaCalls() = runTest {
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(validQuestCompletion(id = CompletionId("c1")))
        }
        val mediaCommitter = FakePrivateMediaCommitter()
        val useCase = DeleteCompletion(completionRepository, mediaCommitter)

        val result = useCase(CompletionId("c1"))

        assertTrue(result is DataResult.Success)
        assertTrue(mediaCommitter.deleteCommittedCalls.isEmpty())
    }

    @Test
    fun unrelatedCompletionsAndTheirFilesAreUntouched() = runTest {
        val keptPhoto = MemoryMedia.Photo(MemoryMediaId("media-kept"), MediaReference("completions/c2/photo-1.jpg"))
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(validQuestCompletion(id = CompletionId("c1")))
            saveCompletion(validQuestCompletion(id = CompletionId("c2"), media = listOf(keptPhoto)))
        }
        val mediaCommitter = FakePrivateMediaCommitter()
        val useCase = DeleteCompletion(completionRepository, mediaCommitter)

        useCase(CompletionId("c1"))

        assertEquals(emptyList(), mediaCommitter.deleteCommittedCalls)
        val remaining = (completionRepository.getCompletion(CompletionId("c2")) as DataResult.Success).value
        assertEquals(keptPhoto, remaining?.media?.single())
    }

    @Test
    fun repositoryGetFailurePropagatesAndSkipsDeletion() = runTest {
        val completionRepository = FakeCompletionRepository()
        completionRepository.setNextError(AppError.Storage(StorageError.READ_FAILED))
        val mediaCommitter = FakePrivateMediaCommitter()
        val useCase = DeleteCompletion(completionRepository, mediaCommitter)

        val result = useCase(CompletionId("missing"))

        assertEquals(AppError.Storage(StorageError.READ_FAILED), (result as DataResult.Error).error)
        assertTrue(mediaCommitter.deleteCommittedCalls.isEmpty())
    }

    @Test
    fun deleteCompletionFailurePreventsMediaDeletion() = runTest {
        val photo = MemoryMedia.Photo(MemoryMediaId("media-1"), MediaReference("completions/c1/photo-1.jpg"))
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(validQuestCompletion(id = CompletionId("c1"), media = listOf(photo)))
        }
        completionRepository.setDeleteCompletionError(AppError.Storage(StorageError.WRITE_FAILED))
        val mediaCommitter = FakePrivateMediaCommitter()
        val useCase = DeleteCompletion(completionRepository, mediaCommitter)

        val result = useCase(CompletionId("c1"))

        assertTrue(result is DataResult.Error)
        assertTrue(mediaCommitter.deleteCommittedCalls.isEmpty())
    }
}

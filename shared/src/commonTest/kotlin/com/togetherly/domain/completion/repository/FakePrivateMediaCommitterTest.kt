package com.togetherly.domain.completion.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.PendingMediaReference
import com.togetherly.domain.completion.PendingVoiceReference
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

private val COMPLETION_ID = CompletionId("completion-1")
private val MEDIA_ID = MemoryMediaId("media-1")

class FakePrivateMediaCommitterTest {

    @Test
    fun commitPhotoSucceedsAndRecordsTheCall() = runTest {
        val committer = FakePrivateMediaCommitter()

        val result = committer.commitPhoto(PendingMediaReference("pending-photo"), COMPLETION_ID, MEDIA_ID)

        val photo = (result as DataResult.Success).value
        assertEquals(MEDIA_ID, photo.id)
        assertEquals(listOf(photo), committer.committedPhotoCalls)
    }

    @Test
    fun commitVoiceSucceedsAndPreservesDuration() = runTest {
        val committer = FakePrivateMediaCommitter()
        val pending = PendingVoiceReference(PendingMediaReference("pending-voice"), 12.seconds)

        val result = committer.commitVoice(pending, COMPLETION_ID, MEDIA_ID)

        val voice = (result as DataResult.Success).value
        assertEquals(12.seconds, voice.duration)
    }

    @Test
    fun configuredCommitPhotoErrorIsConsumedOnce() = runTest {
        val committer = FakePrivateMediaCommitter()
        val error = AppError.Storage(StorageError.WRITE_FAILED)
        committer.setCommitPhotoError(error)

        val first = committer.commitPhoto(PendingMediaReference("pending-photo"), COMPLETION_ID, MEDIA_ID)
        val second = committer.commitPhoto(PendingMediaReference("pending-photo"), COMPLETION_ID, MEDIA_ID)

        assertEquals(DataResult.Error(error), first)
        assertEquals(true, second is DataResult.Success)
    }

    @Test
    fun throwOnNextCommitVoiceThrowsInsteadOfReturningAResult() = runTest {
        val committer = FakePrivateMediaCommitter()
        committer.throwOnNextCommitVoice(kotlinx.coroutines.CancellationException("cancelled"))

        assertFailsWith<kotlinx.coroutines.CancellationException> {
            committer.commitVoice(PendingVoiceReference(PendingMediaReference("pending-voice"), 1.seconds), COMPLETION_ID, MEDIA_ID)
        }
    }

    @Test
    fun deleteCommittedAndDeletePendingRecordTheirCalls() = runTest {
        val committer = FakePrivateMediaCommitter()
        val photo = MemoryMedia.Photo(MEDIA_ID, com.togetherly.domain.completion.MediaReference("ref"))
        val pending = PendingMediaReference("pending-photo")

        committer.deleteCommitted(photo)
        committer.deletePending(pending)

        assertEquals(listOf<MemoryMedia>(photo), committer.deleteCommittedCalls)
        assertEquals(listOf(pending), committer.deletePendingCalls)
    }
}

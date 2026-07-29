package com.togetherly.data.media

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.PendingMediaReference
import com.togetherly.domain.completion.PendingVoiceReference
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

private val COMPLETION_ID = CompletionId("completion-1")
private val MEDIA_ID = MemoryMediaId("media-1")

class PrivateMediaCommitterImplTest {

    @Test
    fun commitPhotoDelegatesAndWrapsIntoDomainMedia() = runTest {
        val storage = FakePrivateMediaStorage()
        val committer = PrivateMediaCommitterImpl(storage)

        val result = committer.commitPhoto(PendingMediaReference("pending-photo"), COMPLETION_ID, MEDIA_ID)

        val photo = (result as DataResult.Success).value
        assertEquals(MEDIA_ID, photo.id)
        assertEquals(MediaReference("completions/completion-1/photo-media-1.jpg"), photo.localReference)
        assertEquals(listOf(PendingMediaReference("pending-photo")), storage.commitPhotoCalls)
    }

    @Test
    fun commitPhotoSurfacesAStorageFailure() = runTest {
        val storage = FakePrivateMediaStorage()
        val error = AppError.Storage(StorageError.WRITE_FAILED)
        storage.nextCommitPhotoResult = DataResult.Error(error)
        val committer = PrivateMediaCommitterImpl(storage)

        val result = committer.commitPhoto(PendingMediaReference("pending-photo"), COMPLETION_ID, MEDIA_ID)

        assertEquals(DataResult.Error(error), result)
    }

    @Test
    fun commitVoiceDelegatesAndPreservesDuration() = runTest {
        val storage = FakePrivateMediaStorage()
        val committer = PrivateMediaCommitterImpl(storage)
        val pendingVoice = PendingVoiceReference(PendingMediaReference("pending-voice"), 5.seconds)

        val result = committer.commitVoice(pendingVoice, COMPLETION_ID, MEDIA_ID)

        val voice = (result as DataResult.Success).value
        assertEquals(MEDIA_ID, voice.id)
        assertEquals(5.seconds, voice.duration)
        assertEquals(MediaReference("completions/completion-1/voice-media-1.m4a"), voice.localReference)
        assertEquals(listOf(PendingMediaReference("pending-voice")), storage.commitVoiceCalls)
    }

    @Test
    fun commitVoiceSurfacesAStorageFailure() = runTest {
        val storage = FakePrivateMediaStorage()
        val error = AppError.Storage(StorageError.WRITE_FAILED)
        storage.nextCommitVoiceResult = DataResult.Error(error)
        val committer = PrivateMediaCommitterImpl(storage)
        val pendingVoice = PendingVoiceReference(PendingMediaReference("pending-voice"), 5.seconds)

        val result = committer.commitVoice(pendingVoice, COMPLETION_ID, MEDIA_ID)

        assertEquals(DataResult.Error(error), result)
    }

    @Test
    fun deleteCommittedDelegatesForPhoto() = runTest {
        val storage = FakePrivateMediaStorage()
        val committer = PrivateMediaCommitterImpl(storage)
        val photo = MemoryMedia.Photo(MEDIA_ID, MediaReference("completions/completion-1/photo-media-1.jpg"))

        val result = committer.deleteCommitted(photo)

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(listOf(photo.localReference), storage.deleteCommittedCalls)
    }

    @Test
    fun deleteCommittedDelegatesForVoice() = runTest {
        val storage = FakePrivateMediaStorage()
        val committer = PrivateMediaCommitterImpl(storage)
        val voice = MemoryMedia.Voice(MEDIA_ID, MediaReference("completions/completion-1/voice-media-1.m4a"), 5.seconds)

        val result = committer.deleteCommitted(voice)

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(listOf(voice.localReference), storage.deleteCommittedCalls)
    }

    @Test
    fun deletePendingDelegates() = runTest {
        val storage = FakePrivateMediaStorage()
        val committer = PrivateMediaCommitterImpl(storage)
        val pending = PendingMediaReference("pending-photo")

        val result = committer.deletePending(pending)

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(listOf(pending), storage.deletePendingCalls)
    }
}

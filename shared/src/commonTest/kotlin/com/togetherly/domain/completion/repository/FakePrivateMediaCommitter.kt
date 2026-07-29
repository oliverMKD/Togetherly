package com.togetherly.domain.completion.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.PendingMediaReference
import com.togetherly.domain.completion.PendingVoiceReference

class FakePrivateMediaCommitter : PrivateMediaCommitter {

    val committedPhotoCalls = mutableListOf<MemoryMedia.Photo>()
    val committedVoiceCalls = mutableListOf<MemoryMedia.Voice>()
    val deleteCommittedCalls = mutableListOf<MemoryMedia>()
    val deletePendingCalls = mutableListOf<PendingMediaReference>()

    private var commitPhotoError: AppError? = null
    private var commitVoiceError: AppError? = null
    private var commitVoiceThrowable: Throwable? = null
    private var deleteCommittedError: AppError? = null
    private var deletePendingError: AppError? = null

    fun setCommitPhotoError(error: AppError) {
        commitPhotoError = error
    }

    fun setCommitVoiceError(error: AppError) {
        commitVoiceError = error
    }

    /**
     * Simulates coroutine cancellation reaching the committer mid-commit — a returned
     * [DataResult.Error] can't stand in for a thrown [kotlinx.coroutines.CancellationException].
     */
    fun throwOnNextCommitVoice(throwable: Throwable) {
        commitVoiceThrowable = throwable
    }

    fun setDeleteCommittedError(error: AppError) {
        deleteCommittedError = error
    }

    fun setDeletePendingError(error: AppError) {
        deletePendingError = error
    }

    override suspend fun commitPhoto(
        pendingReference: PendingMediaReference,
        completionId: CompletionId,
        mediaId: MemoryMediaId,
    ): DataResult<MemoryMedia.Photo> {
        commitPhotoError?.let {
            commitPhotoError = null
            return DataResult.Error(it)
        }
        val media = MemoryMedia.Photo(id = mediaId, localReference = MediaReference("photo-${mediaId.value}"))
        committedPhotoCalls += media
        return DataResult.Success(media)
    }

    override suspend fun commitVoice(
        pendingReference: PendingVoiceReference,
        completionId: CompletionId,
        mediaId: MemoryMediaId,
    ): DataResult<MemoryMedia.Voice> {
        commitVoiceThrowable?.let {
            commitVoiceThrowable = null
            throw it
        }
        commitVoiceError?.let {
            commitVoiceError = null
            return DataResult.Error(it)
        }
        val media = MemoryMedia.Voice(
            id = mediaId,
            localReference = MediaReference("voice-${mediaId.value}"),
            duration = pendingReference.duration,
        )
        committedVoiceCalls += media
        return DataResult.Success(media)
    }

    override suspend fun deleteCommitted(media: MemoryMedia): DataResult<Unit> {
        deleteCommittedCalls += media
        deleteCommittedError?.let {
            deleteCommittedError = null
            return DataResult.Error(it)
        }
        return DataResult.Success(Unit)
    }

    override suspend fun deletePending(reference: PendingMediaReference): DataResult<Unit> {
        deletePendingCalls += reference
        deletePendingError?.let {
            deletePendingError = null
            return DataResult.Error(it)
        }
        return DataResult.Success(Unit)
    }
}

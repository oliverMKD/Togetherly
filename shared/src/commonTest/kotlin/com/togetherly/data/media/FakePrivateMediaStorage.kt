package com.togetherly.data.media

import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.PendingMediaReference
import kotlin.time.Duration

class FakePrivateMediaStorage : PrivateMediaStorage {

    val commitPhotoCalls = mutableListOf<PendingMediaReference>()
    val commitVoiceCalls = mutableListOf<PendingMediaReference>()
    val deleteCommittedCalls = mutableListOf<MediaReference>()
    val deletePendingCalls = mutableListOf<PendingMediaReference>()

    var nextCommitPhotoResult: DataResult<CommittedPhoto>? = null
    var nextCommitVoiceResult: DataResult<CommittedVoice>? = null
    var nextDeleteCommittedResult: DataResult<Unit> = DataResult.Success(Unit)
    var nextDeletePendingResult: DataResult<Unit> = DataResult.Success(Unit)

    override suspend fun createPendingPhoto(source: PhotoImportSource): DataResult<PendingPhoto> =
        DataResult.Error(AppError.Unexpected())

    override suspend fun commitPhoto(
        pending: PendingMediaReference,
        completionId: CompletionId,
        mediaId: MemoryMediaId,
    ): DataResult<CommittedPhoto> {
        commitPhotoCalls += pending
        return nextCommitPhotoResult ?: DataResult.Success(
            CommittedPhoto(
                reference = MediaReference("completions/${completionId.value}/photo-${mediaId.value}.jpg"),
                thumbnailReference = MediaReference("completions/${completionId.value}/thumb-${mediaId.value}.jpg"),
                width = 100,
                height = 200,
                sizeBytes = 42L,
            ),
        )
    }

    override suspend fun commitVoice(
        pending: PendingMediaReference,
        completionId: CompletionId,
        mediaId: MemoryMediaId,
        duration: Duration,
    ): DataResult<CommittedVoice> {
        commitVoiceCalls += pending
        return nextCommitVoiceResult ?: DataResult.Success(
            CommittedVoice(
                reference = MediaReference("completions/${completionId.value}/voice-${mediaId.value}.m4a"),
                duration = duration,
                sizeBytes = 42L,
            ),
        )
    }

    override suspend fun deletePending(reference: PendingMediaReference): DataResult<Unit> {
        deletePendingCalls += reference
        return nextDeletePendingResult
    }

    override suspend fun deleteCommitted(reference: MediaReference): DataResult<Unit> {
        deleteCommittedCalls += reference
        return nextDeleteCommittedResult
    }

    override suspend fun openPhoto(reference: MediaReference): DataResult<PrivatePhotoData> =
        DataResult.Error(AppError.Unexpected())

    override suspend fun openPendingPhoto(reference: PendingMediaReference): DataResult<PrivatePhotoData> =
        DataResult.Error(AppError.Unexpected())
}

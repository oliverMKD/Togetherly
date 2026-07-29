package com.togetherly.data.media

import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.PendingMediaReference
import com.togetherly.domain.completion.PendingVoiceReference
import com.togetherly.domain.completion.repository.PrivateMediaCommitter

/**
 * Bridges the domain-level [PrivateMediaCommitter] contract onto the data-layer [PrivateMediaStorage] —
 * this is the only place a [CommittedPhoto]/[CommittedVoice] data-layer type gets translated into
 * the domain's [MemoryMedia]. [deletePending] and the [MemoryMedia]-agnostic half of
 * [deleteCommitted] both delegate straight through since [PrivateMediaStorage]'s own delete methods
 * are already reference-agnostic (see that interface's own KDoc).
 */
class PrivateMediaCommitterImpl(
    private val mediaStorage: PrivateMediaStorage,
) : PrivateMediaCommitter {

    override suspend fun commitPhoto(
        pendingReference: PendingMediaReference,
        completionId: CompletionId,
        mediaId: MemoryMediaId,
    ): DataResult<MemoryMedia.Photo> = when (val result = mediaStorage.commitPhoto(pendingReference, completionId, mediaId)) {
        is DataResult.Error -> result
        is DataResult.Success -> DataResult.Success(
            MemoryMedia.Photo(id = mediaId, localReference = result.value.reference),
        )
    }

    override suspend fun commitVoice(
        pendingReference: PendingVoiceReference,
        completionId: CompletionId,
        mediaId: MemoryMediaId,
    ): DataResult<MemoryMedia.Voice> = when (
        val result = mediaStorage.commitVoice(pendingReference.reference, completionId, mediaId, pendingReference.duration)
    ) {
        is DataResult.Error -> result
        is DataResult.Success -> DataResult.Success(
            MemoryMedia.Voice(id = mediaId, localReference = result.value.reference, duration = result.value.duration),
        )
    }

    override suspend fun deleteCommitted(media: MemoryMedia): DataResult<Unit> = when (media) {
        is MemoryMedia.Photo -> mediaStorage.deleteCommitted(media.localReference)
        is MemoryMedia.Voice -> mediaStorage.deleteCommitted(media.localReference)
    }

    override suspend fun deletePending(reference: PendingMediaReference): DataResult<Unit> =
        mediaStorage.deletePending(reference)
}

package com.togetherly.domain.completion.repository

import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.PendingMediaReference
import com.togetherly.domain.completion.PendingVoiceReference

/**
 * Promotes app-private staging files into permanent private storage and removes them again — it
 * never picks, records, or interprets media on its own. Filesystem writes and Room writes cannot
 * share one native transaction, so callers (see `SaveCompletionMemory`) are responsible for
 * compensating cleanup when a commit succeeds but a later step fails; this contract only
 * guarantees each individual operation either fully succeeds or leaves no new permanent file
 * behind.
 */
interface PrivateMediaCommitter {

    suspend fun commitPhoto(
        pendingReference: PendingMediaReference,
        completionId: CompletionId,
        mediaId: MemoryMediaId,
    ): DataResult<MemoryMedia.Photo>

    suspend fun commitVoice(
        pendingReference: PendingVoiceReference,
        completionId: CompletionId,
        mediaId: MemoryMediaId,
    ): DataResult<MemoryMedia.Voice>

    suspend fun deleteCommitted(
        media: MemoryMedia,
    ): DataResult<Unit>

    suspend fun deletePending(
        reference: PendingMediaReference,
    ): DataResult<Unit>
}

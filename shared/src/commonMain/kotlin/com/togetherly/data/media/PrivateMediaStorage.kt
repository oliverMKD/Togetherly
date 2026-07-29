package com.togetherly.data.media

import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.PendingMediaReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * Owns app-private photo storage end to end: importing a picker result into a pending file,
 * promoting a pending file into permanent per-completion storage (photo + thumbnail together),
 * and deleting either. Every method runs off the main thread. Stored references are always
 * relative to the private media root — never an absolute path, never a platform URI — and every
 * reference this contract is handed back is validated against path traversal before any
 * filesystem call is made.
 */
interface PrivateMediaStorage {

    suspend fun createPendingPhoto(
        source: PhotoImportSource,
    ): DataResult<PendingPhoto>

    suspend fun commitPhoto(
        pending: PendingMediaReference,
        completionId: CompletionId,
        mediaId: MemoryMediaId,
    ): DataResult<CommittedPhoto>

    /**
     * [duration] is passed through, not re-derived from the file — it's already known precisely
     * from when [VoiceRecorder][com.togetherly.data.media.VoiceRecorder] captured the clip, and
     * extracting audio duration from a container would need a whole extra platform API for no
     * benefit here.
     */
    suspend fun commitVoice(
        pending: PendingMediaReference,
        completionId: CompletionId,
        mediaId: MemoryMediaId,
        duration: Duration,
    ): DataResult<CommittedVoice>

    /** Reference-agnostic: works for a photo, its thumbnail, or a voice file. */
    suspend fun deletePending(
        reference: PendingMediaReference,
    ): DataResult<Unit>

    /** Reference-agnostic: also deletes a photo's thumbnail (see [PrivateMediaPaths.thumbnailReferenceFor]). */
    suspend fun deleteCommitted(
        reference: MediaReference,
    ): DataResult<Unit>

    suspend fun openPhoto(
        reference: MediaReference,
    ): DataResult<PrivatePhotoData>

    /** For previewing a freshly-imported photo before it's committed (e.g. the memory-capture screen). */
    suspend fun openPendingPhoto(
        reference: PendingMediaReference,
    ): DataResult<PrivatePhotoData>

    /**
     * Opens the Journey-sized thumbnail alongside a committed photo, not the full photo itself —
     * the Journey timeline (Step 10.6) must never decode a full-resolution image just to render a
     * small thumbnail. Has a default implementation (not a platform `actual`): [PrivateMediaPaths]
     * is pure string manipulation, so deriving the thumbnail's own [MediaReference] and reusing
     * [openPhoto] to read it needs no platform-specific code at all. Falls back to [reference]
     * itself if it doesn't look like a photo reference this scheme ever produced, so a caller
     * always gets back *something* to display rather than a spurious error.
     */
    suspend fun openPhotoThumbnail(
        reference: MediaReference,
    ): DataResult<PrivatePhotoData> = openPhoto(PrivateMediaPaths.thumbnailReferenceFor(reference) ?: reference)
}

/**
 * A separate, narrower contract from [PrivateMediaStorage] since orphan scanning is a maintenance
 * sweep (e.g. run periodically or at app start), not part of the save/commit/discard pipeline
 * every other method here supports. [thresholdAge] intentionally defaults conservatively — a
 * pending file that's still within an active editing session must never be swept away.
 */
interface PendingMediaOrphanCleaner {
    suspend fun deleteExpiredPending(
        now: Instant,
        thresholdAge: Duration = 24.hours,
    ): DataResult<Int>
}

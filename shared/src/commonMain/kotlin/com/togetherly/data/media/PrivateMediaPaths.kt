package com.togetherly.data.media

import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMediaId

/**
 * Builds and validates the relative paths private media lives at underneath the private media
 * root — pure string manipulation, with no filesystem access of its own, so it's usable from
 * common code and testable without touching a real directory. Every reference this produces is
 * relative to the root (never an absolute path, never a platform URI) and uses only generated
 * IDs — never a family name, quest title, or note content — as the spec for this feature requires.
 */
internal object PrivateMediaPaths {
    const val PENDING_DIRECTORY = "pending"
    private const val COMPLETIONS_DIRECTORY = "completions"
    private const val PHOTO_PREFIX = "photo-"
    private const val THUMBNAIL_PREFIX = "thumb-"
    private const val VOICE_PREFIX = "voice-"
    private const val JPEG_EXTENSION = ".jpg"

    /**
     * AAC audio in an M4A container — the standard, efficient choice on both platforms
     * (`MediaRecorder`'s `MPEG_4`/`AAC` output on Android, `AVAudioRecorder`'s
     * `kAudioFormatMPEG4AAC` on iOS). Both platforms use this same extension; see each
     * `VoiceRecorder` implementation's own KDoc for the encoder configuration.
     */
    private const val VOICE_EXTENSION = ".m4a"

    /** A single path component: non-blank, not `.`/`..`, and free of any separator. */
    fun isSafePathSegment(segment: String): Boolean =
        segment.isNotBlank() && segment != "." && segment != ".." &&
            !segment.contains('/') && !segment.contains('\\')

    /**
     * A full relative reference as it would be stored in [MediaReference]/[com.togetherly.domain.completion.PendingMediaReference] —
     * never absolute, never containing `..`, and made up entirely of safe segments. Used to
     * reject a stored reference before it ever reaches a filesystem call (defense in depth against
     * a corrupted or tampered database row), not just at construction time.
     */
    fun isSafeRelativeReference(reference: String): Boolean {
        if (reference.isBlank() || reference.startsWith("/") || reference.contains('\\')) return false
        val segments = reference.split('/')
        return segments.all { isSafePathSegment(it) }
    }

    fun pendingPhotoRelativeReference(generatedId: String): String {
        require(isSafePathSegment(generatedId)) { "Unsafe pending media id" }
        return "$PENDING_DIRECTORY/$generatedId$JPEG_EXTENSION"
    }

    fun pendingVoiceRelativeReference(generatedId: String): String {
        require(isSafePathSegment(generatedId)) { "Unsafe pending media id" }
        return "$PENDING_DIRECTORY/$VOICE_PREFIX$generatedId$VOICE_EXTENSION"
    }

    fun completionDirectoryRelativeReference(completionId: CompletionId): String {
        require(isSafePathSegment(completionId.value)) { "Unsafe completion id" }
        return "$COMPLETIONS_DIRECTORY/${completionId.value}"
    }

    fun photoRelativeReference(completionId: CompletionId, mediaId: MemoryMediaId): String {
        require(isSafePathSegment(mediaId.value)) { "Unsafe media id" }
        return "${completionDirectoryRelativeReference(completionId)}/$PHOTO_PREFIX${mediaId.value}$JPEG_EXTENSION"
    }

    fun thumbnailRelativeReference(completionId: CompletionId, mediaId: MemoryMediaId): String {
        require(isSafePathSegment(mediaId.value)) { "Unsafe media id" }
        return "${completionDirectoryRelativeReference(completionId)}/$THUMBNAIL_PREFIX${mediaId.value}$JPEG_EXTENSION"
    }

    fun voiceRelativeReference(completionId: CompletionId, mediaId: MemoryMediaId): String {
        require(isSafePathSegment(mediaId.value)) { "Unsafe media id" }
        return "${completionDirectoryRelativeReference(completionId)}/$VOICE_PREFIX${mediaId.value}$VOICE_EXTENSION"
    }

    /**
     * A tiny sidecar next to a pending photo holding its already-known width/height as plain
     * text — read back at commit time instead of decoding the image again purely to learn its
     * dimensions. Never a stored [com.togetherly.domain.completion.MediaReference]/domain
     * reference of its own; it exists only between [PrivateMediaStorage.createPendingPhoto] and
     * [PrivateMediaStorage.commitPhoto] and is deleted alongside whichever pending file it belongs to.
     */
    fun dimensionsSidecarReference(relativeReference: String): String = "$relativeReference.dims"

    /**
     * Derives a committed photo's thumbnail reference from the photo reference itself by naming
     * convention, rather than requiring both to be passed and kept in sync separately — `null`
     * when [photoReference] doesn't look like a `photo-` reference this scheme ever produces
     * (e.g. it's already a thumbnail or a voice reference), so callers never delete the wrong file.
     */
    fun thumbnailReferenceFor(photoReference: MediaReference): MediaReference? {
        val value = photoReference.value
        val lastSlash = value.lastIndexOf('/')
        val directory = if (lastSlash >= 0) value.substring(0, lastSlash + 1) else ""
        val fileName = if (lastSlash >= 0) value.substring(lastSlash + 1) else value
        if (!fileName.startsWith(PHOTO_PREFIX)) return null
        return MediaReference(directory + THUMBNAIL_PREFIX + fileName.removePrefix(PHOTO_PREFIX))
    }
}

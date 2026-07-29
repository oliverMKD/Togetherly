package com.togetherly.data.media

import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMediaId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrivateMediaPathsTest {

    @Test
    fun photoAndThumbnailReferencesShareTheCompletionDirectory() {
        val completionId = CompletionId("completion-1")
        val mediaId = MemoryMediaId("media-1")

        val photo = PrivateMediaPaths.photoRelativeReference(completionId, mediaId)
        val thumbnail = PrivateMediaPaths.thumbnailRelativeReference(completionId, mediaId)

        assertEquals("completions/completion-1/photo-media-1.jpg", photo)
        assertEquals("completions/completion-1/thumb-media-1.jpg", thumbnail)
    }

    @Test
    fun pendingReferenceLivesUnderThePendingDirectory() {
        assertEquals("pending/generated-id.jpg", PrivateMediaPaths.pendingPhotoRelativeReference("generated-id"))
    }

    @Test
    fun thumbnailReferenceForDerivesFromAPhotoReference() {
        val photo = MediaReference("completions/completion-1/photo-media-1.jpg")

        val thumbnail = PrivateMediaPaths.thumbnailReferenceFor(photo)

        assertEquals(MediaReference("completions/completion-1/thumb-media-1.jpg"), thumbnail)
    }

    @Test
    fun thumbnailReferenceForReturnsNullForANonPhotoReference() {
        val voiceLike = MediaReference("completions/completion-1/voice-media-1.aac")

        assertNull(PrivateMediaPaths.thumbnailReferenceFor(voiceLike))
    }

    @Test
    fun safeRelativeReferenceRejectsParentDirectoryTraversal() {
        assertTrue(!PrivateMediaPaths.isSafeRelativeReference("completions/../../etc/passwd"))
        assertTrue(!PrivateMediaPaths.isSafeRelativeReference("../pending/x.jpg"))
    }

    @Test
    fun safeRelativeReferenceRejectsAbsolutePaths() {
        assertTrue(!PrivateMediaPaths.isSafeRelativeReference("/etc/passwd"))
    }

    @Test
    fun safeRelativeReferenceRejectsBlankOrBackslash() {
        assertTrue(!PrivateMediaPaths.isSafeRelativeReference(""))
        assertTrue(!PrivateMediaPaths.isSafeRelativeReference("completions\\completion-1\\photo-1.jpg"))
    }

    @Test
    fun safeRelativeReferenceAcceptsAWellFormedReference() {
        assertTrue(PrivateMediaPaths.isSafeRelativeReference("completions/completion-1/photo-media-1.jpg"))
        assertTrue(PrivateMediaPaths.isSafeRelativeReference("pending/generated-id.jpg"))
    }

    @Test
    fun pathSegmentRejectsSeparatorsAndDotSegments() {
        assertTrue(!PrivateMediaPaths.isSafePathSegment(""))
        assertTrue(!PrivateMediaPaths.isSafePathSegment("."))
        assertTrue(!PrivateMediaPaths.isSafePathSegment(".."))
        assertTrue(!PrivateMediaPaths.isSafePathSegment("a/b"))
        assertTrue(!PrivateMediaPaths.isSafePathSegment("a\\b"))
        assertTrue(PrivateMediaPaths.isSafePathSegment("completion-1"))
    }
}

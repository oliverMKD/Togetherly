package com.togetherly.domain.completion

import com.togetherly.domain.validation.DomainValidationException
import com.togetherly.domain.validation.DomainValidationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class MemoryMediaOperationsTest {

    private val photo = MemoryMedia.Photo(
        id = MemoryMediaId("media-photo-1"),
        localReference = MediaReference("completions/completion-1/photo.jpg"),
    )

    private val voice = MemoryMedia.Voice(
        id = MemoryMediaId("media-voice-1"),
        localReference = MediaReference("completions/completion-1/voice.m4a"),
        duration = 30.seconds,
    )

    @Test
    fun attachingOnePhotoIsAccepted() {
        val completion = validQuestCompletion().attachMedia(photo)

        assertEquals(listOf(photo), completion.media)
    }

    @Test
    fun attachingOneVoiceMemoryIsAccepted() {
        val completion = validQuestCompletion().attachMedia(voice)

        assertEquals(listOf(voice), completion.media)
    }

    @Test
    fun duplicateMediaIdIsRejected() {
        val withPhoto = validQuestCompletion().attachMedia(photo)
        val duplicatePhoto = photo.copy(localReference = MediaReference("completions/completion-1/photo-2.jpg"))

        val exception = assertFailsWith<DomainValidationException> {
            withPhoto.attachMedia(duplicatePhoto)
        }
        assertEquals(DomainValidationReason.DUPLICATE_VALUE, exception.reason)
    }

    @Test
    fun secondPhotoIsRejected() {
        val withPhoto = validQuestCompletion().attachMedia(photo)
        val secondPhoto = MemoryMedia.Photo(
            id = MemoryMediaId("media-photo-2"),
            localReference = MediaReference("completions/completion-1/photo-2.jpg"),
        )

        val exception = assertFailsWith<DomainValidationException> {
            withPhoto.attachMedia(secondPhoto)
        }
        assertEquals(DomainValidationReason.DUPLICATE_VALUE, exception.reason)
    }

    @Test
    fun secondVoiceMemoryIsRejected() {
        val withVoice = validQuestCompletion().attachMedia(voice)
        val secondVoice = MemoryMedia.Voice(
            id = MemoryMediaId("media-voice-2"),
            localReference = MediaReference("completions/completion-1/voice-2.m4a"),
            duration = 10.seconds,
        )

        val exception = assertFailsWith<DomainValidationException> {
            withVoice.attachMedia(secondVoice)
        }
        assertEquals(DomainValidationReason.DUPLICATE_VALUE, exception.reason)
    }

    @Test
    fun invalidVoiceDurationIsRejected() {
        val zero = assertFailsWith<DomainValidationException> {
            MemoryMedia.Voice(
                id = MemoryMediaId("media-voice-1"),
                localReference = MediaReference("completions/completion-1/voice.m4a"),
                duration = Duration.ZERO,
            )
        }
        assertEquals(DomainValidationReason.NON_POSITIVE_VALUE, zero.reason)

        val tooLong = assertFailsWith<DomainValidationException> {
            MemoryMedia.Voice(
                id = MemoryMediaId("media-voice-1"),
                localReference = MediaReference("completions/completion-1/voice.m4a"),
                duration = MemoryMedia.Voice.MAX_DURATION + 1.minutes,
            )
        }
        assertEquals(DomainValidationReason.VALUE_TOO_LONG, tooLong.reason)
    }

    @Test
    fun removingExistingMediaRemovesIt() {
        val withPhoto = validQuestCompletion().attachMedia(photo)

        val withoutPhoto = withPhoto.removeMedia(photo.id)

        assertEquals(emptyList(), withoutPhoto.media)
    }

    @Test
    fun removingUnknownMediaReturnsUnchangedCompletion() {
        val completion = validQuestCompletion().attachMedia(photo)

        val result = completion.removeMedia(MemoryMediaId("unknown-media"))

        assertEquals(completion, result)
    }

    @Test
    fun attachingMediaDoesNotMutateTheOriginalCompletion() {
        val original = validQuestCompletion()

        val updated = original.attachMedia(photo)

        assertEquals(emptyList(), original.media)
        assertEquals(listOf(photo), updated.media)
    }

    @Test
    fun removingMediaDoesNotMutateTheOriginalCompletion() {
        val original = validQuestCompletion().attachMedia(photo)

        val updated = original.removeMedia(photo.id)

        assertEquals(listOf(photo), original.media)
        assertEquals(emptyList(), updated.media)
    }
}

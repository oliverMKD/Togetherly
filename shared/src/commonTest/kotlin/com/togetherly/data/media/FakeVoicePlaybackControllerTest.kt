package com.togetherly.data.media

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.PendingMediaReference
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class FakeVoicePlaybackControllerTest {

    @Test
    fun playRecordsTheReferenceAndReturnsTheConfiguredResult() = runTest {
        val controller = FakeVoicePlaybackController()
        val reference = MediaReference("completions/completion-1/voice-media-1.m4a")

        val result = controller.play(reference)

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(listOf(reference), controller.playCalls)
    }

    @Test
    fun playPendingRecordsTheReference() = runTest {
        val controller = FakeVoicePlaybackController()
        val reference = PendingMediaReference("pending-voice")

        controller.playPending(reference)

        assertEquals(listOf(reference), controller.playPendingCalls)
    }

    @Test
    fun configuredFailureIsReturnedFromEveryOperation() = runTest {
        val controller = FakeVoicePlaybackController()
        val error = AppError.Storage(StorageError.READ_FAILED)
        controller.nextResult = DataResult.Error(error)

        assertEquals(DataResult.Error(error), controller.play(MediaReference("completions/completion-1/voice-media-1.m4a")))
        assertEquals(DataResult.Error(error), controller.pause())
        assertEquals(DataResult.Error(error), controller.stop())
    }

    @Test
    fun observeStateReflectsManuallySetState() = runTest {
        val controller = FakeVoicePlaybackController()

        controller.setState(VoicePlaybackState.Playing(1.seconds, 5.seconds))

        assertEquals(VoicePlaybackState.Playing(1.seconds, 5.seconds), controller.observeState().value)
    }
}

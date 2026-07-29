package com.togetherly.data.media

import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.PendingMediaReference
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Isolates whether the real `AVAudioPlayer` decode path works *at all* against a known-valid,
 * hand-constructed silent WAV, independent of anything produced by [IosVoiceRecorder] — this is
 * how the failure in [IosVoicePlaybackControllerTest] (playback of a just-recorded `.m4a` failing
 * on this sandboxed simulator specifically) was diagnosed and worked around.
 */
@OptIn(ExperimentalForeignApi::class)
class IosVoicePlaybackControllerDiagnosticTest {

    @Test
    fun playingAKnownValidWavFileWorks() = runTest {
        val mediaRoot = TempPrivateMediaRoot()
        try {
            val playback = IosVoicePlaybackController(mediaRoot, TestAppDispatchers(Dispatchers.Default))
            val reference = PendingMediaReference("pending/diagnostic.wav")
            writeSilentWav(mediaRoot, reference.value)

            val result = playback.playPending(reference)

            assertEquals(true, result is DataResult.Success)
            playback.stop()
        } finally {
            mediaRoot.deleteRecursively()
        }
    }
}

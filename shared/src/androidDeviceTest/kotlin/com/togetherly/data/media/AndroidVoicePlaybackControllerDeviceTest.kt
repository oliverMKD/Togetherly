package com.togetherly.data.media

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.datetime.DefaultAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.id.SequentialIdGenerator
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.PendingMediaReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exercises the real `MediaPlayer` pipeline, playing back a clip recorded by
 * [AndroidVoiceRecorder] in the same test. Requires a connected device or emulator with a working
 * audio input/output route to actually run — compile-checked only in this environment.
 */
@RunWith(AndroidJUnit4::class)
class AndroidVoicePlaybackControllerDeviceTest {

    private lateinit var mediaRoot: TempPrivateMediaRoot
    private lateinit var recorder: AndroidVoiceRecorder
    private lateinit var playback: AndroidVoicePlaybackController

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName,
            android.Manifest.permission.RECORD_AUDIO,
        )
        mediaRoot = TempPrivateMediaRoot()
        val dispatchers = TestAppDispatchers(Dispatchers.Default)
        recorder = AndroidVoiceRecorder(mediaRoot, SequentialIdGenerator("voice"), DefaultAppClock(), dispatchers)
        playback = AndroidVoicePlaybackController(mediaRoot, dispatchers)
    }

    @After
    fun tearDown() {
        mediaRoot.deleteRecursively()
    }

    private suspend fun recordRealClip(): PendingVoiceRecording {
        recorder.start()
        delay(1_200)
        return (recorder.stop() as DataResult.Success).value
    }

    @Test
    fun playingPendingClipTransitionsToPlaying() = runTest {
        val pending = recordRealClip()

        val result = playback.playPending(pending.reference)

        assertEquals(DataResult.Success(Unit), result)
        assertTrue(playback.observeState().value is VoicePlaybackState.Playing)
        playback.stop()
    }

    @Test
    fun pausingTransitionsToPaused() = runTest {
        val pending = recordRealClip()
        playback.playPending(pending.reference)

        val result = playback.pause()

        assertEquals(DataResult.Success(Unit), result)
        assertTrue(playback.observeState().value is VoicePlaybackState.Paused)
        playback.stop()
    }

    @Test
    fun playingAnInvalidReferenceReturnsATypedReadError() = runTest {
        val result = playback.playPending(PendingMediaReference("pending/does-not-exist.m4a"))

        assertEquals(DataResult.Error(AppError.Storage(StorageError.READ_FAILED)), result)
    }
}

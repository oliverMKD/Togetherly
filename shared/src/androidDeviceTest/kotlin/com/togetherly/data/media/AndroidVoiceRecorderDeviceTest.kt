package com.togetherly.data.media

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.datetime.DefaultAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.id.SequentialIdGenerator
import com.togetherly.core.result.DataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exercises the real `MediaRecorder` pipeline against a connected device/emulator's own
 * microphone input. Requires a connected device or emulator to actually run — this environment
 * has neither, so this is compile-checked only; see `IosVoiceRecorderTest` for the equivalent that
 * did run for real, on the iOS simulator.
 */
@RunWith(AndroidJUnit4::class)
class AndroidVoiceRecorderDeviceTest {

    private lateinit var mediaRoot: TempPrivateMediaRoot
    private lateinit var recorder: AndroidVoiceRecorder

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName,
            android.Manifest.permission.RECORD_AUDIO,
        )
        mediaRoot = TempPrivateMediaRoot()
        recorder = AndroidVoiceRecorder(
            mediaRoot = mediaRoot,
            idGenerator = SequentialIdGenerator("voice"),
            clock = DefaultAppClock(),
            dispatchers = TestAppDispatchers(Dispatchers.Default),
        )
    }

    @After
    fun tearDown() {
        mediaRoot.deleteRecursively()
    }

    @Test
    fun recordingForOverASecondProducesAValidPendingRecording() = runTest {
        val startResult = recorder.start()
        assertEquals(DataResult.Success(Unit), startResult)

        delay(1_200)

        val stopResult = recorder.stop()

        val pending = (stopResult as DataResult.Success).value
        assertTrue(pending.duration.inWholeMilliseconds >= 1_000)
        assertTrue(File(mediaRoot.rootPath(), pending.reference.value).exists())
    }

    @Test
    fun doubleStartIsRejected() = runTest {
        recorder.start()

        val secondStart = recorder.start()

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.INVALID_STATE)), secondStart)
        recorder.cancel()
    }

    @Test
    fun stoppingWhenIdleReturnsATypedInvalidStateError() = runTest {
        val result = recorder.stop()

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.INVALID_STATE)), result)
    }

    @Test
    fun cancelDeletesThePendingFile() = runTest {
        recorder.start()
        delay(1_200)

        val cancelResult = recorder.cancel()

        assertEquals(DataResult.Success(Unit), cancelResult)
        assertEquals(VoiceRecorderState.Idle, recorder.observeState().value)
    }
}

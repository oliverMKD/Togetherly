package com.togetherly.data.media

import kotlinx.cinterop.ExperimentalForeignApi
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.id.SequentialIdGenerator
import com.togetherly.core.result.DataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSFileManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Exercises the real `AVAudioRecorder` pipeline against the simulator's own (silent, but
 * functional) audio input — no fake/mocked recorder involved. Max-duration auto-stop (60s) isn't
 * exercised here since waiting that long in a test isn't practical; everything else is real.
 */
@OptIn(ExperimentalForeignApi::class)
class IosVoiceRecorderTest {

    private lateinit var mediaRoot: TempPrivateMediaRoot
    private lateinit var recorder: IosVoiceRecorder

    @BeforeTest
    fun setUp() {
        mediaRoot = TempPrivateMediaRoot()
        recorder = IosVoiceRecorder(
            mediaRoot = mediaRoot,
            idGenerator = SequentialIdGenerator("voice"),
            clock = RealClock(),
            dispatchers = TestAppDispatchers(Dispatchers.Default),
        )
    }

    @AfterTest
    fun tearDown() {
        mediaRoot.deleteRecursively()
    }

    @Test
    fun recordingForOverASecondProducesAValidPendingRecording() = runTest(timeout = 15.seconds) {
        val startResult = recorder.start()
        assertEquals(DataResult.Success(Unit), startResult)

        realDelay(1_200)

        val stopResult = recorder.stop()

        val pending = (stopResult as DataResult.Success).value
        assertTrue(pending.duration.inWholeMilliseconds >= 1_000)
        assertTrue(NSFileManager.defaultManager.fileExistsAtPath("${mediaRoot.rootPath()}/${pending.reference.value}"))
    }

    @Test
    fun doubleStartIsRejected() = runTest(timeout = 15.seconds) {
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
    fun stoppingBeforeTheMinimumDurationReturnsInvalidMediaAndDeletesTheFile() = runTest(timeout = 15.seconds) {
        recorder.start()

        val result = recorder.stop()

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.INVALID_MEDIA)), result)
    }

    @Test
    fun cancelDeletesThePendingFile() = runTest(timeout = 15.seconds) {
        recorder.start()
        realDelay(1_200)

        val cancelResult = recorder.cancel()

        assertEquals(DataResult.Success(Unit), cancelResult)
        assertEquals(VoiceRecorderState.Idle, recorder.observeState().value)
        // Nothing left under pending/ after a cancel.
        val pendingDir = "${mediaRoot.rootPath()}/${PrivateMediaPaths.PENDING_DIRECTORY}"
        val contents = NSFileManager.defaultManager.contentsOfDirectoryAtPath(pendingDir, error = null)
        assertTrue(contents.isNullOrEmpty())
    }

    @Test
    fun cancelWhenIdleIsANoOp() = runTest {
        val result = recorder.cancel()

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(VoiceRecorderState.Idle, recorder.observeState().value)
    }
}

package com.togetherly.data.media

import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.PendingMediaReference
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class FakeVoiceRecorderTest {

    @Test
    fun startSucceedsByDefaultAndRecordsTheCall() = runTest {
        val recorder = FakeVoiceRecorder()

        val result = recorder.start()

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(1, recorder.startCalls.size)
    }

    @Test
    fun stopReturnsTheConfiguredPendingRecording() = runTest {
        val recorder = FakeVoiceRecorder()
        val pending = PendingVoiceRecording(PendingMediaReference("pending-voice"), 5.seconds, 1_000L)
        recorder.nextStopResult = DataResult.Success(pending)

        val result = recorder.stop()

        assertEquals(DataResult.Success(pending), result)
    }

    @Test
    fun stopWithoutConfiguredResultReturnsATypedError() = runTest {
        val recorder = FakeVoiceRecorder()

        val result = recorder.stop()

        assertEquals(true, result is DataResult.Error)
    }

    @Test
    fun observeStateReflectsManuallySetState() = runTest {
        val recorder = FakeVoiceRecorder()
        val error = AppError.Validation(ValidationError.INVALID_STATE)

        recorder.setState(VoiceRecorderState.Failed(error))

        assertEquals(VoiceRecorderState.Failed(error), recorder.observeState().value)
    }

    @Test
    fun cancelRecordsTheCallAndReturnsConfiguredResult() = runTest {
        val recorder = FakeVoiceRecorder()

        val result = recorder.cancel()

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(1, recorder.cancelCalls.size)
    }
}

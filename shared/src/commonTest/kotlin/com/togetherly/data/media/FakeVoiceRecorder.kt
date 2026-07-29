package com.togetherly.data.media

import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeVoiceRecorder : VoiceRecorder {

    private val stateFlow = MutableStateFlow<VoiceRecorderState>(VoiceRecorderState.Idle)

    var nextStartResult: DataResult<Unit> = DataResult.Success(Unit)
    var nextStopResult: DataResult<PendingVoiceRecording>? = null
    var nextCancelResult: DataResult<Unit> = DataResult.Success(Unit)

    val startCalls = mutableListOf<Unit>()
    val stopCalls = mutableListOf<Unit>()
    val cancelCalls = mutableListOf<Unit>()

    fun setState(state: VoiceRecorderState) {
        stateFlow.value = state
    }

    override fun observeState(): StateFlow<VoiceRecorderState> = stateFlow

    override suspend fun start(): DataResult<Unit> {
        startCalls += Unit
        return nextStartResult
    }

    override suspend fun stop(): DataResult<PendingVoiceRecording> {
        stopCalls += Unit
        return nextStopResult ?: DataResult.Error(AppError.Unexpected())
    }

    override suspend fun cancel(): DataResult<Unit> {
        cancelCalls += Unit
        return nextCancelResult
    }
}

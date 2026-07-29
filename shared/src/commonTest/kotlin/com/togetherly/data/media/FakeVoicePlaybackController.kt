package com.togetherly.data.media

import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.PendingMediaReference
import com.togetherly.core.result.DataResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeVoicePlaybackController : VoicePlaybackController {

    private val stateFlow = MutableStateFlow<VoicePlaybackState>(VoicePlaybackState.Idle)

    var nextResult: DataResult<Unit> = DataResult.Success(Unit)

    val playCalls = mutableListOf<MediaReference>()
    val playPendingCalls = mutableListOf<PendingMediaReference>()
    val pauseCalls = mutableListOf<Unit>()
    val stopCalls = mutableListOf<Unit>()

    fun setState(state: VoicePlaybackState) {
        stateFlow.value = state
    }

    override fun observeState(): StateFlow<VoicePlaybackState> = stateFlow

    override suspend fun play(reference: MediaReference): DataResult<Unit> {
        playCalls += reference
        return nextResult
    }

    override suspend fun playPending(reference: PendingMediaReference): DataResult<Unit> {
        playPendingCalls += reference
        return nextResult
    }

    override suspend fun pause(): DataResult<Unit> {
        pauseCalls += Unit
        return nextResult
    }

    override suspend fun stop(): DataResult<Unit> {
        stopCalls += Unit
        return nextResult
    }
}

package com.togetherly.data.media

import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.PendingMediaReference
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

/**
 * Plays at most one voice clip at a time — starting either [play] or [playPending] stops and
 * releases whatever was already playing first, so there is never more than one active player.
 * [playPending] exists for previewing a just-recorded clip before it's committed;
 * [play] is for playback from Journey/a saved completion's already-committed voice memory.
 */
interface VoicePlaybackController {

    fun observeState(): StateFlow<VoicePlaybackState>

    suspend fun play(
        reference: MediaReference,
    ): DataResult<Unit>

    suspend fun playPending(
        reference: PendingMediaReference,
    ): DataResult<Unit>

    suspend fun pause(): DataResult<Unit>

    suspend fun stop(): DataResult<Unit>
}

sealed interface VoicePlaybackState {
    data object Idle : VoicePlaybackState

    data class Playing(
        val position: Duration,
        val duration: Duration,
    ) : VoicePlaybackState

    data class Paused(
        val position: Duration,
        val duration: Duration,
    ) : VoicePlaybackState

    data class Failed(
        val error: AppError,
    ) : VoicePlaybackState
}

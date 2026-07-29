package com.togetherly.data.media

import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.PendingMediaReference
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Shared by every platform's [VoiceRecorder] implementation. */
object VoiceRecordingLimits {
    val MAXIMUM_DURATION = 60.seconds
    val MINIMUM_DURATION = 1.seconds
}

/**
 * Records at most one app-private voice clip at a time, directly into pending storage — there is
 * no separate "create pending" step like photos have, since audio is written as it's captured.
 * [start] fails with a typed invalid-state error if already recording; [stop] fails the same way
 * if nothing is recording *and* no automatically-finalized clip (see [VoiceRecorderState]'s own
 * KDoc on the 60-second cap) is waiting to be claimed. Every terminal path — [stop], [cancel], an
 * internal error, or the maximum duration being reached — releases the underlying platform
 * recorder; none of them can leak it.
 */
interface VoiceRecorder {

    fun observeState(): StateFlow<VoiceRecorderState>

    suspend fun start(): DataResult<Unit>

    suspend fun stop(): DataResult<PendingVoiceRecording>

    suspend fun cancel(): DataResult<Unit>
}

sealed interface VoiceRecorderState {
    data object Idle : VoiceRecorderState

    data class Recording(
        val startedAt: Instant,
        val elapsed: Duration,
        val maximum: Duration,
    ) : VoiceRecorderState

    data object Stopping : VoiceRecorderState

    data class Failed(
        val error: AppError,
    ) : VoiceRecorderState
}

data class PendingVoiceRecording(
    val reference: PendingMediaReference,
    val duration: Duration,
    val sizeBytes: Long,
)

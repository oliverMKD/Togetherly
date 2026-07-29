package com.togetherly.data.media

import android.media.MediaPlayer
import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.PendingMediaReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * `MediaPlayer` playback of a single private voice file — [play]/[playPending] release whatever
 * was already playing first (see [PrivateMediaStorage]'s own single-player guarantee applied here
 * to playback rather than recording), so only one clip is ever active. Calling [play]/[playPending]
 * again for the *same* file while [VoicePlaybackState.Paused] resumes in place rather than
 * restarting from the beginning, since the interface has no separate `resume` method.
 */
internal class AndroidVoicePlaybackController(
    private val mediaRoot: PrivateMediaRoot,
    private val dispatchers: AppDispatchers,
) : VoicePlaybackController {

    private val stateFlow = MutableStateFlow<VoicePlaybackState>(VoicePlaybackState.Idle)
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    private var player: MediaPlayer? = null
    private var currentRelativePath: String? = null
    private var positionTicker: Job? = null

    override fun observeState(): StateFlow<VoicePlaybackState> = stateFlow

    override suspend fun play(reference: MediaReference): DataResult<Unit> = playPath(reference.value)

    override suspend fun playPending(reference: PendingMediaReference): DataResult<Unit> = playPath(reference.value)

    private suspend fun playPath(relativeReference: String): DataResult<Unit> = mutex.withLock {
        withContext(dispatchers.io) {
            if (!PrivateMediaPaths.isSafeRelativeReference(relativeReference)) {
                return@withContext DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))
            }

            val existingPlayer = player
            if (existingPlayer != null && currentRelativePath == relativeReference && stateFlow.value is VoicePlaybackState.Paused) {
                return@withContext resumeExisting(existingPlayer)
            }

            val absolutePath = "${mediaRoot.rootPath()}/$relativeReference"
            if (!File(absolutePath).exists()) {
                return@withContext DataResult.Error(AppError.Storage(StorageError.READ_FAILED))
            }

            releasePlayerQuietly()
            try {
                val mediaPlayer = MediaPlayer()
                mediaPlayer.setDataSource(absolutePath)
                mediaPlayer.setOnCompletionListener {
                    scope.launch { mutex.withLock { onPlaybackCompleted() } }
                }
                mediaPlayer.setOnErrorListener { _, _, _ ->
                    scope.launch { mutex.withLock { onPlaybackError() } }
                    true
                }
                mediaPlayer.prepare()
                mediaPlayer.start()
                player = mediaPlayer
                currentRelativePath = relativeReference
                val duration = mediaPlayer.duration.milliseconds
                stateFlow.value = VoicePlaybackState.Playing(Duration.ZERO, duration)
                startPositionTicker(duration)
                DataResult.Success(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                releasePlayerQuietly()
                val error = AppError.Storage(StorageError.READ_FAILED, t)
                stateFlow.value = VoicePlaybackState.Failed(error)
                DataResult.Error(error)
            }
        }
    }

    private fun resumeExisting(mediaPlayer: MediaPlayer): DataResult<Unit> = try {
        mediaPlayer.start()
        val duration = mediaPlayer.duration.milliseconds
        stateFlow.value = VoicePlaybackState.Playing(mediaPlayer.currentPosition.milliseconds, duration)
        startPositionTicker(duration)
        DataResult.Success(Unit)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (t: Throwable) {
        releasePlayerQuietly()
        val error = AppError.Storage(StorageError.READ_FAILED, t)
        stateFlow.value = VoicePlaybackState.Failed(error)
        DataResult.Error(error)
    }

    override suspend fun pause(): DataResult<Unit> = mutex.withLock {
        withContext(dispatchers.io) {
            val mediaPlayer = player
            val state = stateFlow.value
            if (mediaPlayer == null || state !is VoicePlaybackState.Playing) {
                return@withContext DataResult.Error(AppError.Validation(ValidationError.INVALID_STATE))
            }
            positionTicker?.cancel()
            try {
                mediaPlayer.pause()
                stateFlow.value = VoicePlaybackState.Paused(mediaPlayer.currentPosition.milliseconds, state.duration)
                DataResult.Success(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                releasePlayerQuietly()
                val error = AppError.Storage(StorageError.READ_FAILED, t)
                stateFlow.value = VoicePlaybackState.Failed(error)
                DataResult.Error(error)
            }
        }
    }

    override suspend fun stop(): DataResult<Unit> = mutex.withLock {
        withContext(dispatchers.io) {
            positionTicker?.cancel()
            releasePlayerQuietly()
            currentRelativePath = null
            stateFlow.value = VoicePlaybackState.Idle
            DataResult.Success(Unit)
        }
    }

    /** Must be called while holding [mutex]. */
    private fun onPlaybackCompleted() {
        positionTicker?.cancel()
        releasePlayerQuietly()
        currentRelativePath = null
        stateFlow.value = VoicePlaybackState.Idle
    }

    /** Must be called while holding [mutex]. */
    private fun onPlaybackError() {
        positionTicker?.cancel()
        releasePlayerQuietly()
        currentRelativePath = null
        stateFlow.value = VoicePlaybackState.Failed(AppError.Storage(StorageError.READ_FAILED))
    }

    private fun releasePlayerQuietly() {
        try {
            player?.release()
        } catch (t: Throwable) {
            // Already in an error/cleanup path; nothing further to do with a release failure.
        }
        player = null
    }

    private fun startPositionTicker(duration: Duration) {
        positionTicker = scope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MILLIS)
                val mediaPlayer = player ?: break
                val position = try {
                    mediaPlayer.currentPosition.milliseconds
                } catch (t: Throwable) {
                    break
                }
                stateFlow.value = VoicePlaybackState.Playing(position, duration)
            }
        }
    }

    private companion object {
        const val TICK_INTERVAL_MILLIS = 200L
    }
}

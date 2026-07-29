package com.togetherly.data.media

import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.PendingMediaReference
import kotlinx.cinterop.ExperimentalForeignApi
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
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.darwin.NSObject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * `AVAudioPlayer` playback of a single private voice file — see [AndroidVoicePlaybackController]'s
 * own KDoc for the single-player and resume-via-[play]/[playPending] design this mirrors.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosVoicePlaybackController(
    private val mediaRoot: PrivateMediaRoot,
    private val dispatchers: AppDispatchers,
) : VoicePlaybackController {

    private class PlayerDelegate(
        private val onFinished: () -> Unit,
        private val onError: () -> Unit,
    ) : NSObject(), AVAudioPlayerDelegateProtocol {
        override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
            if (successfully) onFinished() else onError()
        }

        override fun audioPlayerDecodeErrorDidOccur(player: AVAudioPlayer, error: NSError?) {
            onError()
        }
    }

    private val stateFlow = MutableStateFlow<VoicePlaybackState>(VoicePlaybackState.Idle)
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    private var player: AVAudioPlayer? = null
    private var delegate: PlayerDelegate? = null
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
            if (!NSFileManager.defaultManager.fileExistsAtPath(absolutePath)) {
                return@withContext DataResult.Error(AppError.Storage(StorageError.READ_FAILED))
            }

            releasePlayerQuietly()
            try {
                AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, error = null)
                val url = NSURL.fileURLWithPath(absolutePath)
                // The Kotlin binding claims this constructor is non-null, but it can genuinely
                // return null at runtime (an unsupported/corrupt file) — an explicit nullable
                // cast is needed to actually check for that instead of trusting the static type.
                val audioPlayer = (AVAudioPlayer(contentsOfURL = url, error = null) as AVAudioPlayer?)
                    ?: throw IllegalStateException("Could not create AVAudioPlayer")
                val playerDelegate = PlayerDelegate(
                    onFinished = { scope.launch { mutex.withLock { onPlaybackCompleted() } } },
                    onError = { scope.launch { mutex.withLock { onPlaybackError() } } },
                )
                audioPlayer.delegate = playerDelegate
                audioPlayer.play()

                player = audioPlayer
                delegate = playerDelegate
                currentRelativePath = relativeReference
                val duration = audioPlayer.duration.seconds
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

    private fun resumeExisting(audioPlayer: AVAudioPlayer): DataResult<Unit> = try {
        audioPlayer.play()
        val duration = audioPlayer.duration.seconds
        stateFlow.value = VoicePlaybackState.Playing(audioPlayer.currentTime.seconds, duration)
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
            val audioPlayer = player
            val state = stateFlow.value
            if (audioPlayer == null || state !is VoicePlaybackState.Playing) {
                return@withContext DataResult.Error(AppError.Validation(ValidationError.INVALID_STATE))
            }
            positionTicker?.cancel()
            try {
                audioPlayer.pause()
                stateFlow.value = VoicePlaybackState.Paused(audioPlayer.currentTime.seconds, state.duration)
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
        player?.stop()
        player = null
        delegate = null
    }

    private fun startPositionTicker(duration: Duration) {
        positionTicker = scope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MILLIS)
                val audioPlayer = player ?: break
                stateFlow.value = VoicePlaybackState.Playing(audioPlayer.currentTime.seconds, duration)
            }
        }
    }

    private companion object {
        const val TICK_INTERVAL_MILLIS = 200L
    }
}

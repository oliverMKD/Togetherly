package com.togetherly.data.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.datetime.AppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.id.IdGenerator
import com.togetherly.core.result.DataResult
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
import org.koin.mp.KoinPlatform
import java.io.File
import kotlin.time.Instant

/**
 * `MediaRecorder` with `MPEG_4` output + `AAC` encoding — see [PrivateMediaPaths]'s own KDoc for
 * why this pairs with iOS's `AVAudioRecorder`/`kAudioFormatMPEG4AAC` under the same `.m4a`
 * extension. Every public method is serialized through [mutex] since `MediaRecorder`'s own
 * `OnInfoListener`/`OnErrorListener` callbacks can fire on an arbitrary internal thread — without
 * it, an automatic max-duration finalize could race a user-initiated [stop].
 *
 * Backgrounding is deliberately *not* handled inside this class: doing so correctly needs an
 * Activity/lifecycle-owner reference, which would mean either accepting a platform UI type here or
 * reaching for `ProcessLifecycleOwner` (a dependency this module doesn't otherwise need). [stop]
 * and [cancel] are already safe to call from any trigger — a Composable lifecycle observer at the
 * Route boundary (Step 10.4) is the intended place to call [stop] when the hosting screen
 * backgrounds, per this feature's "stops and returns the valid clip when possible" preference.
 */
internal class AndroidVoiceRecorder(
    private val mediaRoot: PrivateMediaRoot,
    private val idGenerator: IdGenerator,
    private val clock: AppClock,
    private val dispatchers: AppDispatchers,
) : VoiceRecorder {

    private val stateFlow = MutableStateFlow<VoiceRecorderState>(VoiceRecorderState.Idle)
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    private var recorder: MediaRecorder? = null
    private var currentRelativeReference: String? = null
    private var tickerJob: Job? = null

    /** An automatically max-duration-finalized clip waiting for the next [stop] call to claim it. */
    private var autoFinalized: PendingVoiceRecording? = null

    private fun uniquePendingVoiceRelativeReference(): String {
        val baseId = idGenerator.generate()
        var attempt = 0
        while (true) {
            val id = if (attempt == 0) baseId else "$baseId-$attempt"
            val candidate = PrivateMediaPaths.pendingVoiceRelativeReference(id)
            if (!File("${mediaRoot.rootPath()}/$candidate").exists()) return candidate
            attempt++
        }
    }

    override fun observeState(): StateFlow<VoiceRecorderState> = stateFlow

    override suspend fun start(): DataResult<Unit> = mutex.withLock {
        withContext(dispatchers.io) {
            if (stateFlow.value is VoiceRecorderState.Recording || stateFlow.value is VoiceRecorderState.Stopping) {
                return@withContext DataResult.Error(AppError.Validation(ValidationError.INVALID_STATE))
            }

            val relativeReference = uniquePendingVoiceRelativeReference()
            val absolutePath = "${mediaRoot.rootPath()}/$relativeReference"

            try {
                File(absolutePath).parentFile?.mkdirs()
                val mediaRecorder = createMediaRecorder()
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                mediaRecorder.setAudioEncodingBitRate(AUDIO_BIT_RATE)
                mediaRecorder.setAudioSamplingRate(AUDIO_SAMPLE_RATE)
                mediaRecorder.setMaxDuration(VoiceRecordingLimits.MAXIMUM_DURATION.inWholeMilliseconds.toInt())
                mediaRecorder.setOutputFile(absolutePath)
                mediaRecorder.setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        scope.launch { mutex.withLock { finalizeOnMaxDuration() } }
                    }
                }
                mediaRecorder.setOnErrorListener { _, _, _ ->
                    scope.launch { mutex.withLock { failRecording() } }
                }
                mediaRecorder.prepare()
                mediaRecorder.start()

                recorder = mediaRecorder
                currentRelativeReference = relativeReference
                autoFinalized = null
                val startedAt = clock.now()
                stateFlow.value = VoiceRecorderState.Recording(startedAt, kotlin.time.Duration.ZERO, VoiceRecordingLimits.MAXIMUM_DURATION)
                startTicker(startedAt)
                DataResult.Success(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                releaseRecorderQuietly()
                File(absolutePath).delete()
                val error = AppError.Storage(StorageError.WRITE_FAILED, t)
                stateFlow.value = VoiceRecorderState.Failed(error)
                DataResult.Error(error)
            }
        }
    }

    override suspend fun stop(): DataResult<PendingVoiceRecording> = mutex.withLock {
        withContext(dispatchers.io) {
            autoFinalized?.let {
                autoFinalized = null
                return@withContext DataResult.Success(it)
            }

            val state = stateFlow.value
            if (state !is VoiceRecorderState.Recording) {
                return@withContext DataResult.Error(AppError.Validation(ValidationError.INVALID_STATE))
            }

            tickerJob?.cancel()
            stateFlow.value = VoiceRecorderState.Stopping
            val elapsed = clock.now() - state.startedAt
            val relativeReference = requireNotNull(currentRelativeReference)

            val result = try {
                recorder?.stop()
                releaseRecorderQuietly()
                if (elapsed < VoiceRecordingLimits.MINIMUM_DURATION) {
                    File("${mediaRoot.rootPath()}/$relativeReference").delete()
                    DataResult.Error(AppError.Validation(ValidationError.INVALID_MEDIA))
                } else {
                    val file = File("${mediaRoot.rootPath()}/$relativeReference")
                    DataResult.Success(PendingVoiceRecording(PendingMediaReference(relativeReference), elapsed, file.length()))
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                // MediaRecorder.stop() throws when effectively nothing usable was captured.
                releaseRecorderQuietly()
                File("${mediaRoot.rootPath()}/$relativeReference").delete()
                DataResult.Error(AppError.Validation(ValidationError.INVALID_MEDIA))
            }

            currentRelativeReference = null
            stateFlow.value = VoiceRecorderState.Idle
            result
        }
    }

    override suspend fun cancel(): DataResult<Unit> = mutex.withLock {
        withContext(dispatchers.io) {
            tickerJob?.cancel()
            autoFinalized = null
            val relativeReference = currentRelativeReference
            try {
                recorder?.stop()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                // Discarding regardless of whether a clean stop was possible.
            }
            releaseRecorderQuietly()
            currentRelativeReference = null
            relativeReference?.let { File("${mediaRoot.rootPath()}/$it").delete() }
            stateFlow.value = VoiceRecorderState.Idle
            DataResult.Success(Unit)
        }
    }

    /** Must be called while holding [mutex]. */
    private fun finalizeOnMaxDuration() {
        val state = stateFlow.value as? VoiceRecorderState.Recording ?: return
        tickerJob?.cancel()
        val relativeReference = currentRelativeReference ?: return
        try {
            recorder?.stop()
            releaseRecorderQuietly()
            val file = File("${mediaRoot.rootPath()}/$relativeReference")
            autoFinalized = PendingVoiceRecording(PendingMediaReference(relativeReference), VoiceRecordingLimits.MAXIMUM_DURATION, file.length())
            stateFlow.value = VoiceRecorderState.Idle
        } catch (t: Throwable) {
            releaseRecorderQuietly()
            File("${mediaRoot.rootPath()}/$relativeReference").delete()
            stateFlow.value = VoiceRecorderState.Failed(AppError.Storage(StorageError.WRITE_FAILED, t))
        } finally {
            currentRelativeReference = null
        }
    }

    /** Must be called while holding [mutex]. */
    private fun failRecording() {
        tickerJob?.cancel()
        val relativeReference = currentRelativeReference
        releaseRecorderQuietly()
        currentRelativeReference = null
        autoFinalized = null
        relativeReference?.let { File("${mediaRoot.rootPath()}/$it").delete() }
        stateFlow.value = VoiceRecorderState.Failed(AppError.Storage(StorageError.WRITE_FAILED))
    }

    private fun releaseRecorderQuietly() {
        try {
            recorder?.release()
        } catch (t: Throwable) {
            // Already in an error/cleanup path; nothing further to do with a release failure.
        }
        recorder = null
    }

    private fun startTicker(startedAt: Instant) {
        tickerJob = scope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MILLIS)
                val elapsed = clock.now() - startedAt
                stateFlow.value = VoiceRecorderState.Recording(startedAt, elapsed, VoiceRecordingLimits.MAXIMUM_DURATION)
            }
        }
    }

    private fun createMediaRecorder(): MediaRecorder {
        val appContext = KoinPlatform.getKoin().get<Context>().applicationContext
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(appContext)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    private companion object {
        const val AUDIO_BIT_RATE = 96_000
        const val AUDIO_SAMPLE_RATE = 44_100
        const val TICK_INTERVAL_MILLIS = 200L
    }
}

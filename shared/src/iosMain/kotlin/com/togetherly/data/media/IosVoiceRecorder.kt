package com.togetherly.data.media

import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.datetime.AppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.id.IdGenerator
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.PendingMediaReference
import com.togetherly.data.platform.protectComplete
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
import platform.AVFAudio.AVAudioQualityMedium
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioRecorderDelegateProtocol
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVEncoderAudioQualityKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfFile
import platform.darwin.NSObject
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * `AVAudioRecorder` with `kAudioFormatMPEG4AAC` in an M4A container — pairs with Android's
 * `MediaRecorder`(`MPEG_4`/`AAC`) under the same extension; see [PrivateMediaPaths]. Uses plain
 * `record()` plus this class's own elapsed-time ticker to enforce
 * [VoiceRecordingLimits.MAXIMUM_DURATION] — `recordForDuration`'s OS-scheduled auto-stop was tried
 * first, but on this simulator/SDK combination it produced `.m4a` files `AVAudioPlayer` could not
 * decode (confirmed against a hand-built, independently-valid WAV fixture, which *did* play
 * correctly — see `IosVoicePlaybackControllerDiagnosticTest`), while an explicit `stop()` call
 * always produces a playable file. Auto-stop is dispatched through the exact same
 * [finalizeOnMaxDuration] path as before, just triggered by the ticker instead of a delegate
 * callback.
 *
 * Backgrounding is intentionally not handled here — see [AndroidVoiceRecorder]'s equivalent KDoc;
 * the same reasoning applies (this class has no UIKit/lifecycle dependency, and [stop]/[cancel]
 * are already safe to call from any trigger a Route-boundary lifecycle observer might use).
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosVoiceRecorder(
    private val mediaRoot: PrivateMediaRoot,
    private val idGenerator: IdGenerator,
    private val clock: AppClock,
    private val dispatchers: AppDispatchers,
) : VoiceRecorder {

    /**
     * [audioRecorderDidFinishRecording] is deliberately a no-op — it fires for every stop
     * (including this class's own explicit [stop]/[cancel]/[finalizeOnMaxDuration] calls, not
     * only unexpected ones), and since those call sites already handle everything deterministically
     * themselves, reacting to it too would race the very call that triggered it.
     */
    private class RecorderDelegate(
        private val onError: () -> Unit,
    ) : NSObject(), AVAudioRecorderDelegateProtocol {
        override fun audioRecorderDidFinishRecording(recorder: AVAudioRecorder, successfully: Boolean) {
            // Intentionally empty — see this class's own KDoc above.
        }

        override fun audioRecorderEncodeErrorDidOccur(recorder: AVAudioRecorder, error: NSError?) {
            onError()
        }
    }

    private val stateFlow = MutableStateFlow<VoiceRecorderState>(VoiceRecorderState.Idle)
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    private var recorder: AVAudioRecorder? = null
    private var delegate: RecorderDelegate? = null
    private var currentRelativeReference: String? = null
    private var tickerJob: Job? = null

    /** An automatically max-duration-finalized clip waiting for the next [stop] call to claim it. */
    private var autoFinalized: PendingVoiceRecording? = null

    /** See [AndroidVoiceRecorder]'s equivalent — same numeric-suffix collision fallback, kept in parity across platforms. */
    private fun uniquePendingVoiceRelativeReference(): String {
        val baseId = idGenerator.generate()
        var attempt = 0
        while (true) {
            val id = if (attempt == 0) baseId else "$baseId-$attempt"
            val candidate = PrivateMediaPaths.pendingVoiceRelativeReference(id)
            if (!NSFileManager.defaultManager.fileExistsAtPath("${mediaRoot.rootPath()}/$candidate")) return candidate
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
                val directory = absolutePath.substringBeforeLast('/')
                NSFileManager.defaultManager.createDirectoryAtPath(
                    path = directory,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null,
                )

                // `setActive:error:`/`setActive:withOptions:error:` don't resolve through this
                // Kotlin/Native AVFAudio binding in this SDK snapshot despite the selector being
                // present in the klib metadata (a category/module-splitting quirk) — category
                // configuration alone plus the OS's own implicit activation on record start is
                // used instead. See [deactivateSession]'s own note.
                AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayAndRecord, error = null)

                val settings: Map<Any?, Any?> = mapOf(
                    AVFormatIDKey to kAudioFormatMPEG4AAC,
                    AVSampleRateKey to AUDIO_SAMPLE_RATE,
                    AVNumberOfChannelsKey to 1,
                    AVEncoderAudioQualityKey to AVAudioQualityMedium,
                )
                val url = NSURL.fileURLWithPath(absolutePath)
                val audioRecorder = AVAudioRecorder(uRL = url, settings = settings, error = null)

                val recorderDelegate = RecorderDelegate(
                    onError = {
                        scope.launch { mutex.withLock { failRecording() } }
                    },
                )
                audioRecorder.delegate = recorderDelegate
                audioRecorder.record()
                protectComplete(absolutePath)

                recorder = audioRecorder
                delegate = recorderDelegate
                currentRelativeReference = relativeReference
                autoFinalized = null
                val startedAt = clock.now()
                stateFlow.value = VoiceRecorderState.Recording(startedAt, Duration.ZERO, VoiceRecordingLimits.MAXIMUM_DURATION)
                startTicker(startedAt)
                DataResult.Success(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                releaseRecorderQuietly()
                deactivateSession()
                deleteFileAt(absolutePath)
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
            val absolutePath = "${mediaRoot.rootPath()}/$relativeReference"

            recorder?.stop()
            deactivateSession()
            releaseRecorderQuietly()

            val result = if (elapsed < VoiceRecordingLimits.MINIMUM_DURATION) {
                deleteFileAt(absolutePath)
                DataResult.Error(AppError.Validation(ValidationError.INVALID_MEDIA))
            } else {
                DataResult.Success(PendingVoiceRecording(PendingMediaReference(relativeReference), elapsed, fileSizeAt(absolutePath)))
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
            recorder?.stop()
            deactivateSession()
            releaseRecorderQuietly()
            currentRelativeReference = null
            relativeReference?.let { deleteFileAt("${mediaRoot.rootPath()}/$it") }
            stateFlow.value = VoiceRecorderState.Idle
            DataResult.Success(Unit)
        }
    }

    /** Must be called while holding [mutex]. Invoked by the ticker once [VoiceRecordingLimits.MAXIMUM_DURATION] is reached. */
    private fun finalizeOnMaxDuration() {
        if (stateFlow.value !is VoiceRecorderState.Recording) return
        val relativeReference = currentRelativeReference ?: return
        val absolutePath = "${mediaRoot.rootPath()}/$relativeReference"
        try {
            recorder?.stop()
            deactivateSession()
            releaseRecorderQuietly()
            autoFinalized = PendingVoiceRecording(PendingMediaReference(relativeReference), VoiceRecordingLimits.MAXIMUM_DURATION, fileSizeAt(absolutePath))
            stateFlow.value = VoiceRecorderState.Idle
        } catch (t: Throwable) {
            releaseRecorderQuietly()
            deactivateSession()
            deleteFileAt(absolutePath)
            stateFlow.value = VoiceRecorderState.Failed(AppError.Storage(StorageError.WRITE_FAILED, t))
        }
        currentRelativeReference = null
    }

    /** Must be called while holding [mutex]. */
    private fun failRecording() {
        tickerJob?.cancel()
        val relativeReference = currentRelativeReference
        deactivateSession()
        releaseRecorderQuietly()
        currentRelativeReference = null
        autoFinalized = null
        relativeReference?.let { deleteFileAt("${mediaRoot.rootPath()}/$it") }
        stateFlow.value = VoiceRecorderState.Failed(AppError.Storage(StorageError.WRITE_FAILED))
    }

    private fun releaseRecorderQuietly() {
        recorder = null
        delegate = null
    }

    /**
     * There is no explicit session deactivation call available here (see [start]'s note on
     * `setActive` not resolving) — releasing the recorder and letting the OS reclaim the session
     * is the best available restoration in this environment.
     */
    private fun deactivateSession() {
        // Intentionally empty — see this call's own KDoc above.
    }

    private fun deleteFileAt(path: String) {
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
    }

    private fun fileSizeAt(path: String): Long = (NSData.dataWithContentsOfFile(path)?.length ?: 0uL).toLong()

    private fun startTicker(startedAt: Instant) {
        tickerJob = scope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MILLIS)
                val elapsed = clock.now() - startedAt
                if (elapsed >= VoiceRecordingLimits.MAXIMUM_DURATION) {
                    mutex.withLock { finalizeOnMaxDuration() }
                    break
                }
                stateFlow.value = VoiceRecorderState.Recording(startedAt, elapsed, VoiceRecordingLimits.MAXIMUM_DURATION)
            }
        }
    }

    private companion object {
        const val AUDIO_SAMPLE_RATE = 44_100.0
        const val TICK_INTERVAL_MILLIS = 200L
    }
}

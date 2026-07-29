package com.togetherly.data.media

import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.id.SequentialIdGenerator
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.PendingMediaReference
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Exercises the real `AVAudioPlayer` pipeline against a known-valid audio fixture written
 * directly into private storage. This is deliberately *not* chained off a live [IosVoiceRecorder]
 * recording — [IosVoiceRecorderTest] proves recording itself works (real files, real elapsed
 * duration), but this sandboxed simulator's audio stack does not reliably produce a file
 * `AVAudioPlayer` can decode immediately afterward (confirmed in
 * `IosVoicePlaybackControllerDiagnosticTest`, where the same known-valid WAV bytes used here play
 * back correctly). Using a real fixture instead of a recorded one still exercises every real
 * decode/play/pause/stop/single-player code path in [IosVoicePlaybackController].
 */
@OptIn(ExperimentalForeignApi::class)
class IosVoicePlaybackControllerTest {

    private lateinit var mediaRoot: TempPrivateMediaRoot
    private lateinit var playback: IosVoicePlaybackController

    @BeforeTest
    fun setUp() {
        mediaRoot = TempPrivateMediaRoot()
        playback = IosVoicePlaybackController(mediaRoot, TestAppDispatchers(Dispatchers.Default))
    }

    @AfterTest
    fun tearDown() {
        mediaRoot.deleteRecursively()
    }

    private fun pendingClip(name: String): PendingMediaReference {
        val reference = PendingMediaReference("pending/$name.wav")
        writeSilentWav(mediaRoot, reference.value)
        return reference
    }

    @Test
    fun playingPendingClipTransitionsToPlaying() = runTest(timeout = 15.seconds) {
        val reference = pendingClip("clip-1")

        val result = playback.playPending(reference)

        assertEquals(DataResult.Success(Unit), result)
        assertTrue(playback.observeState().value is VoicePlaybackState.Playing)
        playback.stop()
    }

    @Test
    fun stoppingReturnsToIdle() = runTest(timeout = 15.seconds) {
        val reference = pendingClip("clip-1")
        playback.playPending(reference)

        val result = playback.stop()

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(VoicePlaybackState.Idle, playback.observeState().value)
    }

    @Test
    fun pausingTransitionsToPaused() = runTest(timeout = 15.seconds) {
        val reference = pendingClip("clip-1")
        playback.playPending(reference)

        val result = playback.pause()

        assertEquals(DataResult.Success(Unit), result)
        assertTrue(playback.observeState().value is VoicePlaybackState.Paused)
        playback.stop()
    }

    @Test
    fun pausingWhileNotPlayingReturnsATypedInvalidStateError() = runTest {
        val result = playback.pause()

        assertTrue(result is DataResult.Error)
    }

    @Test
    fun playingAnInvalidReferenceReturnsATypedReadError() = runTest(timeout = 15.seconds) {
        val result = playback.playPending(PendingMediaReference("pending/does-not-exist.m4a"))

        assertEquals(DataResult.Error(AppError.Storage(StorageError.READ_FAILED)), result)
    }

    @Test
    fun startingANewClipStopsThePreviousOne() = runTest(timeout = 15.seconds) {
        val first = pendingClip("clip-1")
        playback.playPending(first)
        val second = pendingClip("clip-2")

        val result = playback.playPending(second)

        assertEquals(DataResult.Success(Unit), result)
        assertTrue(playback.observeState().value is VoicePlaybackState.Playing)
        playback.stop()
    }

    @Test
    fun playingACommittedVoiceFileWorks() = runTest(timeout = 15.seconds) {
        val pending = pendingClip("clip-1")
        val storage = IosPrivateMediaStorage(
            mediaRoot = mediaRoot,
            photoImporter = FakePhotoImporter(),
            imageNormalizer = FakeImageNormalizer(),
            thumbnailGenerator = FakeThumbnailGenerator(),
            idGenerator = SequentialIdGenerator("media"),
            dispatchers = TestAppDispatchers(Dispatchers.Default),
            diagnostics = FakeOperationalDiagnostics(),
        )
        val committed = (
            storage.commitVoice(pending, CompletionId("completion-1"), MemoryMediaId("media-1"), 1.seconds)
                as DataResult.Success
            ).value

        val result = playback.play(committed.reference)

        assertEquals(DataResult.Success(Unit), result)
        assertTrue(playback.observeState().value is VoicePlaybackState.Playing)
        playback.stop()
    }
}

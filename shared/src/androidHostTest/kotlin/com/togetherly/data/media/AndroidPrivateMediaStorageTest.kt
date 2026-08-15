package com.togetherly.data.media

import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.id.IdGenerator
import com.togetherly.core.id.SequentialIdGenerator
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.PendingMediaReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val COMPLETION_ID = CompletionId("completion-1")

/**
 * Exercises the real file-management logic in [AndroidPrivateMediaStorage] against a real temp
 * directory, using fake [PhotoImporter]/[ImageNormalizer]/[ThumbnailGenerator] so no Bitmap
 * decoding is required — this plain JVM unit test has no Robolectric/device dependency, but still
 * proves every path/commit/delete/cleanup behavior this class owns.
 */
class AndroidPrivateMediaStorageTest {

    private lateinit var mediaRoot: TempPrivateMediaRoot
    private lateinit var photoImporter: FakePhotoImporter
    private lateinit var imageNormalizer: FakeImageNormalizer
    private lateinit var thumbnailGenerator: FakeThumbnailGenerator
    private lateinit var storage: AndroidPrivateMediaStorage

    @Before
    fun setUp() {
        mediaRoot = TempPrivateMediaRoot()
        photoImporter = FakePhotoImporter()
        imageNormalizer = FakeImageNormalizer()
        thumbnailGenerator = FakeThumbnailGenerator()
        storage = AndroidPrivateMediaStorage(
            mediaRoot = mediaRoot,
            photoImporter = photoImporter,
            imageNormalizer = imageNormalizer,
            thumbnailGenerator = thumbnailGenerator,
            idGenerator = SequentialIdGenerator("media"),
            // Unconfined rather than a StandardTestDispatcher: this class only ever needs a
            // background dispatcher to satisfy withContext, never virtual time control, and a
            // second independent TestCoroutineScheduler would conflict with runTest's own.
            dispatchers = TestAppDispatchers(Dispatchers.Unconfined),
            diagnostics = FakeOperationalDiagnostics(),
        )
    }

    @After
    fun tearDown() {
        mediaRoot.deleteRecursively()
    }

    @Test
    fun createPendingPhotoWritesANormalizedFileUnderPending() = runTest {
        imageNormalizer.nextResult = DataResult.Success(NormalizedImage(byteArrayOf(1, 2, 3, 4), 111, 222))

        val result = storage.createPendingPhoto(FakePhotoImportSource)

        val pending = (result as DataResult.Success).value
        assertEquals(111, pending.width)
        assertEquals(222, pending.height)
        assertEquals(4L, pending.sizeBytes)
        val file = File(mediaRoot.rootPath(), pending.reference.value)
        assertTrue(file.exists())
        assertEquals(listOf(1.toByte(), 2.toByte(), 3.toByte(), 4.toByte()), file.readBytes().toList())
    }

    @Test
    fun commitPhotoMovesThePendingFileAndWritesAThumbnail() = runTest {
        val pendingResult = storage.createPendingPhoto(FakePhotoImportSource)
        val pending = (pendingResult as DataResult.Success).value

        val result = storage.commitPhoto(pending.reference, COMPLETION_ID, MemoryMediaId("media-1"))

        val committed = (result as DataResult.Success).value
        assertEquals(MediaReference("completions/completion-1/photo-media-1.jpg"), committed.reference)
        assertEquals(MediaReference("completions/completion-1/thumb-media-1.jpg"), committed.thumbnailReference)
        assertTrue(File(mediaRoot.rootPath(), committed.reference.value).exists())
        assertTrue(File(mediaRoot.rootPath(), committed.thumbnailReference.value).exists())
        assertFalse(File(mediaRoot.rootPath(), pending.reference.value).exists())
        assertEquals(1, thumbnailGenerator.thumbnailCalls.size)
    }

    @Test
    fun commitPhotoOnAMissingPendingFileReturnsATypedReadError() = runTest {
        val result = storage.commitPhoto(PendingMediaReference("pending/does-not-exist.jpg"), COMPLETION_ID, MemoryMediaId("media-1"))

        assertEquals(DataResult.Error(AppError.Storage(StorageError.READ_FAILED)), result)
    }

    @Test
    fun duplicatePendingPhotoFilenamesAreCollisionSafe() = runTest {
        val collisionStorage = storageWithIdGenerator(FixedIdGenerator("collision"))

        val first = (collisionStorage.createPendingPhoto(FakePhotoImportSource) as DataResult.Success).value
        val second = (collisionStorage.createPendingPhoto(FakePhotoImportSource) as DataResult.Success).value

        assertNotEquals(first.reference.value, second.reference.value)
        assertTrue(File(mediaRoot.rootPath(), first.reference.value).exists())
        assertTrue(File(mediaRoot.rootPath(), second.reference.value).exists())
    }

    @Test
    fun interruptedPhotoCommitCleansUpThePartialFileAndKeepsThePendingDraft() = runTest {
        val pending = (storage.createPendingPhoto(FakePhotoImportSource) as DataResult.Success).value
        val blockedTarget = File(mediaRoot.rootPath(), "completions/completion-1/photo-media-1.jpg")
        blockedTarget.parentFile?.mkdirs()
        blockedTarget.mkdirs()

        val result = storage.commitPhoto(pending.reference, COMPLETION_ID, MemoryMediaId("media-1"))

        assertTrue(result is DataResult.Error)
        assertTrue(result.error is AppError.Storage)
        assertEquals(StorageError.WRITE_FAILED, (result.error as AppError.Storage).reason)
        assertFalse(blockedTarget.exists())
        assertTrue(File(mediaRoot.rootPath(), pending.reference.value).exists())
    }

    @Test
    fun thumbnailFailureCleansUpTheJustWrittenPhotoAndPreservesThePendingFile() = runTest {
        val pendingResult = storage.createPendingPhoto(FakePhotoImportSource)
        val pending = (pendingResult as DataResult.Success).value
        val error = AppError.Storage(StorageError.WRITE_FAILED)
        thumbnailGenerator.nextResult = DataResult.Error(error)

        val result = storage.commitPhoto(pending.reference, COMPLETION_ID, MemoryMediaId("media-1"))

        assertEquals(DataResult.Error(error), result)
        assertFalse(File(mediaRoot.rootPath(), "completions/completion-1/photo-media-1.jpg").exists())
        assertTrue(File(mediaRoot.rootPath(), pending.reference.value).exists())
    }

    @Test
    fun commitVoiceWritesThePendingBytesAndDeletesTheDraft() = runTest {
        val pendingFile = File(mediaRoot.rootPath(), "pending/pending-voice.m4a").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(4, 5, 6))
        }
        val pending = PendingMediaReference("pending/pending-voice.m4a")

        val result = storage.commitVoice(pending, COMPLETION_ID, MemoryMediaId("voice-1"), 7.seconds)

        val committed = (result as DataResult.Success).value
        assertEquals(MediaReference("completions/completion-1/voice-voice-1.m4a"), committed.reference)
        assertTrue(File(mediaRoot.rootPath(), committed.reference.value).exists())
        assertFalse(pendingFile.exists())
    }

    @Test
    fun deletingCommittedMediaRemovesThePhotoAndItsThumbnailTogether() = runTest {
        val pending = (storage.createPendingPhoto(FakePhotoImportSource) as DataResult.Success).value
        val committed = (storage.commitPhoto(pending.reference, COMPLETION_ID, MemoryMediaId("media-1")) as DataResult.Success).value

        val result = storage.deleteCommitted(committed.reference)

        assertEquals(DataResult.Success(Unit), result)
        assertFalse(File(mediaRoot.rootPath(), committed.reference.value).exists())
        assertFalse(File(mediaRoot.rootPath(), committed.thumbnailReference.value).exists())
    }

    @Test
    fun deletingTheLastCommittedMediaRemovesTheNowEmptyCompletionDirectory() = runTest {
        val pending = (storage.createPendingPhoto(FakePhotoImportSource) as DataResult.Success).value
        val committed = (storage.commitPhoto(pending.reference, COMPLETION_ID, MemoryMediaId("media-1")) as DataResult.Success).value

        storage.deleteCommitted(committed.reference)

        assertFalse(File(mediaRoot.rootPath(), "completions/completion-1").exists())
    }

    @Test
    fun deletingCommittedMediaIsIdempotent() = runTest {
        val pending = (storage.createPendingPhoto(FakePhotoImportSource) as DataResult.Success).value
        val committed = (storage.commitPhoto(pending.reference, COMPLETION_ID, MemoryMediaId("media-1")) as DataResult.Success).value

        val first = storage.deleteCommitted(committed.reference)
        val second = storage.deleteCommitted(committed.reference)

        assertEquals(DataResult.Success(Unit), first)
        assertEquals(DataResult.Success(Unit), second)
    }

    @Test
    fun deletingPendingIsIdempotent() = runTest {
        val pending = (storage.createPendingPhoto(FakePhotoImportSource) as DataResult.Success).value

        val first = storage.deletePending(pending.reference)
        val second = storage.deletePending(pending.reference)

        assertEquals(DataResult.Success(Unit), first)
        assertEquals(DataResult.Success(Unit), second)
        assertFalse(File(mediaRoot.rootPath(), pending.reference.value).exists())
    }

    @Test
    fun deletingOneCommittedMemoryLeavesTheOtherAndEventuallyRemovesTheDirectory() = runTest {
        val pendingPhoto = (storage.createPendingPhoto(FakePhotoImportSource) as DataResult.Success).value
        val photo = (storage.commitPhoto(pendingPhoto.reference, COMPLETION_ID, MemoryMediaId("media-1")) as DataResult.Success).value

        val pendingVoiceFile = File(mediaRoot.rootPath(), "pending/pending-voice.m4a").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(8, 9, 10))
        }
        val voice = (storage.commitVoice(PendingMediaReference("pending/pending-voice.m4a"), COMPLETION_ID, MemoryMediaId("voice-1"), 3.seconds) as DataResult.Success).value

        assertTrue(File(mediaRoot.rootPath(), photo.reference.value).exists())
        assertTrue(File(mediaRoot.rootPath(), voice.reference.value).exists())

        assertEquals(DataResult.Success(Unit), storage.deleteCommitted(photo.reference))
        assertFalse(File(mediaRoot.rootPath(), photo.reference.value).exists())
        assertTrue(File(mediaRoot.rootPath(), voice.reference.value).exists())

        assertEquals(DataResult.Success(Unit), storage.deleteCommitted(voice.reference))
        assertFalse(File(mediaRoot.rootPath(), voice.reference.value).exists())
        assertFalse(File(mediaRoot.rootPath(), "completions/completion-1").exists())

        assertEquals(DataResult.Success(Unit), storage.deleteCommitted(voice.reference))
    }

    @Test
    fun pathTraversalIsRejectedBeforeTouchingTheFilesystem() = runTest {
        val traversalReference = MediaReference("completions/../../outside.jpg")

        val deleteResult = storage.deleteCommitted(traversalReference)
        val openResult = storage.openPhoto(traversalReference)
        val commitResult = storage.commitPhoto(PendingMediaReference("../../outside.jpg"), COMPLETION_ID, MemoryMediaId("media-1"))

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT)), deleteResult)
        assertEquals(DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT)), openResult)
        assertEquals(DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT)), commitResult)
    }

    // openPhoto always decodes real image bounds from the committed bytes (no dimensions sidecar
    // exists for already-committed photos) — that path needs a real Bitmap decoder, so it's
    // covered by AndroidPrivateMediaStorageDeviceTest (instrumented) instead of here.

    @Test
    fun orphanScanDeletesExpiredPendingFilesButPreservesRecentOnes() = runTest {
        val old = (storage.createPendingPhoto(FakePhotoImportSource) as DataResult.Success).value
        val recent = (storage.createPendingPhoto(FakePhotoImportSource) as DataResult.Success).value
        val oldFile = File(mediaRoot.rootPath(), old.reference.value)
        val recentFile = File(mediaRoot.rootPath(), recent.reference.value)
        val oldSidecar = File(mediaRoot.rootPath(), "${old.reference.value}.dims")
        val recentSidecar = File(mediaRoot.rootPath(), "${recent.reference.value}.dims")
        val now = Instant.parse("2026-06-15T12:00:00Z")
        oldFile.setLastModified(now.toEpochMilliseconds() - 25.hoursInMillis())
        oldSidecar.setLastModified(now.toEpochMilliseconds() - 25.hoursInMillis())
        recentFile.setLastModified(now.toEpochMilliseconds() - 1.hoursInMillis())
        recentSidecar.setLastModified(now.toEpochMilliseconds() - 1.hoursInMillis())

        val result = storage.deleteExpiredPending(now)

        assertEquals(DataResult.Success(2), result)
        assertFalse(oldFile.exists())
        assertFalse(oldSidecar.exists())
        assertTrue(recentFile.exists())
        assertTrue(recentSidecar.exists())
    }

    private fun Int.hoursInMillis(): Long = this * 60L * 60L * 1000L

    private fun storageWithIdGenerator(idGenerator: IdGenerator): AndroidPrivateMediaStorage =
        AndroidPrivateMediaStorage(
            mediaRoot = mediaRoot,
            photoImporter = photoImporter,
            imageNormalizer = imageNormalizer,
            thumbnailGenerator = thumbnailGenerator,
            idGenerator = idGenerator,
            dispatchers = TestAppDispatchers(Dispatchers.Unconfined),
            diagnostics = FakeOperationalDiagnostics(),
        )

    private class FixedIdGenerator(private val id: String) : IdGenerator {
        override fun generate(): String = id
    }
}

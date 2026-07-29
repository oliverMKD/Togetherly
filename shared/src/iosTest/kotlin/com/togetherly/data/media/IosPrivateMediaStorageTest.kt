package com.togetherly.data.media

import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.id.SequentialIdGenerator
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.PendingMediaReference
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

private val COMPLETION_ID = CompletionId("completion-1")

/** A real, valid 1x1 transparent GIF (42 bytes) — small enough to inline, but genuinely decodable. */
private val ONE_PIXEL_GIF = byteArrayOf(
    71, 73, 70, 56, 57, 97, 1, 0, 1, 0, (-128).toByte(), 0, 0, 0, 0, 0,
    (-1).toByte(), (-1).toByte(), (-1).toByte(), 33, (-7).toByte(), 4, 1, 0, 0, 0, 0, 44,
    0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 1, 76, 0, 59,
)

/**
 * The iOS counterpart to `AndroidPrivateMediaStorageTest` — same fakes, same assertions, run for
 * real against the simulator's filesystem via [NSFileManager] rather than `java.io.File`.
 */
@OptIn(ExperimentalForeignApi::class)
class IosPrivateMediaStorageTest {

    private lateinit var mediaRoot: TempPrivateMediaRoot
    private lateinit var imageNormalizer: FakeImageNormalizer
    private lateinit var thumbnailGenerator: FakeThumbnailGenerator
    private lateinit var storage: IosPrivateMediaStorage

    private fun exists(relativeReference: String): Boolean =
        NSFileManager.defaultManager.fileExistsAtPath("${mediaRoot.rootPath()}/$relativeReference")

    @BeforeTest
    fun setUp() {
        mediaRoot = TempPrivateMediaRoot()
        imageNormalizer = FakeImageNormalizer()
        thumbnailGenerator = FakeThumbnailGenerator()
        storage = IosPrivateMediaStorage(
            mediaRoot = mediaRoot,
            photoImporter = FakePhotoImporter(),
            imageNormalizer = imageNormalizer,
            thumbnailGenerator = thumbnailGenerator,
            idGenerator = SequentialIdGenerator("media"),
            dispatchers = TestAppDispatchers(Dispatchers.Unconfined),
            diagnostics = FakeOperationalDiagnostics(),
        )
    }

    @AfterTest
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
        assertTrue(exists(pending.reference.value))
    }

    @Test
    fun commitPhotoMovesThePendingFileAndWritesAThumbnail() = runTest {
        val pending = (storage.createPendingPhoto(FakePhotoImportSource) as DataResult.Success).value

        val result = storage.commitPhoto(pending.reference, COMPLETION_ID, MemoryMediaId("media-1"))

        val committed = (result as DataResult.Success).value
        assertEquals(MediaReference("completions/completion-1/photo-media-1.jpg"), committed.reference)
        assertEquals(MediaReference("completions/completion-1/thumb-media-1.jpg"), committed.thumbnailReference)
        assertTrue(exists(committed.reference.value))
        assertTrue(exists(committed.thumbnailReference.value))
        assertFalse(exists(pending.reference.value))
        assertEquals(1, thumbnailGenerator.thumbnailCalls.size)
    }

    @Test
    fun commitPhotoOnAMissingPendingFileReturnsATypedReadError() = runTest {
        val result = storage.commitPhoto(PendingMediaReference("pending/does-not-exist.jpg"), COMPLETION_ID, MemoryMediaId("media-1"))

        assertEquals(DataResult.Error(AppError.Storage(StorageError.READ_FAILED)), result)
    }

    @Test
    fun thumbnailFailureCleansUpTheJustWrittenPhotoAndPreservesThePendingFile() = runTest {
        val pending = (storage.createPendingPhoto(FakePhotoImportSource) as DataResult.Success).value
        val error = AppError.Storage(StorageError.WRITE_FAILED)
        thumbnailGenerator.nextResult = DataResult.Error(error)

        val result = storage.commitPhoto(pending.reference, COMPLETION_ID, MemoryMediaId("media-1"))

        assertEquals(DataResult.Error(error), result)
        assertFalse(exists("completions/completion-1/photo-media-1.jpg"))
        assertTrue(exists(pending.reference.value))
    }

    @Test
    fun deletingCommittedMediaRemovesThePhotoAndItsThumbnailTogether() = runTest {
        val pending = (storage.createPendingPhoto(FakePhotoImportSource) as DataResult.Success).value
        val committed = (storage.commitPhoto(pending.reference, COMPLETION_ID, MemoryMediaId("media-1")) as DataResult.Success).value

        val result = storage.deleteCommitted(committed.reference)

        assertEquals(DataResult.Success(Unit), result)
        assertFalse(exists(committed.reference.value))
        assertFalse(exists(committed.thumbnailReference.value))
    }

    @Test
    fun deletingTheLastCommittedMediaRemovesTheNowEmptyCompletionDirectory() = runTest {
        val pending = (storage.createPendingPhoto(FakePhotoImportSource) as DataResult.Success).value
        val committed = (storage.commitPhoto(pending.reference, COMPLETION_ID, MemoryMediaId("media-1")) as DataResult.Success).value

        storage.deleteCommitted(committed.reference)

        assertFalse(exists("completions/completion-1"))
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
        assertFalse(exists(pending.reference.value))
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

    @Test
    fun openPhotoReadsBackTheCommittedBytesAndRealDecodedDimensions() = runTest {
        // A real, valid 1x1 transparent GIF — openPhoto has no dimensions sidecar to fall back on
        // (that only exists for pending files), so it always decodes the real committed bytes;
        // this proves that decode actually runs and reports the true 1x1 size, not whatever the
        // (unrelated, and here deliberately different) fake normalizer claimed at import time.
        imageNormalizer.nextResult = DataResult.Success(NormalizedImage(ONE_PIXEL_GIF, 50, 60))
        val pending = (storage.createPendingPhoto(FakePhotoImportSource) as DataResult.Success).value
        val committed = (storage.commitPhoto(pending.reference, COMPLETION_ID, MemoryMediaId("media-1")) as DataResult.Success).value

        val result = storage.openPhoto(committed.reference)

        val photoData = (result as DataResult.Success).value
        assertEquals(ONE_PIXEL_GIF.toList(), photoData.bytes.toList())
        assertEquals(1, photoData.width)
        assertEquals(1, photoData.height)
    }

    @Test
    fun orphanScanDeletesExpiredPendingFilesButPreservesRecentOnes() = runTest {
        val old = (storage.createPendingPhoto(FakePhotoImportSource) as DataResult.Success).value
        val recent = (storage.createPendingPhoto(FakePhotoImportSource) as DataResult.Success).value
        val oldPath = "${mediaRoot.rootPath()}/${old.reference.value}"
        val recentPath = "${mediaRoot.rootPath()}/${recent.reference.value}"
        val now = Instant.parse("2026-06-15T12:00:00Z")
        setModificationDate(oldPath, now.toEpochMilliseconds() - 25.hoursInMillis())
        setModificationDate(recentPath, now.toEpochMilliseconds() - 1.hoursInMillis())

        val result = storage.deleteExpiredPending(now)

        assertEquals(DataResult.Success(1), result)
        assertFalse(NSFileManager.defaultManager.fileExistsAtPath(oldPath))
        assertTrue(NSFileManager.defaultManager.fileExistsAtPath(recentPath))
    }

    private fun Int.hoursInMillis(): Long = this * 60L * 60L * 1000L

    private fun setModificationDate(path: String, epochMillis: Long) {
        val referenceSeconds = epochMillis / 1000.0 - REFERENCE_DATE_OFFSET_SECONDS
        val date = NSDate(timeIntervalSinceReferenceDate = referenceSeconds)
        NSFileManager.defaultManager.setAttributes(mapOf(NSFileModificationDate to date), ofItemAtPath = path, error = null)
    }
}

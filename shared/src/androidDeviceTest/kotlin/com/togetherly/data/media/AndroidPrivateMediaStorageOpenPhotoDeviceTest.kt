package com.togetherly.data.media

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.id.SequentialIdGenerator
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MemoryMediaId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals

/**
 * Covers `openPhoto`'s real decode path (it has no dimensions sidecar to fall back on — only
 * pending files get one), which needs a real Bitmap decoder unavailable in `androidHostTest`.
 * Requires a connected device or emulator to actually run.
 */
@RunWith(AndroidJUnit4::class)
class AndroidPrivateMediaStorageOpenPhotoDeviceTest {

    private lateinit var mediaRoot: TempPrivateMediaRoot
    private lateinit var storage: AndroidPrivateMediaStorage

    @Before
    fun setUp() {
        mediaRoot = TempPrivateMediaRoot()
        storage = AndroidPrivateMediaStorage(
            mediaRoot = mediaRoot,
            photoImporter = FakePhotoImporter(),
            imageNormalizer = AndroidImageNormalizer(),
            thumbnailGenerator = AndroidThumbnailGenerator(),
            idGenerator = SequentialIdGenerator("media"),
            dispatchers = TestAppDispatchers(Dispatchers.Unconfined),
            diagnostics = FakeOperationalDiagnostics(),
        )
    }

    @After
    fun tearDown() {
        mediaRoot.deleteRecursively()
    }

    @Test
    fun openPhotoDecodesTheRealCommittedImageDimensions() = runTest {
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        bitmap.recycle()
        val fakeImporter = FakePhotoImporter(DataResult.Success(RawImageBytes(output.toByteArray(), output.size().toLong())))
        val realStorage = AndroidPrivateMediaStorage(
            mediaRoot = mediaRoot,
            photoImporter = fakeImporter,
            imageNormalizer = AndroidImageNormalizer(),
            thumbnailGenerator = AndroidThumbnailGenerator(),
            idGenerator = SequentialIdGenerator("media"),
            dispatchers = TestAppDispatchers(Dispatchers.Unconfined),
            diagnostics = FakeOperationalDiagnostics(),
        )
        val pending = (realStorage.createPendingPhoto(FakePhotoImportSource) as DataResult.Success).value
        val committed = (realStorage.commitPhoto(pending.reference, CompletionId("completion-1"), MemoryMediaId("media-1")) as DataResult.Success).value

        val result = realStorage.openPhoto(committed.reference)

        val photoData = (result as DataResult.Success).value
        assertEquals(120, photoData.width)
        assertEquals(80, photoData.height)
    }
}

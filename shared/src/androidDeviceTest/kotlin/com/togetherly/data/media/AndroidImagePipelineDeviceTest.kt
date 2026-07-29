package com.togetherly.data.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun realJpegBytes(width: Int, height: Int, quality: Int = 100): ByteArray {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val output = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
    bitmap.recycle()
    return output.toByteArray()
}

/**
 * Exercises the real Bitmap-backed [AndroidImageNormalizer]/[AndroidThumbnailGenerator] — these
 * need a real Android runtime (no Robolectric is configured in this project), so they live here
 * rather than in `androidHostTest`. Requires a connected device or emulator to actually run.
 */
@RunWith(AndroidJUnit4::class)
class AndroidImagePipelineDeviceTest {

    @Test
    fun normalizingAValidImageReportsItsDimensions() = runTest {
        val normalizer = AndroidImageNormalizer()
        val raw = RawImageBytes(realJpegBytes(300, 200), realJpegBytes(300, 200).size.toLong())

        val result = normalizer.normalize(raw, maxDimension = 2048, jpegQuality = 85)

        val normalized = (result as DataResult.Success).value
        assertEquals(300, normalized.width)
        assertEquals(200, normalized.height)
    }

    @Test
    fun normalizingNeverUpscalesASmallerImage() = runTest {
        val normalizer = AndroidImageNormalizer()
        val raw = RawImageBytes(realJpegBytes(50, 40), 0)

        val result = normalizer.normalize(raw, maxDimension = 2048, jpegQuality = 85)

        val normalized = (result as DataResult.Success).value
        assertEquals(50, normalized.width)
        assertEquals(40, normalized.height)
    }

    @Test
    fun normalizingDownscalesToTheMaximumDimension() = runTest {
        val normalizer = AndroidImageNormalizer()
        val raw = RawImageBytes(realJpegBytes(4000, 2000), 0)

        val result = normalizer.normalize(raw, maxDimension = 1000, jpegQuality = 85)

        val normalized = (result as DataResult.Success).value
        assertEquals(1000, normalized.width)
        assertEquals(500, normalized.height)
    }

    @Test
    fun normalizingRejectsEmptyBytes() = runTest {
        val normalizer = AndroidImageNormalizer()

        val result = normalizer.normalize(RawImageBytes(ByteArray(0), 0), 2048, 85)

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.INVALID_MEDIA)), result)
    }

    @Test
    fun normalizingRejectsAnOversizedInput() = runTest {
        val normalizer = AndroidImageNormalizer()
        val raw = RawImageBytes(byteArrayOf(1), ImageLimits.MAX_INPUT_SIZE_BYTES + 1)

        val result = normalizer.normalize(raw, 2048, 85)

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.MEDIA_TOO_LARGE)), result)
    }

    @Test
    fun thumbnailGenerationProducesASmallerImage() = runTest {
        val generator = AndroidThumbnailGenerator()
        val source = realJpegBytes(1000, 1000)

        val result = generator.generateThumbnail(source, maxDimension = 200, jpegQuality = 80)

        val thumbnailBytes = (result as DataResult.Success).value
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.size, options)
        assertEquals(200, options.outWidth)
        assertTrue(thumbnailBytes.size < source.size)
    }
}

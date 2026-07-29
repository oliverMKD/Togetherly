package com.togetherly.data.media

import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.core.result.runCatchingData

internal class AndroidImageNormalizer : ImageNormalizer {

    override suspend fun normalize(
        raw: RawImageBytes,
        maxDimension: Int,
        jpegQuality: Int,
    ): DataResult<NormalizedImage> {
        if (raw.bytes.isEmpty()) return DataResult.Error(AppError.Validation(ValidationError.INVALID_MEDIA))
        if (raw.sizeBytes > ImageLimits.MAX_INPUT_SIZE_BYTES) {
            return DataResult.Error(AppError.Validation(ValidationError.MEDIA_TOO_LARGE))
        }

        return runCatchingData {
            val orientation = AndroidBitmapUtils.readOrientation(raw.bytes)
            val decoded = AndroidBitmapUtils.decode(raw.bytes)
            val upright = AndroidBitmapUtils.applyOrientation(decoded, orientation)
            val resized = AndroidBitmapUtils.downscaleIfNeeded(upright, maxDimension)
            val bytes = AndroidBitmapUtils.encodeJpeg(resized, jpegQuality)
            val width = resized.width
            val height = resized.height
            resized.recycle()
            NormalizedImage(bytes = bytes, width = width, height = height)
        }
    }
}

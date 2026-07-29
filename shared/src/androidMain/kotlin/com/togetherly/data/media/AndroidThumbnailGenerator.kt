package com.togetherly.data.media

import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.core.result.runCatchingData

internal class AndroidThumbnailGenerator : ThumbnailGenerator {

    override suspend fun generateThumbnail(
        normalizedImageBytes: ByteArray,
        maxDimension: Int,
        jpegQuality: Int,
    ): DataResult<ByteArray> {
        if (normalizedImageBytes.isEmpty()) return DataResult.Error(AppError.Validation(ValidationError.INVALID_MEDIA))

        return runCatchingData {
            val decoded = AndroidBitmapUtils.decode(normalizedImageBytes)
            val scaled = AndroidBitmapUtils.downscaleIfNeeded(decoded, maxDimension)
            val bytes = AndroidBitmapUtils.encodeJpeg(scaled, jpegQuality)
            scaled.recycle()
            bytes
        }
    }
}

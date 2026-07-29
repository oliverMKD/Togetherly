package com.togetherly.data.media

import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.core.result.runCatchingData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

@OptIn(ExperimentalForeignApi::class)
internal class IosThumbnailGenerator : ThumbnailGenerator {

    override suspend fun generateThumbnail(
        normalizedImageBytes: ByteArray,
        maxDimension: Int,
        jpegQuality: Int,
    ): DataResult<ByteArray> {
        if (normalizedImageBytes.isEmpty()) return DataResult.Error(AppError.Validation(ValidationError.INVALID_MEDIA))

        return runCatchingData {
            val decoded = UIImage.imageWithData(normalizedImageBytes.toNSData())
                ?: throw IllegalArgumentException("Could not decode image bytes")
            val resized = IosImageUtils.downscaleIfNeeded(decoded, maxDimension)
            val jpegData = UIImageJPEGRepresentation(resized, jpegQuality / 100.0)
                ?: throw IllegalStateException("Could not encode JPEG")
            jpegData.toByteArray()
        }
    }
}

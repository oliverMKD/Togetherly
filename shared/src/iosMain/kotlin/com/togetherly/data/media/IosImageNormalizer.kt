package com.togetherly.data.media

import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.core.result.runCatchingData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

@OptIn(ExperimentalForeignApi::class)
internal class IosImageNormalizer : ImageNormalizer {

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
            val decoded = UIImage.imageWithData(raw.bytes.toNSData())
                ?: throw IllegalArgumentException("Could not decode image bytes")
            val upright = IosImageUtils.redrawUpright(decoded)
            val resized = IosImageUtils.downscaleIfNeeded(upright, maxDimension)
            val jpegData = UIImageJPEGRepresentation(resized, jpegQuality / 100.0)
                ?: throw IllegalStateException("Could not encode JPEG")
            val (width, height) = IosImageUtils.dimensions(resized)
            NormalizedImage(bytes = jpegData.toByteArray(), width = width, height = height)
        }
    }
}

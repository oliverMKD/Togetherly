package com.togetherly.data.media

import com.togetherly.core.result.DataResult

/**
 * Produces a small JPEG for Journey from bytes that have already been through [ImageNormalizer] —
 * orientation and metadata are already handled by that point, so this only needs to downscale
 * and re-encode, never upscaling past the source image's own size.
 */
interface ThumbnailGenerator {
    suspend fun generateThumbnail(
        normalizedImageBytes: ByteArray,
        maxDimension: Int = ImageLimits.THUMBNAIL_MAX_DIMENSION,
        jpegQuality: Int = ImageLimits.THUMBNAIL_JPEG_QUALITY,
    ): DataResult<ByteArray>
}

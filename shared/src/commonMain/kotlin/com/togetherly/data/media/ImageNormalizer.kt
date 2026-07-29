package com.togetherly.data.media

import com.togetherly.core.result.DataResult

/** Tunable limits shared by every platform's [ImageNormalizer]/[ThumbnailGenerator]. */
object ImageLimits {
    const val MAX_DIMENSION = 2048
    const val JPEG_QUALITY = 85
    const val THUMBNAIL_MAX_DIMENSION = 320
    const val THUMBNAIL_JPEG_QUALITY = 80

    /** Rejects a suspiciously huge input before it's ever decoded, to bound memory use. */
    const val MAX_INPUT_SIZE_BYTES = 25L * 1024 * 1024
}

data class NormalizedImage(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
)

/**
 * Decodes [RawImageBytes], corrects orientation by baking it into the pixels (never trusting a
 * downstream reader to honor EXIF orientation), downscales to at most [maxDimension] on the
 * longer side — never upscaling a smaller image — and re-encodes as JPEG at [jpegQuality]. The
 * re-encode step is also what strips GPS/device/original-filename metadata: the output is a fresh
 * image built only from decoded pixels, never a byte-for-byte copy of the input file.
 */
interface ImageNormalizer {
    suspend fun normalize(
        raw: RawImageBytes,
        maxDimension: Int = ImageLimits.MAX_DIMENSION,
        jpegQuality: Int = ImageLimits.JPEG_QUALITY,
    ): DataResult<NormalizedImage>
}

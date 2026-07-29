package com.togetherly.data.media

import com.togetherly.core.result.DataResult

class FakeThumbnailGenerator(
    var nextResult: DataResult<ByteArray> = DataResult.Success(byteArrayOf(5, 5, 5)),
) : ThumbnailGenerator {
    val thumbnailCalls = mutableListOf<ByteArray>()

    override suspend fun generateThumbnail(normalizedImageBytes: ByteArray, maxDimension: Int, jpegQuality: Int): DataResult<ByteArray> {
        thumbnailCalls += normalizedImageBytes
        return nextResult
    }
}

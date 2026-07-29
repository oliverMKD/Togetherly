package com.togetherly.data.media

import com.togetherly.core.result.DataResult

class FakeImageNormalizer(
    var nextResult: DataResult<NormalizedImage> = DataResult.Success(NormalizedImage(byteArrayOf(9, 9, 9), 10, 20)),
) : ImageNormalizer {
    val normalizeCalls = mutableListOf<RawImageBytes>()

    override suspend fun normalize(raw: RawImageBytes, maxDimension: Int, jpegQuality: Int): DataResult<NormalizedImage> {
        normalizeCalls += raw
        return nextResult
    }
}

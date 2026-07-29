package com.togetherly.data.media

import com.togetherly.core.result.DataResult

class FakePhotoImporter(
    var nextResult: DataResult<RawImageBytes> = DataResult.Success(RawImageBytes(byteArrayOf(1, 2, 3), 3L)),
) : PhotoImporter {
    val importCalls = mutableListOf<PhotoImportSource>()

    override suspend fun import(source: PhotoImportSource): DataResult<RawImageBytes> {
        importCalls += source
        return nextResult
    }
}

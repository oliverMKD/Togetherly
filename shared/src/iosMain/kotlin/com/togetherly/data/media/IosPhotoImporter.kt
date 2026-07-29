package com.togetherly.data.media

import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.core.result.runCatchingData

internal class IosPhotoImporter : PhotoImporter {

    override suspend fun import(source: PhotoImportSource): DataResult<RawImageBytes> {
        val iosSource = source as? IosPhotoImportSource
            ?: return DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))

        return runCatchingData {
            val bytes = iosSource.data.toByteArray()
            RawImageBytes(bytes = bytes, sizeBytes = bytes.size.toLong())
        }
    }
}

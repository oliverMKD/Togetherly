package com.togetherly.data.media

import android.content.Context
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.core.result.runCatchingData
import org.koin.mp.KoinPlatform

/**
 * Opens the picked [Uri][android.net.Uri] through the app's own [android.content.ContentResolver]
 * and reads it once into memory — the [Uri] itself is never retained past this call. Resolves the
 * application [Context] the same way [com.togetherly.data.local.database.createDatabaseBuilder]
 * does, via [KoinPlatform], since this class's constructor signature has no room for one.
 */
internal class AndroidPhotoImporter : PhotoImporter {

    override suspend fun import(source: PhotoImportSource): DataResult<RawImageBytes> {
        val androidSource = source as? AndroidPhotoImportSource
            ?: return DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))

        return runCatchingData {
            val appContext = KoinPlatform.getKoin().get<Context>().applicationContext
            val bytes = appContext.contentResolver.openInputStream(androidSource.uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Unable to open the picked photo")
            RawImageBytes(bytes = bytes, sizeBytes = bytes.size.toLong())
        }
    }
}

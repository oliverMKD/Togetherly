package com.togetherly.data.media

import com.togetherly.core.error.AppError

sealed interface PhotoPickerResult {
    data class Imported(
        val pendingPhoto: PendingPhoto,
    ) : PhotoPickerResult

    data object Cancelled : PhotoPickerResult

    data class Failure(
        val error: AppError,
    ) : PhotoPickerResult
}

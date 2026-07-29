package com.togetherly.data.local.keys

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult

/** A single, reused typed error for every unrecognized stored enum key — never a silent default. */
internal fun <T> unknownStorageKey(): DataResult<T> =
    DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED))

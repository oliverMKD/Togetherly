package com.togetherly.data.local.mapper

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.validation.DomainValidationException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** The one typed error every corrupted-row mapping failure returns — never a silent default. */
internal fun <T> corruptedStorage(): DataResult<T> =
    DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED))

internal fun parseStoredLocalDate(value: String): DataResult<LocalDate> = try {
    DataResult.Success(LocalDate.parse(value))
} catch (e: IllegalArgumentException) {
    corruptedStorage()
}

internal fun parseStoredLocalTime(value: String): DataResult<LocalTime> = try {
    DataResult.Success(LocalTime.parse(value))
} catch (e: IllegalArgumentException) {
    corruptedStorage()
}

/**
 * Constructs a domain value, converting a [DomainValidationException] — e.g. an invalid
 * timestamp relationship, or a row that otherwise fails a domain invariant — into a typed
 * corrupted-storage error instead of letting it escape uncontrolled. Mirrors
 * [com.togetherly.content.mapper.mapCatching]'s role at the content boundary.
 */
internal inline fun <T> mapStorageCatching(block: () -> T): DataResult<T> = try {
    DataResult.Success(block())
} catch (e: DomainValidationException) {
    corruptedStorage()
}

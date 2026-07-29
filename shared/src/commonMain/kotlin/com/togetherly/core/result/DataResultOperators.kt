package com.togetherly.core.result

import com.togetherly.core.error.AppError

inline fun <T, R> DataResult<T>.map(
    transform: (T) -> R,
): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(value))
    is DataResult.Error -> this
}

inline fun <T, R> DataResult<T>.flatMap(
    transform: (T) -> DataResult<R>,
): DataResult<R> = when (this) {
    is DataResult.Success -> transform(value)
    is DataResult.Error -> this
}

inline fun <T> DataResult<T>.onSuccess(
    action: (T) -> Unit,
): DataResult<T> {
    if (this is DataResult.Success) action(value)
    return this
}

inline fun <T> DataResult<T>.onError(
    action: (AppError) -> Unit,
): DataResult<T> {
    if (this is DataResult.Error) action(error)
    return this
}

/**
 * Unwraps a [DataResult.Success] value, or non-locally returns from the calling function via
 * [onError] on [DataResult.Error] — lets multi-field mapping code read as a flat sequence of
 * unwraps instead of a chain of `is DataResult.Error` early-return checks.
 */
inline fun <T> DataResult<T>.getOrElse(
    onError: (AppError) -> Nothing,
): T = when (this) {
    is DataResult.Success -> value
    is DataResult.Error -> onError(error)
}

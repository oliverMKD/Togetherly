package com.togetherly.core.result

import com.togetherly.core.error.AppError
import kotlinx.coroutines.CancellationException

/**
 * A last-resort safety net for genuinely unanticipated failures, not a general error-mapping
 * strategy — future repository implementations should catch and map specific exceptions to
 * specific [AppError] variants explicitly, since mapping everything to [AppError.Unexpected]
 * here would hide meaningful errors. Coroutine cancellation is always rethrown, never wrapped.
 */
inline fun <T> runCatchingData(
    block: () -> T,
): DataResult<T> = try {
    DataResult.Success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    DataResult.Error(AppError.Unexpected(cause = throwable))
}

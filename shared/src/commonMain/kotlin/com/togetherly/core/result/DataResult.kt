package com.togetherly.core.result

import com.togetherly.core.error.AppError

/**
 * The result of a repository operation: either the value produced, or a typed [AppError].
 * There is no loading state — loading is presentation state, not a repository result.
 *
 * Conventions for future repositories:
 * - Observable state uses `Flow<DataResult<T>>`. [Success] carries the current source-of-truth
 *   value; [Error] represents a read/observation failure. Loading is never represented, and a
 *   Flow must never silently swallow an error — infrastructure failures are mapped into
 *   [AppError] rather than dropped. Coroutine cancellation always propagates normally.
 * - One-shot reads/writes use `suspend fun operation(): DataResult<T>`.
 * - Successful writes or deletes that produce no value use `DataResult<Unit>` — there is no
 *   separate operation-result type.
 */
sealed interface DataResult<out T> {

    data class Success<T>(
        val value: T,
    ) : DataResult<T>

    data class Error(
        val error: AppError,
    ) : DataResult<Nothing>
}

package com.togetherly.data

import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.DiagnosticContext
import com.togetherly.core.telemetry.OperationalDiagnostics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext

private val persistenceFailureContext = DiagnosticContext(mapOf("feature" to "persistence", "operation" to "storage_operation"))
private val persistenceReadFailureContext = DiagnosticContext(mapOf("feature" to "persistence", "operation" to "storage_read"))

/**
 * The one shared database-operation helper for every Room-backed repository: runs [block] on
 * [AppDispatchers.io], rethrows [CancellationException] untouched, and maps any other exception
 * to a typed [AppError.Storage] with [reason] — never a raw Room/SQLite exception, never a
 * generic [AppError.Unexpected]. [block] returns [DataResult] itself rather than a raw value, so
 * a mapper's own [AppError.Storage.DATA_CORRUPTED][com.togetherly.core.error.StorageError.DATA_CORRUPTED]
 * result passes through unwrapped instead of being nested inside another `Success`.
 *
 * Every caught exception is also reported via [diagnostics] — this is the "persistence failure"
 * capture boundary — before being wrapped into the returned [AppError.Storage].
 */
internal suspend fun <T> AppDispatchers.runCatchingStorage(
    reason: StorageError,
    diagnostics: OperationalDiagnostics,
    block: suspend () -> DataResult<T>,
): DataResult<T> = withContext(io) {
    try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (e: Exception) {
        diagnostics.captureHandledException(e, persistenceFailureContext)
        DataResult.Error(AppError.Storage(reason, cause = e))
    }
}

/**
 * The shared `observe*` error path: rethrows [CancellationException], and maps any other
 * exception from the underlying Room [Flow] to a typed [AppError.Storage] emission rather than
 * letting it terminate the flow uncaught. Reports the exception via [diagnostics] before mapping
 * it — the "persistence failure" capture boundary for read flows.
 */
internal fun <T> Flow<DataResult<T>>.catchStorageReadErrors(diagnostics: OperationalDiagnostics): Flow<DataResult<T>> = catch { throwable ->
    if (throwable is CancellationException) throw throwable
    diagnostics.captureHandledException(throwable, persistenceReadFailureContext)
    emit(DataResult.Error(AppError.Storage(StorageError.READ_FAILED, cause = throwable)))
}

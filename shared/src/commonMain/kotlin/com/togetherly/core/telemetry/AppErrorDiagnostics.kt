package com.togetherly.core.telemetry

import com.togetherly.core.error.AppError

/**
 * Extracts the most useful [Throwable] for [OperationalDiagnostics.captureHandledException] from
 * an [AppError] — the error's own `cause` when present, otherwise a synthetic exception naming its
 * reason enum. Safe either way: every reason enum is Togetherly's own closed, predefined,
 * non-personal vocabulary (`ContentError`/`StorageError`/`PurchaseError`/…), never user text, so
 * embedding one directly in a diagnostic exception message is never a privacy concern the way an
 * arbitrary caught exception's own message can be.
 */
fun AppError.toDiagnosticThrowable(): Throwable = when (this) {
    is AppError.Content -> cause ?: IllegalStateException("Content error: $reason")
    is AppError.Storage -> cause ?: IllegalStateException("Storage error: $reason")
    is AppError.Purchase -> cause ?: IllegalStateException("Purchase error: $reason")
    is AppError.Unexpected -> cause ?: IllegalStateException("Unexpected error")
    is AppError.Validation -> IllegalStateException("Validation error: $reason")
    is AppError.Permission -> IllegalStateException("Permission error: $reason")
}

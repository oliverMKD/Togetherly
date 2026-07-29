package com.togetherly.core.error

import com.togetherly.domain.purchase.PurchaseError

/**
 * Every [cause] is retained only for logging and diagnostics: it must never be surfaced as
 * user-facing copy, never be branched on by platform-specific exception type in domain code,
 * and must never carry family memory content in its message.
 */
sealed interface AppError {

    data class Validation(
        val reason: ValidationError,
    ) : AppError

    data class Storage(
        val reason: StorageError,
        val cause: Throwable? = null,
    ) : AppError

    data class Content(
        val reason: ContentError,
        val cause: Throwable? = null,
    ) : AppError

    data class Purchase(
        val reason: PurchaseError,
        val cause: Throwable? = null,
    ) : AppError

    data class Permission(
        val reason: PermissionError,
    ) : AppError

    data class Unexpected(
        val cause: Throwable? = null,
    ) : AppError
}

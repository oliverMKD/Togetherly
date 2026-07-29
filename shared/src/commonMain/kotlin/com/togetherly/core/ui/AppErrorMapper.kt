package com.togetherly.core.ui

import com.togetherly.core.error.AppError
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.error_generic_message

/**
 * Maps an [AppError] onto user-safe copy. Every branch resolves to the same generic message today
 * because [AppError]'s own contract already forbids surfacing [AppError.cause]/reason enums as
 * user-facing text (see its KDoc) — there is nothing branch-specific left to say to a family that
 * would be both true and safe to show. The `when` stays exhaustive rather than collapsing to a
 * single `else` so a newly added [AppError] variant fails to compile here until someone decides
 * what it should say, instead of silently inheriting the generic message.
 */
fun AppError.toUiText(): UiText = when (this) {
    is AppError.Validation,
    is AppError.Storage,
    is AppError.Content,
    is AppError.Purchase,
    is AppError.Permission,
    is AppError.Unexpected,
    -> UiText.Resource(Res.string.error_generic_message)
}

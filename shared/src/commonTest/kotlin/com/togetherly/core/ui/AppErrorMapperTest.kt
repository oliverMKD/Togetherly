package com.togetherly.core.ui

import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.error.PermissionError
import com.togetherly.core.error.StorageError
import com.togetherly.core.error.ValidationError
import com.togetherly.domain.purchase.PurchaseError
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.error_generic_message
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Every [AppError] branch maps to the same generic, resource-backed message — see
 * [AppErrorMapper.kt][toUiText]'s own KDoc for why that's correct rather than incomplete: nothing
 * more specific can safely be shown to the user given [AppError]'s own no-leaking-`cause` contract.
 * This test exists so a newly added [AppError] variant that compiles here is proven to still
 * resolve to that same safe default, not left unhandled.
 */
class AppErrorMapperTest {

    private val expected = UiText.Resource(Res.string.error_generic_message)

    @Test
    fun everyAppErrorVariantMapsToTheGenericMessage() {
        assertEquals(expected, AppError.Validation(ValidationError.INVALID_INPUT).toUiText())
        assertEquals(expected, AppError.Storage(StorageError.READ_FAILED).toUiText())
        assertEquals(expected, AppError.Content(ContentError.CATALOGUE_UNAVAILABLE).toUiText())
        assertEquals(expected, AppError.Purchase(PurchaseError.NetworkProblem).toUiText())
        assertEquals(expected, AppError.Permission(PermissionError.NOT_GRANTED).toUiText())
        assertEquals(expected, AppError.Unexpected().toUiText())
    }
}

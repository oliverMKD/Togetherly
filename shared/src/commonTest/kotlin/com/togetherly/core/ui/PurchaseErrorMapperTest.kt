package com.togetherly.core.ui

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.domain.purchase.PurchaseError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PurchaseErrorMapperTest {

    @Test
    fun everyPurchaseErrorMapsToItsOwnDistinctMessage() {
        val messages = PurchaseErrorValues.map { it.toUiText() }.toSet()

        assertEquals(PurchaseErrorValues.size, messages.size)
    }

    @Test
    fun purchaseWrappedAppErrorUsesTheSpecificPurchaseReasonNotTheGenericMessage() {
        val appError = AppError.Purchase(PurchaseError.StoreUnavailable)

        val result = appError.toPurchaseAwareUiText()

        assertEquals(PurchaseError.StoreUnavailable.toUiText(), result)
        assertNotEquals(appError.toUiText(), result)
    }

    @Test
    fun nonPurchaseAppErrorFallsBackToTheGenericMapper() {
        val appError = AppError.Storage(StorageError.READ_FAILED)

        val result = appError.toPurchaseAwareUiText()

        assertEquals(appError.toUiText(), result)
    }

    private companion object {
        val PurchaseErrorValues = listOf(
            PurchaseError.ProductUnavailable,
            PurchaseError.StoreUnavailable,
            PurchaseError.NetworkProblem,
            PurchaseError.ConfigurationProblem,
            PurchaseError.PurchaseNotAllowed,
            PurchaseError.AlreadyOwned,
            PurchaseError.Unknown,
        )
    }
}

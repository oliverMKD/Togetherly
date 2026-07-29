package com.togetherly.domain.purchase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PurchaseResultTest {

    @Test
    fun cancellationIsDistinctFromFailure() {
        val result: PurchaseResult = PurchaseResult.Cancelled

        assertIs<PurchaseResult.Cancelled>(result)
    }

    @Test
    fun pendingPurchaseRetainsProductId() {
        val productId = ProductId("family_plus_monthly")

        val result = PurchaseResult.Pending(productId)

        assertEquals(productId, result.productId)
    }

    @Test
    fun successContainsResultingAccess() {
        val access = FamilyAccess.lifetime()

        val result = PurchaseResult.Success(access)

        assertEquals(access, result.access)
    }
}

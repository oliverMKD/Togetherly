package com.togetherly.domain.purchase

sealed interface PurchaseResult {
    data class Success(
        val access: FamilyAccess,
    ) : PurchaseResult

    data object Cancelled : PurchaseResult

    data class Pending(
        val productId: ProductId,
    ) : PurchaseResult

    data class Failure(
        val error: PurchaseError,
    ) : PurchaseResult
}

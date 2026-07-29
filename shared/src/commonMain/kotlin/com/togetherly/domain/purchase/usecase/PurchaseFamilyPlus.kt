package com.togetherly.domain.purchase.usecase

import com.togetherly.core.result.DataResult
import com.togetherly.domain.purchase.ProductId
import com.togetherly.domain.purchase.PurchaseError
import com.togetherly.domain.purchase.PurchaseResult
import com.togetherly.domain.purchase.repository.EntitlementRepository

/**
 * Verifies [productId] is among the currently known packages before purchasing, "when
 * appropriate": if the package list can't currently be loaded, that failure alone must not
 * block an otherwise-valid purchase attempt, so the check is skipped and the provider is asked
 * to attempt the purchase directly.
 */
class PurchaseFamilyPlus(
    private val entitlementRepository: EntitlementRepository,
) {
    suspend operator fun invoke(productId: ProductId): PurchaseResult {
        val packagesResult = entitlementRepository.getPackages()
        val packages = (packagesResult as? DataResult.Success)?.value
        if (packages != null && packages.none { it.productId == productId }) {
            return PurchaseResult.Failure(PurchaseError.ProductUnavailable)
        }

        return entitlementRepository.purchase(productId)
    }
}

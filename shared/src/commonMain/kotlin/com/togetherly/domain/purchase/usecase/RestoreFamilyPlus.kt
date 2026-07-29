package com.togetherly.domain.purchase.usecase

import com.togetherly.domain.purchase.RestoreResult
import com.togetherly.domain.purchase.repository.EntitlementRepository

/**
 * Intentionally a thin, direct forward to [EntitlementRepository.restorePurchases] — see
 * [com.togetherly.domain.purchase.usecase.RefreshFamilyAccess] for the same reasoning.
 */
class RestoreFamilyPlus(
    private val entitlementRepository: EntitlementRepository,
) {
    suspend operator fun invoke(): RestoreResult = entitlementRepository.restorePurchases()
}

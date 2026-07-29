package com.togetherly.domain.purchase.usecase

import com.togetherly.core.result.DataResult
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.repository.EntitlementRepository

/**
 * Intentionally a thin, direct forward to [EntitlementRepository.refreshAccess] — its value is
 * a stable, discoverable, feature-facing name in the use-case layer that presentation code
 * depends on uniformly, not additional logic. See [com.togetherly.domain.purchase.usecase.PurchaseFamilyPlus]
 * for a use case in this same family that does add real logic.
 */
class RefreshFamilyAccess(
    private val entitlementRepository: EntitlementRepository,
) {
    suspend operator fun invoke(): DataResult<AccessSnapshot> = entitlementRepository.refreshAccess()
}

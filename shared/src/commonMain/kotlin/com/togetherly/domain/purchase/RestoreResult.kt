package com.togetherly.domain.purchase

/**
 * Distinct from [PurchaseResult]: a restore that completes successfully but finds no prior
 * purchase is still [Success], wrapping the (unchanged, likely free) [AccessSnapshot] — it is
 * not a technical failure. Restore also has no [PurchaseResult.Cancelled] or
 * [PurchaseResult.Pending] equivalent; those states don't apply to reconciling existing
 * entitlements. [Success] wraps [AccessSnapshot] rather than [FamilyAccess] to stay consistent
 * with the rest of [com.togetherly.domain.purchase.repository.EntitlementRepository], which
 * reports the full reconciled state, not just the access sub-piece.
 */
sealed interface RestoreResult {

    data class Success(
        val access: AccessSnapshot,
    ) : RestoreResult

    data class Failure(
        val error: PurchaseError,
    ) : RestoreResult
}

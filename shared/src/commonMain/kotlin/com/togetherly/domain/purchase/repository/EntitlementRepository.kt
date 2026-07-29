package com.togetherly.domain.purchase.repository

import com.togetherly.core.result.DataResult
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.ProductId
import com.togetherly.domain.purchase.PurchasePackage
import com.togetherly.domain.purchase.PurchaseResult
import com.togetherly.domain.purchase.RestoreResult
import kotlinx.coroutines.flow.Flow

/**
 * Provider-neutral: nothing here mentions RevenueCat, Google Play Billing, or StoreKit, and this
 * repository never decides which paywall to show or when to show it — that belongs to
 * presentation/use-case code.
 *
 * [observeAccess] and [getAccess] reflect the latest known entitlement snapshot without forcing
 * a network round trip; [refreshAccess] explicitly asks the provider to reconcile current state.
 * [observePackages] and [getPackages] return currently available packages with prices the
 * store/provider has already localized — this repository never formats or calculates a price.
 * Free content and free access remain available even when package loading or [refreshAccess]
 * fails; a provider error here never blocks the free experience.
 *
 * [PurchaseResult.Cancelled] represents a user cancelling — a normal outcome, never surfaced as
 * a [DataResult.Error] and never [PurchaseResult.Failure]. A successful purchase's
 * [PurchaseResult.Success] carries the resulting access. See [RestoreResult] for why restore
 * uses its own result type instead of reusing [PurchaseResult].
 */
interface EntitlementRepository {

    fun observeAccess(): Flow<DataResult<AccessSnapshot>>

    suspend fun getAccess(): DataResult<AccessSnapshot>

    suspend fun refreshAccess(): DataResult<AccessSnapshot>

    fun observePackages(): Flow<DataResult<List<PurchasePackage>>>

    suspend fun getPackages(): DataResult<List<PurchasePackage>>

    suspend fun purchase(
        productId: ProductId,
    ): PurchaseResult

    suspend fun restorePurchases(): RestoreResult

    /** Whether the purchase provider is configured and ready — gates whether a Customer Center entry point should even be shown (see [com.togetherly.domain.purchase.PurchaseStartupState.Ready]). Provider-neutral: never exposes *why* if not ready. */
    fun isReady(): Boolean

    /**
     * Clears only the locally-cached offline entitlement snapshot and resets in-memory state back
     * to free — never RevenueCat's (or any provider's) own server-side purchase/subscription
     * record, and never a subscription cancellation or logout. A still-genuinely-entitled family
     * is expected to see premium access reappear shortly after, once the provider's own
     * reconciliation runs again (this repository's implementation is expected to trigger that
     * itself). Exists for a full local-data wipe ([com.togetherly.domain.localdata.usecase.DeleteAllLocalData]) —
     * never a general-purpose "log out," since this app has none (every family is identified only
     * by the provider's own anonymous, on-device identity; see [com.togetherly.data.purchase.RevenueCatConfigurator]'s
     * own KDoc).
     */
    suspend fun clearCache(): DataResult<Unit>
}

package com.togetherly.data.purchase

import com.togetherly.core.result.DataResult
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.ProductId
import com.togetherly.domain.purchase.PurchasePackage
import com.togetherly.domain.purchase.PurchaseResult
import com.togetherly.domain.purchase.RestoreResult
import kotlinx.coroutines.flow.Flow

/**
 * The one seam RevenueCat SDK types (`CustomerInfo`, `Offering`, `Package`, `StoreProduct`,
 * RevenueCat exceptions) are allowed to exist behind — [RevenueCatEntitlementRepository] and
 * everything above it only ever sees the provider-neutral `domain.purchase` models this interface
 * already returns. A fake implementation of this interface (see `FakeRevenueCatDataSource`,
 * commonTest) is what lets [RevenueCatEntitlementRepository]'s own tests avoid touching any real
 * RevenueCat SDK/global state at all.
 */
interface RevenueCatDataSource {

    /** Pushes a fresh [AccessSnapshot] whenever RevenueCat's own customer info changes — a purchase, a restore, a renewal, a refund, all reconciled by the SDK itself. */
    fun observeCustomerAccess(): Flow<AccessSnapshot>

    suspend fun getCustomerAccess(): DataResult<AccessSnapshot>

    suspend fun getOfferingPackages(): DataResult<List<PurchasePackage>>

    suspend fun purchase(productId: ProductId): PurchaseResult

    suspend fun restorePurchases(): RestoreResult

    /** Whether the provider is configured and ready to be asked anything — see [com.togetherly.domain.purchase.PurchaseStartupState.Ready]. Gates whether a Customer Center entry point should even be shown. */
    fun isReady(): Boolean

    /**
     * Sets or clears (`null`) RevenueCat's own `$posthogUserId` reserved subscriber attribute — the
     * one-directional link RevenueCat's official PostHog integration reads to associate revenue
     * events with a PostHog distinct id. Never RevenueCat's own App User ID, never a `logIn()` call
     * — see [RevenueCatAnalyticsLinker]'s own KDoc for the full consent-gated call sequence this
     * backs.
     */
    fun setPostHogDistinctId(distinctId: String?)

    /**
     * Sets or clears (`null` per value) a small set of low-risk, predefined RevenueCat customer
     * attributes used for paywall targeting only — never a family profile id, memory content, or
     * anything else this app's own `docs/analytics-event-taxonomy.md` forbids. See
     * `docs/revenuecat-posthog-integration.md` for the reviewed, minimal attribute list.
     */
    fun setCustomerAttributes(attributes: Map<String, String?>)
}

package com.togetherly.data.purchase

import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.datetime.AppClock
import com.togetherly.core.result.DataResult
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.ProductId
import com.togetherly.domain.purchase.PurchasePackage
import com.togetherly.domain.purchase.PurchaseResult
import com.togetherly.domain.purchase.RestoreResult
import com.togetherly.domain.purchase.repository.EntitlementRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The production [EntitlementRepository] — the one place [RevenueCatDataSource] and
 * [EntitlementCache] meet. Nothing above this class knows RevenueCat exists.
 *
 * [_access] is seeded free (never optimistically premium — "unknown access must not unlock
 * premium permanently") and immediately, asynchronously reconciled in [init]: first from
 * [EntitlementCache] (fast, offline-safe, no network), then from a background [refreshAccess]
 * (best-effort; its own failure never resets an already-loaded cached/premium state — see that
 * function's own KDoc). A second, permanent collector in [init] applies every push update from
 * [RevenueCatDataSource.observeCustomerAccess] (a purchase, a renewal, a refund reconciled by
 * RevenueCat itself) directly onto [_access] and the cache, for as long as this singleton lives.
 *
 * [purchase]/[restorePurchases] both update [_access] (and the cache) immediately on their own
 * success — a purchase is confirmed to actually activate `family_plus` by [RevenueCatDataSource]
 * itself before it ever reports [PurchaseResult.Success] here, so "a purchase button was tapped"
 * is never by itself enough to grant access; only a provider-confirmed [FamilyAccess.isPlus] is.
 *
 * [purchase] coalesces concurrent callers onto one shared in-flight [Deferred] ([purchaseMutex]
 * guards [inFlightPurchase]) rather than starting a second real store purchase — a repeated tap or
 * a recomposition-triggered re-dispatch while one is already running joins the same attempt
 * instead of double-charging. The underlying store call runs on [scope] (this singleton's own
 * long-lived scope), not the caller's — a purchase already sent to the store must run to
 * completion and be recorded even if the caller (e.g. a dismissed paywall's `viewModelScope`) is
 * itself cancelled.
 */
internal class RevenueCatEntitlementRepository(
    private val dataSource: RevenueCatDataSource,
    private val cache: EntitlementCache,
    private val clock: AppClock,
    dispatchers: AppDispatchers,
) : EntitlementRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    private val _access = MutableStateFlow<DataResult<AccessSnapshot>>(DataResult.Success(freshFreeSnapshot()))
    private val _packages = MutableStateFlow<DataResult<List<PurchasePackage>>>(DataResult.Success(emptyList()))

    private val purchaseMutex = Mutex()
    private var inFlightPurchase: Deferred<PurchaseResult>? = null

    init {
        scope.launch {
            cache.load()?.let { cached -> _access.value = DataResult.Success(cached) }
            refreshAccess()
        }
        scope.launch {
            dataSource.observeCustomerAccess().collect { snapshot ->
                _access.value = DataResult.Success(snapshot)
                cache.save(snapshot)
            }
        }
    }

    override fun observeAccess(): Flow<DataResult<AccessSnapshot>> = _access.asStateFlow()

    override suspend fun getAccess(): DataResult<AccessSnapshot> = _access.value

    /**
     * A failure here never overwrites [_access] — whatever was already loaded (cached premium,
     * cached free, or the initial free seed) stays exactly as it was, so one transient network
     * failure can never turn a known premium family free. The failure is still reported to this
     * call's own caller, who may want to show a retry affordance.
     */
    override suspend fun refreshAccess(): DataResult<AccessSnapshot> {
        val result = dataSource.getCustomerAccess()
        if (result is DataResult.Success) {
            _access.value = result
            cache.save(result.value)
        }
        return result
    }

    override fun observePackages(): Flow<DataResult<List<PurchasePackage>>> = _packages.asStateFlow()

    override suspend fun getPackages(): DataResult<List<PurchasePackage>> {
        val result = dataSource.getOfferingPackages()
        if (result is DataResult.Success) {
            _packages.value = result
        }
        return result
    }

    override suspend fun purchase(productId: ProductId): PurchaseResult {
        val deferred = purchaseMutex.withLock {
            inFlightPurchase ?: scope.async {
                try {
                    dataSource.purchase(productId)
                } finally {
                    purchaseMutex.withLock { inFlightPurchase = null }
                }
            }.also { inFlightPurchase = it }
        }

        val result = deferred.await()
        if (result is PurchaseResult.Success) {
            applyConfirmedAccess(result.access)
        }
        return result
    }

    override fun isReady(): Boolean = dataSource.isReady()

    /**
     * Resets [_access] to a fresh free snapshot immediately (so nothing still observing this
     * singleton keeps showing stale premium access from memory alone) and clears the persisted
     * [EntitlementCache] row, then fires [refreshAccess] in the background on [scope] — a
     * genuinely entitled family sees premium access reappear as soon as the provider reconciles,
     * without this call (and whatever deletion flow triggered it) blocking on a network round
     * trip. [refreshAccess]'s own failure is ignored here, same as every other "free-mode, never
     * alarming" provider failure in this class.
     */
    override suspend fun clearCache(): DataResult<Unit> {
        cache.clear()
        _access.value = DataResult.Success(freshFreeSnapshot())
        scope.launch { refreshAccess() }
        return DataResult.Success(Unit)
    }

    override suspend fun restorePurchases(): RestoreResult {
        val result = dataSource.restorePurchases()
        if (result is RestoreResult.Success) {
            _access.value = DataResult.Success(result.access)
            cache.save(result.access)
        }
        return result
    }

    private suspend fun applyConfirmedAccess(access: FamilyAccess) {
        val entitlements = if (access.isPlus) setOf(EntitlementId(FAMILY_PLUS_ENTITLEMENT_ID)) else emptySet()
        val snapshot = AccessSnapshot(familyAccess = access, activeEntitlements = entitlements, verifiedAt = clock.now())
        _access.value = DataResult.Success(snapshot)
        cache.save(snapshot)
    }

    private fun freshFreeSnapshot() = AccessSnapshot(
        familyAccess = FamilyAccess.free(),
        activeEntitlements = emptySet(),
        verifiedAt = clock.now(),
    )
}

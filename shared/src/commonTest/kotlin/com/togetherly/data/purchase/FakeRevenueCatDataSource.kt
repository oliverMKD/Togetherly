package com.togetherly.data.purchase

import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.ProductId
import com.togetherly.domain.purchase.PurchasePackage
import com.togetherly.domain.purchase.PurchaseResult
import com.togetherly.domain.purchase.RestoreResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * The one fake standing in for [RevenueCatDataSource] in tests — never a mock of RevenueCat's own
 * static/global SDK state (see this feature's own task spec: no `spyk()`, no mocking RevenueCat
 * directly). [RevenueCatEntitlementRepository]'s own tests are the primary consumer of this class.
 */
class FakeRevenueCatDataSource(
    initialAccess: AccessSnapshot,
) : RevenueCatDataSource {

    private val updatesFlow = MutableSharedFlow<AccessSnapshot>(replay = 1)

    private var ready = true
    private var customerAccessResult: DataResult<AccessSnapshot> = DataResult.Success(initialAccess)
    private var packagesResult: DataResult<List<PurchasePackage>> = DataResult.Success(emptyList())
    private val purchaseResultsByProduct = mutableMapOf<ProductId, PurchaseResult>()
    private var restoreResult: RestoreResult = RestoreResult.Success(initialAccess)
    private var purchaseGate: CompletableDeferred<Unit>? = null

    val purchaseCalls: MutableList<ProductId> = mutableListOf()
    var restoreCallCount: Int = 0
        private set
    var getCustomerAccessCallCount: Int = 0
        private set
    val postHogDistinctIdCalls: MutableList<String?> = mutableListOf()
    val customerAttributeCalls: MutableList<Map<String, String?>> = mutableListOf()

    fun setReady(value: Boolean) {
        ready = value
    }

    fun setCustomerAccessResult(result: DataResult<AccessSnapshot>) {
        customerAccessResult = result
    }

    fun setPackagesResult(result: DataResult<List<PurchasePackage>>) {
        packagesResult = result
    }

    fun setPackagesError(error: AppError) {
        packagesResult = DataResult.Error(error)
    }

    fun setPurchaseResult(productId: ProductId, result: PurchaseResult) {
        purchaseResultsByProduct[productId] = result
    }

    fun setRestoreResult(result: RestoreResult) {
        restoreResult = result
    }

    /** Suspends the next [purchase] call until the returned gate is completed — used to test duplicate-purchase prevention. */
    fun holdNextPurchaseUntilReleased(): CompletableDeferred<Unit> = CompletableDeferred<Unit>().also { purchaseGate = it }

    /** Simulates [com.revenuecat.purchases.kmp.PurchasesDelegate.onCustomerInfoUpdated] pushing a fresh snapshot. */
    suspend fun emitCustomerAccessUpdate(snapshot: AccessSnapshot) {
        updatesFlow.emit(snapshot)
    }

    override fun isReady(): Boolean = ready

    override fun observeCustomerAccess(): Flow<AccessSnapshot> = updatesFlow

    override suspend fun getCustomerAccess(): DataResult<AccessSnapshot> {
        getCustomerAccessCallCount++
        return customerAccessResult
    }

    override suspend fun getOfferingPackages(): DataResult<List<PurchasePackage>> = packagesResult

    override suspend fun purchase(productId: ProductId): PurchaseResult {
        purchaseCalls += productId
        purchaseGate?.await()
        return purchaseResultsByProduct[productId] ?: PurchaseResult.Cancelled
    }

    override suspend fun restorePurchases(): RestoreResult {
        restoreCallCount++
        return restoreResult
    }

    override fun setPostHogDistinctId(distinctId: String?) {
        postHogDistinctIdCalls += distinctId
    }

    override fun setCustomerAttributes(attributes: Map<String, String?>) {
        customerAttributeCalls += attributes
    }
}

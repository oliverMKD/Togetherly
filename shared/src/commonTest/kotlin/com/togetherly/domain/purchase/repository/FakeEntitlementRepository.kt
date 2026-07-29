package com.togetherly.domain.purchase.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.ProductId
import com.togetherly.domain.purchase.PurchasePackage
import com.togetherly.domain.purchase.PurchaseResult
import com.togetherly.domain.purchase.RestoreResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeEntitlementRepository(
    initialAccess: AccessSnapshot,
) : EntitlementRepository {

    private val accessFlow = MutableStateFlow<DataResult<AccessSnapshot>>(DataResult.Success(initialAccess))
    private val packagesFlow = MutableStateFlow<DataResult<List<PurchasePackage>>>(DataResult.Success(emptyList()))

    private var refreshResult: DataResult<AccessSnapshot>? = null
    private val purchaseResultsByProduct: MutableMap<ProductId, PurchaseResult> = mutableMapOf()
    private var restoreResult: RestoreResult = RestoreResult.Success(initialAccess)
    private var ready: Boolean = true

    val requestedProductIds: MutableList<ProductId> = mutableListOf()
    var getAccessCallCount: Int = 0
        private set
    var refreshAccessCallCount: Int = 0
        private set
    var restorePurchasesCallCount: Int = 0
        private set
    var clearCacheCallCount: Int = 0
        private set

    fun setAccess(access: AccessSnapshot) {
        accessFlow.value = DataResult.Success(access)
    }

    fun setPackages(packages: List<PurchasePackage>) {
        packagesFlow.value = DataResult.Success(packages)
    }

    fun setPackagesError(error: AppError) {
        packagesFlow.value = DataResult.Error(error)
    }

    fun setRefreshResult(result: DataResult<AccessSnapshot>) {
        refreshResult = result
    }

    fun setPurchaseResult(productId: ProductId, result: PurchaseResult) {
        purchaseResultsByProduct[productId] = result
    }

    fun setRestoreResult(result: RestoreResult) {
        restoreResult = result
    }

    fun setReady(value: Boolean) {
        ready = value
    }

    override fun observeAccess(): Flow<DataResult<AccessSnapshot>> = accessFlow

    override suspend fun getAccess(): DataResult<AccessSnapshot> {
        getAccessCallCount++
        return accessFlow.value
    }

    override suspend fun refreshAccess(): DataResult<AccessSnapshot> {
        refreshAccessCallCount++
        val result = refreshResult ?: accessFlow.value
        if (result is DataResult.Success) {
            accessFlow.value = result
        }
        return result
    }

    override fun observePackages(): Flow<DataResult<List<PurchasePackage>>> = packagesFlow

    override suspend fun getPackages(): DataResult<List<PurchasePackage>> = packagesFlow.value

    override suspend fun purchase(productId: ProductId): PurchaseResult {
        requestedProductIds += productId
        val result = purchaseResultsByProduct[productId] ?: PurchaseResult.Cancelled
        if (result is PurchaseResult.Success) {
            val current = (accessFlow.value as DataResult.Success).value
            accessFlow.value = DataResult.Success(current.copy(familyAccess = result.access))
        }
        return result
    }

    override fun isReady(): Boolean = ready

    override suspend fun restorePurchases(): RestoreResult {
        restorePurchasesCallCount++
        val result = restoreResult
        if (result is RestoreResult.Success) {
            accessFlow.value = DataResult.Success(result.access)
        }
        return result
    }

    override suspend fun clearCache(): DataResult<Unit> {
        clearCacheCallCount++
        val current = (accessFlow.value as? DataResult.Success)?.value ?: return DataResult.Success(Unit)
        accessFlow.value = DataResult.Success(current.copy(familyAccess = FamilyAccess.free()))
        return DataResult.Success(Unit)
    }
}

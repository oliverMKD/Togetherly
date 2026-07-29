package com.togetherly.domain.purchase.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.BillingPeriod
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.ProductId
import com.togetherly.domain.purchase.PurchaseError
import com.togetherly.domain.purchase.PurchasePackage
import com.togetherly.domain.purchase.PurchasePackageType
import com.togetherly.domain.purchase.PurchaseResult
import com.togetherly.domain.purchase.RestoreResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

private val VERIFIED_AT = Instant.parse("2026-06-15T08:00:00Z")
private val FREE_SNAPSHOT = AccessSnapshot(
    familyAccess = FamilyAccess.free(),
    activeEntitlements = emptySet(),
    verifiedAt = VERIFIED_AT,
)

private fun purchasePackage(
    productId: String,
    type: PurchasePackageType,
    billingPeriod: BillingPeriod?,
) = PurchasePackage(
    productId = ProductId(productId),
    type = type,
    title = "Title",
    formattedPrice = "$1.99",
    billingPeriod = billingPeriod,
    offeringIdentifier = "default",
)

class FakeEntitlementRepositoryTest {

    @Test
    fun initialFreeAccess() = runTest {
        val repository = FakeEntitlementRepository(initialAccess = FREE_SNAPSHOT)

        assertEquals(DataResult.Success(FREE_SNAPSHOT), repository.getAccess())
    }

    @Test
    fun accessObserversReceiveUpdates() = runTest {
        val repository = FakeEntitlementRepository(initialAccess = FREE_SNAPSHOT)
        val lifetimeSnapshot = FREE_SNAPSHOT.copy(familyAccess = FamilyAccess.lifetime())

        repository.setAccess(lifetimeSnapshot)

        assertEquals(DataResult.Success(lifetimeSnapshot), repository.observeAccess().first())
    }

    @Test
    fun refreshReturnsConfiguredState() = runTest {
        val repository = FakeEntitlementRepository(initialAccess = FREE_SNAPSHOT)
        val refreshed = FREE_SNAPSHOT.copy(familyAccess = FamilyAccess.lifetime())
        repository.setRefreshResult(DataResult.Success(refreshed))

        val result = repository.refreshAccess()

        assertEquals(DataResult.Success(refreshed), result)
        assertEquals(DataResult.Success(refreshed), repository.getAccess())
        assertEquals(1, repository.refreshAccessCallCount)
    }

    @Test
    fun packageListPreservesProviderOrder() = runTest {
        val repository = FakeEntitlementRepository(initialAccess = FREE_SNAPSHOT)
        val packages = listOf(
            purchasePackage("family_plus_annual", PurchasePackageType.ANNUAL, BillingPeriod.YEAR),
            purchasePackage("family_plus_monthly", PurchasePackageType.MONTHLY, BillingPeriod.MONTH),
        )

        repository.setPackages(packages)

        assertEquals(DataResult.Success(packages), repository.getPackages())
        assertEquals(DataResult.Success(packages), repository.observePackages().first())
    }

    @Test
    fun packageLoadingErrorIsPreserved() = runTest {
        val repository = FakeEntitlementRepository(initialAccess = FREE_SNAPSHOT)
        val error = AppError.Storage(StorageError.READ_FAILED)
        repository.setPackagesError(error)

        assertEquals(DataResult.Error(error), repository.getPackages())
    }

    @Test
    fun purchaseRecordsProductId() = runTest {
        val repository = FakeEntitlementRepository(initialAccess = FREE_SNAPSHOT)
        val productId = ProductId("family_plus_monthly")

        repository.purchase(productId)

        assertEquals(listOf(productId), repository.requestedProductIds)
    }

    @Test
    fun successfulPurchaseUpdatesAccessWhenConfigured() = runTest {
        val repository = FakeEntitlementRepository(initialAccess = FREE_SNAPSHOT)
        val productId = ProductId("family_plus_lifetime")
        repository.setPurchaseResult(productId, PurchaseResult.Success(FamilyAccess.lifetime()))

        repository.purchase(productId)

        val updated = (repository.getAccess() as DataResult.Success).value
        assertEquals(FamilyAccess.lifetime(), updated.familyAccess)
    }

    @Test
    fun cancellationRemainsDistinctFromFailure() = runTest {
        val repository = FakeEntitlementRepository(initialAccess = FREE_SNAPSHOT)
        val productId = ProductId("family_plus_monthly")
        repository.setPurchaseResult(productId, PurchaseResult.Cancelled)

        val result = repository.purchase(productId)

        assertIs<PurchaseResult.Cancelled>(result)
    }

    @Test
    fun pendingPurchaseRemainsDistinctFromFailure() = runTest {
        val repository = FakeEntitlementRepository(initialAccess = FREE_SNAPSHOT)
        val productId = ProductId("family_plus_monthly")
        repository.setPurchaseResult(productId, PurchaseResult.Pending(productId))

        val result = repository.purchase(productId)

        assertEquals(PurchaseResult.Pending(productId), result)
    }

    @Test
    fun restoreWithNoEntitlementIsASuccessfulReconciliation() = runTest {
        val repository = FakeEntitlementRepository(initialAccess = FREE_SNAPSHOT)
        repository.setRestoreResult(RestoreResult.Success(FREE_SNAPSHOT))

        val result = repository.restorePurchases()

        assertEquals(RestoreResult.Success(FREE_SNAPSHOT), result)
    }

    @Test
    fun providerConfigurationErrorIsTyped() = runTest {
        val repository = FakeEntitlementRepository(initialAccess = FREE_SNAPSHOT)
        val productId = ProductId("family_plus_monthly")
        repository.setPurchaseResult(productId, PurchaseResult.Failure(PurchaseError.ConfigurationProblem))

        val result = repository.purchase(productId)

        assertEquals(PurchaseResult.Failure(PurchaseError.ConfigurationProblem), result)
    }
}

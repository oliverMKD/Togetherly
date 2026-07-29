package com.togetherly.domain.purchase.usecase

import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.BillingPeriod
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.ProductId
import com.togetherly.domain.purchase.PurchaseError
import com.togetherly.domain.purchase.PurchasePackage
import com.togetherly.domain.purchase.PurchasePackageType
import com.togetherly.domain.purchase.PurchaseResult
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val VERIFIED_AT = Instant.parse("2026-06-15T08:00:00Z")
private val FREE_SNAPSHOT = AccessSnapshot(
    familyAccess = FamilyAccess.free(),
    activeEntitlements = emptySet(),
    verifiedAt = VERIFIED_AT,
)
private val MONTHLY = ProductId("family_plus_monthly")

private fun monthlyPackage() = PurchasePackage(
    productId = MONTHLY,
    type = PurchasePackageType.MONTHLY,
    title = "Monthly",
    formattedPrice = "$4.99",
    billingPeriod = BillingPeriod.MONTH,
    offeringIdentifier = "default",
)

class PurchaseFamilyPlusTest {

    @Test
    fun successfulPurchaseIsPreserved() = runTest {
        val repository = FakeEntitlementRepository(initialAccess = FREE_SNAPSHOT)
        repository.setPackages(listOf(monthlyPackage()))
        repository.setPurchaseResult(MONTHLY, PurchaseResult.Success(FamilyAccess.lifetime()))
        val useCase = PurchaseFamilyPlus(repository)

        val result = useCase(MONTHLY)

        assertEquals(PurchaseResult.Success(FamilyAccess.lifetime()), result)
    }

    @Test
    fun cancellationIsPreserved() = runTest {
        val repository = FakeEntitlementRepository(initialAccess = FREE_SNAPSHOT)
        repository.setPackages(listOf(monthlyPackage()))
        repository.setPurchaseResult(MONTHLY, PurchaseResult.Cancelled)
        val useCase = PurchaseFamilyPlus(repository)

        val result = useCase(MONTHLY)

        assertEquals(PurchaseResult.Cancelled, result)
    }

    @Test
    fun pendingStateIsPreserved() = runTest {
        val repository = FakeEntitlementRepository(initialAccess = FREE_SNAPSHOT)
        repository.setPackages(listOf(monthlyPackage()))
        repository.setPurchaseResult(MONTHLY, PurchaseResult.Pending(MONTHLY))
        val useCase = PurchaseFamilyPlus(repository)

        val result = useCase(MONTHLY)

        assertEquals(PurchaseResult.Pending(MONTHLY), result)
    }

    @Test
    fun missingProductIsRejectedAccordingToTheDocumentedPolicy() = runTest {
        val repository = FakeEntitlementRepository(initialAccess = FREE_SNAPSHOT)
        repository.setPackages(listOf(monthlyPackage()))
        val useCase = PurchaseFamilyPlus(repository)

        val result = useCase(ProductId("unknown_product"))

        assertEquals(PurchaseResult.Failure(PurchaseError.ProductUnavailable), result)
        assertEquals(emptyList(), repository.requestedProductIds)
    }
}

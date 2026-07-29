package com.togetherly.data.purchase

import com.revenuecat.purchases.kmp.models.PackageType
import com.revenuecat.purchases.kmp.models.PurchasesErrorCode
import com.togetherly.domain.purchase.AccessSource
import com.togetherly.domain.purchase.BillingPeriod
import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.purchase.PurchaseError
import com.togetherly.domain.purchase.PurchasePackageType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Exercises the pure mapping logic ([RevenueCatCustomerSnapshot]/[RevenueCatPackageSnapshot] and
 * everything downstream of them) with plain Kotlin fixtures — never a real RevenueCat SDK object
 * (`CustomerInfo`, `Package`, `Offering`...), which this feature's own task spec asks tests to
 * avoid constructing directly. See [RevenueCatEntitlementSnapshot]'s own KDoc for why this
 * boundary exists.
 */
class RevenueCatMappersTest {

    private val verifiedAt = Instant.parse("2026-06-15T09:00:00Z")

    @Test
    fun freeCustomerHasNoActiveEntitlements() {
        val snapshot = RevenueCatCustomerSnapshot(activeEntitlementIds = emptySet(), familyPlusEntitlement = null)

        val access = snapshot.toFamilyAccess()

        assertEquals(AccessSource.FREE, access.source)
        assertFalse(access.isPlus)
    }

    @Test
    fun missingFamilyPlusEntitlementIsTreatedAsFreeEvenWithOtherEntitlements() {
        val snapshot = RevenueCatCustomerSnapshot(
            activeEntitlementIds = setOf("some_other_entitlement"),
            familyPlusEntitlement = null,
        )

        assertEquals(AccessSource.FREE, snapshot.toFamilyAccess().source)
    }

    @Test
    fun activeMonthlyEntitlementMapsToSubscription() {
        val expiry = Instant.parse("2026-07-15T09:00:00Z")
        val snapshot = RevenueCatCustomerSnapshot(
            activeEntitlementIds = setOf(FAMILY_PLUS_ENTITLEMENT_ID),
            familyPlusEntitlement = RevenueCatEntitlementSnapshot(isActive = true, expirationDate = expiry, willRenew = true),
        )

        val access = snapshot.toFamilyAccess()

        assertEquals(AccessSource.SUBSCRIPTION, access.source)
        assertTrue(access.isPlus)
        assertEquals(expiry, access.expiresAt)
        assertEquals(true, access.willRenew)
    }

    @Test
    fun activeAnnualEntitlementMapsToSubscriptionWithItsOwnRenewalFlag() {
        val expiry = Instant.parse("2027-06-15T09:00:00Z")
        val snapshot = RevenueCatCustomerSnapshot(
            activeEntitlementIds = setOf(FAMILY_PLUS_ENTITLEMENT_ID),
            familyPlusEntitlement = RevenueCatEntitlementSnapshot(isActive = true, expirationDate = expiry, willRenew = false),
        )

        val access = snapshot.toFamilyAccess()

        assertEquals(AccessSource.SUBSCRIPTION, access.source)
        assertEquals(false, access.willRenew)
    }

    @Test
    fun lifetimeEntitlementHasNoExpirationAndIsNeverTreatedAsExpiringSubscription() {
        val snapshot = RevenueCatCustomerSnapshot(
            activeEntitlementIds = setOf(FAMILY_PLUS_ENTITLEMENT_ID),
            familyPlusEntitlement = RevenueCatEntitlementSnapshot(isActive = true, expirationDate = null, willRenew = false),
        )

        val access = snapshot.toFamilyAccess()

        assertEquals(AccessSource.LIFETIME, access.source)
        assertTrue(access.isPlus)
        assertNull(access.expiresAt)
        assertNull(access.willRenew)
    }

    @Test
    fun accessSnapshotCarriesActiveEntitlementIdsAndVerifiedAt() {
        val snapshot = RevenueCatCustomerSnapshot(
            activeEntitlementIds = setOf(FAMILY_PLUS_ENTITLEMENT_ID, "another_entitlement"),
            familyPlusEntitlement = RevenueCatEntitlementSnapshot(isActive = true, expirationDate = null, willRenew = false),
        )

        val result = snapshot.toAccessSnapshot(verifiedAt)

        assertEquals(
            setOf(EntitlementId(FAMILY_PLUS_ENTITLEMENT_ID), EntitlementId("another_entitlement")),
            result.activeEntitlements,
        )
        assertEquals(verifiedAt, result.verifiedAt)
        assertEquals(AccessSource.LIFETIME, result.familyAccess.source)
    }

    @Test
    fun malformedEntitlementIdentifierIsSkippedRatherThanCrashing() {
        // A blank identifier would fail EntitlementId's own validation — the mapper must not
        // propagate that as a crash, only silently exclude it from the active set.
        val snapshot = RevenueCatCustomerSnapshot(
            activeEntitlementIds = setOf(FAMILY_PLUS_ENTITLEMENT_ID, "   "),
            familyPlusEntitlement = RevenueCatEntitlementSnapshot(isActive = true, expirationDate = null, willRenew = false),
        )

        val result = snapshot.toAccessSnapshot(verifiedAt)

        assertEquals(setOf(EntitlementId(FAMILY_PLUS_ENTITLEMENT_ID)), result.activeEntitlements)
    }

    @Test
    fun monthlyPackageMapsToMonthlyTypeAndBillingPeriod() {
        val snapshot = RevenueCatPackageSnapshot(
            productId = "togetherly_monthly",
            packageType = PackageType.MONTHLY,
            title = "Monthly",
            formattedPrice = "$4.99",
            offeringIdentifier = "default",
        )

        val purchasePackage = snapshot.toPurchasePackageOrNull()

        assertEquals(PurchasePackageType.MONTHLY, purchasePackage?.type)
        assertEquals(BillingPeriod.MONTH, purchasePackage?.billingPeriod)
        assertEquals("$4.99", purchasePackage?.formattedPrice)
    }

    @Test
    fun annualPackageMapsToAnnualTypeAndBillingPeriod() {
        val snapshot = RevenueCatPackageSnapshot(
            productId = "togetherly_annual",
            packageType = PackageType.ANNUAL,
            title = "Annual",
            formattedPrice = "$39.99",
            offeringIdentifier = "default",
        )

        val purchasePackage = snapshot.toPurchasePackageOrNull()

        assertEquals(PurchasePackageType.ANNUAL, purchasePackage?.type)
        assertEquals(BillingPeriod.YEAR, purchasePackage?.billingPeriod)
    }

    @Test
    fun lifetimePackageHasNoBillingPeriod() {
        val snapshot = RevenueCatPackageSnapshot(
            productId = "togetherly_lifetime",
            packageType = PackageType.LIFETIME,
            title = "Lifetime",
            formattedPrice = "$99.99",
            offeringIdentifier = "default",
        )

        val purchasePackage = snapshot.toPurchasePackageOrNull()

        assertEquals(PurchasePackageType.LIFETIME, purchasePackage?.type)
        assertNull(purchasePackage?.billingPeriod)
    }

    @Test
    fun unsupportedPackageTypeMapsToNullRatherThanFailing() {
        val snapshot = RevenueCatPackageSnapshot(
            productId = "togetherly_weekly",
            packageType = PackageType.WEEKLY,
            title = "Weekly",
            formattedPrice = "$1.99",
            offeringIdentifier = "default",
        )

        assertNull(snapshot.toPurchasePackageOrNull())
    }

    @Test
    fun malformedProductIdentifierMapsToNullRatherThanCrashing() {
        // A blank product identifier would fail ProductId's own validation.
        val snapshot = RevenueCatPackageSnapshot(
            productId = "   ",
            packageType = PackageType.MONTHLY,
            title = "Monthly",
            formattedPrice = "$4.99",
            offeringIdentifier = "default",
        )

        assertNull(snapshot.toPurchasePackageOrNull())
    }

    @Test
    fun blankTitleMapsToNullRatherThanCrashing() {
        // PurchasePackage itself rejects a blank title/price — incomplete store data must degrade
        // to "this one package can't be shown," never a crash for the whole offering.
        val snapshot = RevenueCatPackageSnapshot(
            productId = "togetherly_monthly",
            packageType = PackageType.MONTHLY,
            title = "   ",
            formattedPrice = "$4.99",
            offeringIdentifier = "default",
        )

        assertNull(snapshot.toPurchasePackageOrNull())
    }

    @Test
    fun purchaseErrorCodeMapping() {
        assertEquals(PurchaseError.ProductUnavailable, PurchasesErrorCode.ProductNotAvailableForPurchaseError.toPurchaseError())
        assertEquals(PurchaseError.StoreUnavailable, PurchasesErrorCode.StoreProblemError.toPurchaseError())
        assertEquals(PurchaseError.NetworkProblem, PurchasesErrorCode.NetworkError.toPurchaseError())
        assertEquals(PurchaseError.ConfigurationProblem, PurchasesErrorCode.ConfigurationError.toPurchaseError())
        assertEquals(PurchaseError.ConfigurationProblem, PurchasesErrorCode.InvalidCredentialsError.toPurchaseError())
        assertEquals(PurchaseError.PurchaseNotAllowed, PurchasesErrorCode.PurchaseNotAllowedError.toPurchaseError())
        assertEquals(PurchaseError.AlreadyOwned, PurchasesErrorCode.ProductAlreadyPurchasedError.toPurchaseError())
        assertEquals(PurchaseError.Unknown, PurchasesErrorCode.UnknownBackendError.toPurchaseError())
    }
}

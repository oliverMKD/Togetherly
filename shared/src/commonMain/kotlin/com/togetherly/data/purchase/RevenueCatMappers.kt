package com.togetherly.data.purchase

import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Offering
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PackageType
import com.revenuecat.purchases.kmp.models.PurchasesErrorCode
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.BillingPeriod
import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.ProductId
import com.togetherly.domain.purchase.PurchaseError
import com.togetherly.domain.purchase.PurchasePackage
import com.togetherly.domain.purchase.PurchasePackageType
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** The one entitlement identifier this app grants premium access for — see docs/revenuecat-setup.md. */
internal const val FAMILY_PLUS_ENTITLEMENT_ID = "family_plus"

/** The RevenueCat dashboard offering identifier this app expects by convention — never looked up directly; see [toPurchasePackages]. */
internal const val DEFAULT_OFFERING_ID = "default"

/** RevenueCat's own reserved attribute key its official PostHog integration reads — see [RevenueCatAnalyticsLinker]. Never a plain custom key; this exact `$`-prefixed name is documented by RevenueCat itself. */
internal const val POSTHOG_USER_ID_ATTRIBUTE_KEY = "\$posthogUserId"

/**
 * A narrow, plain-Kotlin stand-in for the one entitlement this app cares about, extracted from
 * RevenueCat's own `EntitlementInfo` at the outermost boundary ([CustomerInfo.toCustomerSnapshot]).
 * Everything below this point — [toFamilyAccess], [toAccessSnapshot] — is pure logic over plain
 * types only, fully constructible in `commonTest` with no real SDK object involved, per this
 * feature's own task spec ("introduce narrow internal adapter models at the data-source boundary"
 * when direct SDK types are impractical to construct in tests).
 */
internal data class RevenueCatEntitlementSnapshot(
    val isActive: Boolean,
    val expirationDate: Instant?,
    val willRenew: Boolean,
)

internal data class RevenueCatCustomerSnapshot(
    val activeEntitlementIds: Set<String>,
    val familyPlusEntitlement: RevenueCatEntitlementSnapshot?,
)

@OptIn(ExperimentalTime::class)
internal fun CustomerInfo.toCustomerSnapshot(): RevenueCatCustomerSnapshot {
    val active = entitlements.active
    val familyPlus = active[FAMILY_PLUS_ENTITLEMENT_ID]?.let { entitlement ->
        RevenueCatEntitlementSnapshot(
            isActive = entitlement.isActive,
            expirationDate = entitlement.expirationDate,
            willRenew = entitlement.willRenew,
        )
    }
    return RevenueCatCustomerSnapshot(activeEntitlementIds = active.keys, familyPlusEntitlement = familyPlus)
}

/**
 * `family_plus` is active only when the snapshot actually carries an entitlement — never inferred
 * from which specific product/package was purchased (see this feature's own task spec). A
 * lifetime purchase is recognized by [RevenueCatEntitlementSnapshot.expirationDate] being `null`,
 * never treated as an expiring subscription with a missing date.
 */
internal fun RevenueCatCustomerSnapshot.toFamilyAccess(): FamilyAccess {
    val entitlement = familyPlusEntitlement ?: return FamilyAccess.free()
    val expirationDate = entitlement.expirationDate
    return if (expirationDate == null) {
        FamilyAccess.lifetime()
    } else {
        FamilyAccess.subscription(expiresAt = expirationDate, willRenew = entitlement.willRenew)
    }
}

internal fun RevenueCatCustomerSnapshot.toAccessSnapshot(verifiedAt: Instant): AccessSnapshot {
    val activeEntitlements = activeEntitlementIds
        .mapNotNull { identifier -> runCatching { EntitlementId(identifier) }.getOrNull() }
        .toSet()

    return AccessSnapshot(
        familyAccess = toFamilyAccess(),
        activeEntitlements = activeEntitlements,
        verifiedAt = verifiedAt,
    )
}

/** Same narrow-adapter reasoning as [RevenueCatEntitlementSnapshot] — [packageType] is a plain SDK enum (safe/trivial to reference directly), everything else is a plain string. */
internal data class RevenueCatPackageSnapshot(
    val productId: String,
    val packageType: PackageType,
    val title: String,
    val formattedPrice: String,
    val offeringIdentifier: String,
)

internal fun Package.toPackageSnapshot(offeringIdentifier: String): RevenueCatPackageSnapshot = RevenueCatPackageSnapshot(
    productId = storeProduct.id,
    packageType = packageType,
    title = storeProduct.title,
    formattedPrice = storeProduct.price.formatted,
    offeringIdentifier = offeringIdentifier,
)

/**
 * Prefers [Offering.availablePackages] from whichever offering RevenueCat currently considers
 * current — never a hardcoded lookup by [DEFAULT_OFFERING_ID] alone, so a dashboard change to
 * which offering is current never breaks this app; [DEFAULT_OFFERING_ID] is only a documented
 * fallback if `current` is somehow unset. A package whose type isn't one this app sells (monthly/
 * annual/lifetime) is silently skipped, never a reason to fail the whole offering. [Offering.identifier]
 * is threaded onto every resulting [PurchasePackage] — Step 14.4's `offering_identifier` analytics
 * property reads this back, never a hardcoded/guessed value.
 */
internal fun Offering.toPurchasePackages(): List<PurchasePackage> =
    availablePackages.mapNotNull { it.toPackageSnapshot(identifier).toPurchasePackageOrNull() }

internal fun RevenueCatPackageSnapshot.toPurchasePackageOrNull(): PurchasePackage? {
    val (type, billingPeriod) = when (packageType) {
        PackageType.MONTHLY -> PurchasePackageType.MONTHLY to BillingPeriod.MONTH
        PackageType.ANNUAL -> PurchasePackageType.ANNUAL to BillingPeriod.YEAR
        PackageType.LIFETIME -> PurchasePackageType.LIFETIME to null
        else -> return null
    }

    return runCatching {
        PurchasePackage(
            productId = ProductId(productId),
            type = type,
            title = title,
            formattedPrice = formattedPrice,
            billingPeriod = billingPeriod,
            offeringIdentifier = offeringIdentifier,
        )
    }.getOrNull()
}

/**
 * [PurchasesErrorCode.PurchaseCancelledError] and [PurchasesErrorCode.PaymentPendingError] are
 * deliberately not mapped here — a cancelled purchase is [com.togetherly.domain.purchase.PurchaseResult.Cancelled]
 * and a pending one is [com.togetherly.domain.purchase.PurchaseResult.Pending], neither a
 * [PurchaseError]; callers must check for both before falling back to this mapping.
 */
internal fun PurchasesErrorCode.toPurchaseError(): PurchaseError = when (this) {
    PurchasesErrorCode.ProductNotAvailableForPurchaseError -> PurchaseError.ProductUnavailable
    PurchasesErrorCode.StoreProblemError -> PurchaseError.StoreUnavailable
    PurchasesErrorCode.NetworkError -> PurchaseError.NetworkProblem
    PurchasesErrorCode.ConfigurationError,
    PurchasesErrorCode.InvalidCredentialsError,
    PurchasesErrorCode.InvalidAppUserIdError,
    -> PurchaseError.ConfigurationProblem
    PurchasesErrorCode.PurchaseNotAllowedError,
    PurchasesErrorCode.PurchaseInvalidError,
    -> PurchaseError.PurchaseNotAllowed
    PurchasesErrorCode.ProductAlreadyPurchasedError -> PurchaseError.AlreadyOwned
    else -> PurchaseError.Unknown
}

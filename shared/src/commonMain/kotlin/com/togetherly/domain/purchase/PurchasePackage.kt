package com.togetherly.domain.purchase

import com.togetherly.domain.validation.requireConsistentState
import com.togetherly.domain.validation.requireNotBlank

/**
 * title and formattedPrice are pre-formatted, store-supplied strings — the domain never
 * calculates or parses localized prices.
 */
data class PurchasePackage(
    val productId: ProductId,
    val type: PurchasePackageType,
    val title: String,
    val formattedPrice: String,
    val billingPeriod: BillingPeriod?,
    val offeringIdentifier: String,
) {
    init {
        requireNotBlank(title)
        requireNotBlank(formattedPrice)
        requireNotBlank(offeringIdentifier)
        when (type) {
            PurchasePackageType.MONTHLY -> requireConsistentState(billingPeriod == BillingPeriod.MONTH)
            PurchasePackageType.ANNUAL -> requireConsistentState(billingPeriod == BillingPeriod.YEAR)
            PurchasePackageType.LIFETIME -> requireConsistentState(billingPeriod == null)
        }
    }
}

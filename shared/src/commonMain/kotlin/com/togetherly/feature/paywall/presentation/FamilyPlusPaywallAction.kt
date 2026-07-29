package com.togetherly.feature.paywall.presentation

import com.togetherly.domain.purchase.ProductId

sealed interface FamilyPlusPaywallAction {
    data class PackageSelected(val productId: ProductId) : FamilyPlusPaywallAction
    data object PurchaseClicked : FamilyPlusPaywallAction
    data object RestoreClicked : FamilyPlusPaywallAction
    data object RetryClicked : FamilyPlusPaywallAction
    data object CloseClicked : FamilyPlusPaywallAction
    data object PrivacyPolicyClicked : FamilyPlusPaywallAction
    data object TermsClicked : FamilyPlusPaywallAction
}

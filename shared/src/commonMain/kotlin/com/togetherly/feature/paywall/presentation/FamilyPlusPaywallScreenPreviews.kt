package com.togetherly.feature.paywall.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.core.ui.UiText
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.domain.purchase.BillingPeriod
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.ProductId
import com.togetherly.domain.purchase.PurchasePackage
import com.togetherly.domain.purchase.PurchasePackageType
import com.togetherly.feature.paywall.model.FamilyPlusPaywallUiState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/**
 * A living catalogue of the paywall's states — never a substitute for
 * [FamilyPlusPaywallViewModelTest] (behavior) or the Compose UI tests (semantics/interaction).
 */

private val monthlyPackage = PurchasePackage(
    productId = ProductId("togetherly_monthly"),
    type = PurchasePackageType.MONTHLY,
    title = "Monthly",
    formattedPrice = "$4.99",
    billingPeriod = BillingPeriod.MONTH,
    offeringIdentifier = "default",
)

private val annualPackage = PurchasePackage(
    productId = ProductId("togetherly_annual"),
    type = PurchasePackageType.ANNUAL,
    title = "Annual",
    formattedPrice = "$39.99",
    billingPeriod = BillingPeriod.YEAR,
    offeringIdentifier = "default",
)

private val lifetimePackage = PurchasePackage(
    productId = ProductId("togetherly_lifetime"),
    type = PurchasePackageType.LIFETIME,
    title = "Lifetime",
    formattedPrice = "$99.99",
    billingPeriod = null,
    offeringIdentifier = "default",
)

private val allPackages = listOf(monthlyPackage, annualPackage, lifetimePackage).toPersistentList()

@Composable
private fun FamilyPlusPaywallPreview(state: FamilyPlusPaywallUiState) {
    TogetherlyTheme {
        FamilyPlusPaywallScreen(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun FamilyPlusPaywallLoadingPreview() {
    FamilyPlusPaywallPreview(FamilyPlusPaywallUiState(isLoading = true, packages = persistentListOf()))
}

@Preview
@Composable
private fun FamilyPlusPaywallLoadedPreview() {
    FamilyPlusPaywallPreview(
        FamilyPlusPaywallUiState(isLoading = false, packages = allPackages, selectedPackageId = monthlyPackage.productId),
    )
}

@Preview
@Composable
private fun FamilyPlusPaywallAnnualSelectedPreview() {
    FamilyPlusPaywallPreview(
        FamilyPlusPaywallUiState(isLoading = false, packages = allPackages, selectedPackageId = annualPackage.productId),
    )
}

@Preview
@Composable
private fun FamilyPlusPaywallPurchasingPreview() {
    FamilyPlusPaywallPreview(
        FamilyPlusPaywallUiState(
            isLoading = false,
            packages = allPackages,
            selectedPackageId = annualPackage.productId,
            isPurchasing = true,
        ),
    )
}

@Preview
@Composable
private fun FamilyPlusPaywallRestoringPreview() {
    FamilyPlusPaywallPreview(
        FamilyPlusPaywallUiState(
            isLoading = false,
            packages = allPackages,
            selectedPackageId = monthlyPackage.productId,
            isRestoring = true,
        ),
    )
}

@Preview
@Composable
private fun FamilyPlusPaywallErrorPreview() {
    FamilyPlusPaywallPreview(
        FamilyPlusPaywallUiState(isLoading = false, packages = persistentListOf(), error = UiText.Dynamic("We couldn't load the plans right now. Please try again later.")),
    )
}

@Preview
@Composable
private fun FamilyPlusPaywallAlreadyPremiumPreview() {
    FamilyPlusPaywallPreview(
        FamilyPlusPaywallUiState(isLoading = false, packages = allPackages, access = FamilyAccess.lifetime()),
    )
}

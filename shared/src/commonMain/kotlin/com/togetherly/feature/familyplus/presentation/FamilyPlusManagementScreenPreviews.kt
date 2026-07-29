package com.togetherly.feature.familyplus.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.core.ui.UiText
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.feature.familyplus.model.FamilyPlusManagementUiState
import kotlin.time.Instant

private val EXPIRY = Instant.parse("2026-08-15T00:00:00Z")

@Composable
private fun FamilyPlusManagementPreview(state: FamilyPlusManagementUiState) {
    TogetherlyTheme {
        FamilyPlusManagementScreen(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun FamilyPlusManagementLoadingPreview() {
    FamilyPlusManagementPreview(FamilyPlusManagementUiState(isLoading = true))
}

@Preview
@Composable
private fun FamilyPlusManagementFreePreview() {
    FamilyPlusManagementPreview(
        FamilyPlusManagementUiState(isLoading = false, access = FamilyAccess.free(), customerCenterAvailable = true),
    )
}

@Preview
@Composable
private fun FamilyPlusManagementProviderUnavailablePreview() {
    FamilyPlusManagementPreview(
        FamilyPlusManagementUiState(isLoading = false, access = FamilyAccess.free(), customerCenterAvailable = false),
    )
}

@Preview
@Composable
private fun FamilyPlusManagementRenewingSubscriptionPreview() {
    FamilyPlusManagementPreview(
        FamilyPlusManagementUiState(
            isLoading = false,
            access = FamilyAccess.subscription(expiresAt = EXPIRY, willRenew = true),
            renewalInfo = UiText.Dynamic("Renews on August 15, 2026"),
            customerCenterAvailable = true,
        ),
    )
}

@Preview
@Composable
private fun FamilyPlusManagementEndingSubscriptionPreview() {
    FamilyPlusManagementPreview(
        FamilyPlusManagementUiState(
            isLoading = false,
            access = FamilyAccess.subscription(expiresAt = EXPIRY, willRenew = false),
            renewalInfo = UiText.Dynamic("Ends on August 15, 2026"),
            customerCenterAvailable = true,
        ),
    )
}

@Preview
@Composable
private fun FamilyPlusManagementLifetimePreview() {
    FamilyPlusManagementPreview(
        FamilyPlusManagementUiState(
            isLoading = false,
            access = FamilyAccess.lifetime(),
            renewalInfo = UiText.Dynamic("You have lifetime access to Family Plus"),
            customerCenterAvailable = true,
        ),
    )
}

@Preview
@Composable
private fun FamilyPlusManagementRestoringPreview() {
    FamilyPlusManagementPreview(
        FamilyPlusManagementUiState(isLoading = false, access = FamilyAccess.free(), isRestoring = true, customerCenterAvailable = true),
    )
}

@Preview
@Composable
private fun FamilyPlusManagementMessagePreview() {
    FamilyPlusManagementPreview(
        FamilyPlusManagementUiState(
            isLoading = false,
            access = FamilyAccess.free(),
            customerCenterAvailable = true,
            message = UiText.Dynamic("We didn't find any previous purchases to restore."),
        ),
    )
}

@Preview
@Composable
private fun FamilyPlusManagementCustomerCenterUnavailablePreview() {
    FamilyPlusManagementPreview(
        FamilyPlusManagementUiState(
            isLoading = false,
            access = FamilyAccess.subscription(expiresAt = EXPIRY, willRenew = true),
            renewalInfo = UiText.Dynamic("Renews on August 15, 2026"),
            customerCenterAvailable = false,
            message = UiText.Dynamic("Subscription management isn't available right now. Please try again later."),
        ),
    )
}

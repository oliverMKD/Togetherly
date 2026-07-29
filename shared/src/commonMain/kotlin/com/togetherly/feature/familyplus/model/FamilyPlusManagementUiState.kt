package com.togetherly.feature.familyplus.model

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText
import com.togetherly.domain.purchase.FamilyAccess

/**
 * [access] alone decides free vs. premium content — never a separately-tracked "is premium"
 * flag that could drift from it. [renewalInfo] is `null` whenever RevenueCat hasn't told us an
 * expiration/renewal date (a lifetime purchase, or access not yet loaded) — this screen never
 * guesses a renewal date RevenueCat doesn't provide (see [com.togetherly.feature.familyplus.mapper.toRenewalInfo]).
 * [customerCenterAvailable] gates the "Manage subscription" action — `false` whenever the purchase
 * provider isn't configured/ready, so tapping it can't try to open something that would fail.
 */
@Immutable
data class FamilyPlusManagementUiState(
    val isLoading: Boolean = true,
    val access: FamilyAccess = FamilyAccess.free(),
    val renewalInfo: UiText? = null,
    val isRestoring: Boolean = false,
    val customerCenterAvailable: Boolean = false,
    val message: UiText? = null,
)

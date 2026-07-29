package com.togetherly.feature.paywall.model

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.ProductId
import com.togetherly.domain.purchase.PurchasePackage
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/**
 * [PurchasePackage] is reused directly rather than mapped into a paywall-specific UI model — it is
 * already presentation-safe, store-localized display data (title, formatted price) with no
 * RevenueCat type or logic attached; wrapping it again would just duplicate it.
 *
 * [access] drives which content the screen shows: [FamilyAccess.isPlus] true means the family is
 * already premium (by the time this loads, or because a purchase/restore just confirmed it) — the
 * screen shows the warm "already premium" state instead of the purchase flow (see
 * [com.togetherly.feature.paywall.presentation.FamilyPlusPaywallScreen]).
 *
 * [error] doubles as a general transient status slot, not only failures — a gentle "purchase
 * cancelled, no worries" message lands here too (see [com.togetherly.domain.purchase.PurchaseError]'s
 * mapping in `core.ui`), so the screen never needs a second field just to distinguish severity.
 */
@Immutable
data class FamilyPlusPaywallUiState(
    val isLoading: Boolean = true,
    val packages: PersistentList<PurchasePackage> = persistentListOf(),
    val selectedPackageId: ProductId? = null,
    val isPurchasing: Boolean = false,
    val isRestoring: Boolean = false,
    val access: FamilyAccess = FamilyAccess.free(),
    val error: UiText? = null,
    /** Set once at construction from the triggering [com.togetherly.feature.paywall.model.PaywallContext] — never changes afterward; `null` for contexts with no special intro line (e.g. opened intentionally from Family Plus management). */
    val introMessage: UiText? = null,
)

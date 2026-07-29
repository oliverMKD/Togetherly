package com.togetherly.domain.purchase

/**
 * Whether the purchase provider (RevenueCat) is ready to be asked about entitlements — provider-
 * neutral, since nothing here mentions RevenueCat. [NotConfigured] means no usable public SDK key
 * was found (a fresh checkout with no local key, or a deliberately key-less CI/test environment);
 * the app stays fully usable in free mode in that case, it just never asks the provider anything.
 * [Failed] means a key was present but SDK configuration itself failed (see
 * [com.togetherly.data.purchase.RevenueCatConfigurator]) — also free-mode, distinguished from
 * [NotConfigured] only for diagnostics, never shown to a family as an error. [Ready] does not by
 * itself mean the family has Family Plus — it only means [EntitlementRepository][com.togetherly.domain.purchase.repository.EntitlementRepository]
 * can now be asked; free access is still the default answer until proven otherwise.
 */
sealed interface PurchaseStartupState {
    data object NotConfigured : PurchaseStartupState
    data object Initializing : PurchaseStartupState
    data object Ready : PurchaseStartupState
    data object Failed : PurchaseStartupState
}

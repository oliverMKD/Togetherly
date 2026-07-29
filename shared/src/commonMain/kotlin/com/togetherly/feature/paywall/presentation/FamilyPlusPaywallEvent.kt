package com.togetherly.feature.paywall.presentation

/** [OpenExternalLink] is resolved by [FamilyPlusPaywallRoute] via Compose's own `LocalUriHandler` — opening a URL is a platform action, not business logic, so it never happens inside the ViewModel. */
sealed interface FamilyPlusPaywallEvent {
    data object Close : FamilyPlusPaywallEvent
    data object PurchaseSucceeded : FamilyPlusPaywallEvent
    data object RestoreSucceeded : FamilyPlusPaywallEvent
    data class OpenExternalLink(val url: String) : FamilyPlusPaywallEvent
}

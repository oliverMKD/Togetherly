package com.togetherly.feature.paywall.model

import kotlinx.serialization.Serializable

/**
 * Provider-neutral — never a RevenueCat concept. Only changes the paywall's intro copy/analytics;
 * every context still purchases against the same current offering (see
 * [com.togetherly.navigation.destination.RootDestination.FamilyPlusPaywall]'s own KDoc). Lives
 * here rather than under `navigation.destination` since it's really a paywall-presentation concern
 * that also happens to ride along on a nav argument — [com.togetherly.navigation.host.TogetherlyNavHost]
 * already depends on `feature.paywall` for [com.togetherly.feature.paywall.presentation.FamilyPlusPaywallRoute],
 * so depending on this too keeps the dependency direction consistent (navigation depends on
 * features, never the reverse). [PREMIUM_PACK] has no production trigger yet (Step 12 builds the
 * full premium catalogue) — it exists now so the access-rule/nav-hook plumbing doesn't need to
 * change again when that catalogue arrives.
 */
@Serializable
enum class PaywallContext {
    PREMIUM_REROLL,
    PREMIUM_QUEST,
    PREMIUM_PACK,
    FAMILY_PLUS_MANAGEMENT,
}

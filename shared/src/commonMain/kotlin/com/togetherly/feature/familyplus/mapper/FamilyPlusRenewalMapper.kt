package com.togetherly.feature.familyplus.mapper

import com.togetherly.core.datetime.localizedDateDisplay
import com.togetherly.core.ui.UiText
import com.togetherly.domain.purchase.AccessSource
import com.togetherly.domain.purchase.FamilyAccess
import kotlinx.datetime.TimeZone
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.familyplus_ends_on
import togetherly.shared.generated.resources.familyplus_lifetime_member
import togetherly.shared.generated.resources.familyplus_renews_on

/**
 * Never invents a renewal/expiration date RevenueCat didn't supply: a subscription with no
 * [FamilyAccess.expiresAt] (shouldn't happen per [FamilyAccess]'s own invariants, but this stays
 * defensive) or a non-premium/non-lifetime [FamilyAccess] simply has no renewal info to show.
 * [FamilyAccess.willRenew] distinguishes "renews on" from "ends on" — never assumed either way.
 */
fun FamilyAccess.toRenewalInfo(timeZone: TimeZone): UiText? = when {
    source == AccessSource.LIFETIME -> UiText.Resource(Res.string.familyplus_lifetime_member)
    expiresAt == null -> null
    willRenew == true -> UiText.Resource(Res.string.familyplus_renews_on, listOf(expiresAt.localizedDateDisplay(timeZone)))
    else -> UiText.Resource(Res.string.familyplus_ends_on, listOf(expiresAt.localizedDateDisplay(timeZone)))
}

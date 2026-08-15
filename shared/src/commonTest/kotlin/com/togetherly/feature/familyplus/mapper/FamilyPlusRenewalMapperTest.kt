package com.togetherly.feature.familyplus.mapper

import com.togetherly.core.datetime.localizedDateDisplay
import com.togetherly.core.ui.UiText
import com.togetherly.domain.purchase.FamilyAccess
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Instant
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.familyplus_ends_on
import togetherly.shared.generated.resources.familyplus_lifetime_member
import togetherly.shared.generated.resources.familyplus_renews_on

/**
 * Never asserts an exact formatted date string — [localizedDateDisplay] is locale-dependent, and
 * pinning one locale's output here would make this test fail on any other. Instead, each
 * date-bearing branch is checked against the *same* production formatter call, so this only tests
 * [toRenewalInfo]'s own branching (which resource, which argument), not date formatting itself
 * (already covered by `LocalizedDateTimeFormattersTest`).
 */
class FamilyPlusRenewalMapperTest {

    private val timeZone = TimeZone.UTC
    private val expiresAt = Instant.parse("2026-09-15T00:00:00Z")

    @Test
    fun lifetimeAccessAlwaysShowsTheLifetimeMemberMessage() {
        val info = FamilyAccess.lifetime().toRenewalInfo(timeZone)

        assertEquals(UiText.Resource(Res.string.familyplus_lifetime_member), info)
    }

    @Test
    fun freeAccessHasNoRenewalInfo() {
        val info = FamilyAccess.free().toRenewalInfo(timeZone)

        assertNull(info)
    }

    @Test
    fun promotionalAccessWithNoExpirationHasNoRenewalInfo() {
        val info = FamilyAccess.promotional(expiresAt = null).toRenewalInfo(timeZone)

        assertNull(info)
    }

    @Test
    fun subscriptionThatWillRenewShowsARenewsOnMessageWithTheFormattedDate() {
        val info = FamilyAccess.subscription(expiresAt, willRenew = true).toRenewalInfo(timeZone)

        val resource = assertIs<UiText.Resource>(info)
        assertEquals(Res.string.familyplus_renews_on, resource.resource)
        assertEquals(listOf(expiresAt.localizedDateDisplay(timeZone)), resource.formatArgs)
    }

    @Test
    fun subscriptionThatWillNotRenewShowsAnEndsOnMessageWithTheFormattedDate() {
        val info = FamilyAccess.subscription(expiresAt, willRenew = false).toRenewalInfo(timeZone)

        val resource = assertIs<UiText.Resource>(info)
        assertEquals(Res.string.familyplus_ends_on, resource.resource)
        assertEquals(listOf(expiresAt.localizedDateDisplay(timeZone)), resource.formatArgs)
    }

    /** [FamilyAccess.promotional]/[FamilyAccess.cached] never carry a [FamilyAccess.willRenew] value — both fall to the same "ends on" branch [toRenewalInfo] uses for "not renewing". */
    @Test
    fun promotionalAccessWithAnExpirationShowsAnEndsOnMessage() {
        val info = FamilyAccess.promotional(expiresAt).toRenewalInfo(timeZone)

        val resource = assertIs<UiText.Resource>(info)
        assertEquals(Res.string.familyplus_ends_on, resource.resource)
        assertEquals(listOf(expiresAt.localizedDateDisplay(timeZone)), resource.formatArgs)
    }
}

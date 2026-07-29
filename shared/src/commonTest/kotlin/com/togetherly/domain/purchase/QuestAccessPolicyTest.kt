package com.togetherly.domain.purchase

import com.togetherly.domain.quest.QuestAccess
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T12:00:00Z")
private val FAMILY_PLUS = EntitlementId("family_plus")

class QuestAccessPolicyTest {

    private val policy = QuestAccessPolicy()

    private fun snapshot(
        familyAccess: FamilyAccess,
        activeEntitlements: Set<EntitlementId> = if (familyAccess.isPlus) setOf(FAMILY_PLUS) else emptySet(),
        verifiedAt: Instant = NOW,
    ) = AccessSnapshot(
        familyAccess = familyAccess,
        activeEntitlements = activeEntitlements,
        verifiedAt = verifiedAt,
    )

    @Test
    fun freeQuestAccessibleToFreeFamily() {
        val result = policy.canAccess(
            questAccess = QuestAccess.Free,
            accessSnapshot = snapshot(FamilyAccess.free()),
            now = NOW,
        )

        assertTrue(result)
    }

    @Test
    fun freeQuestRemainsAccessibleDuringExpiredOrMissingPremiumAccess() {
        val expiredSubscription = snapshot(
            FamilyAccess.subscription(expiresAt = NOW - 1.hours, willRenew = false),
        )

        val result = policy.canAccess(
            questAccess = QuestAccess.Free,
            accessSnapshot = expiredSubscription,
            now = NOW,
        )

        assertTrue(result)
    }

    @Test
    fun premiumQuestBlockedForFreeAccess() {
        val result = policy.canAccess(
            questAccess = QuestAccess.Premium(FAMILY_PLUS),
            accessSnapshot = snapshot(FamilyAccess.free()),
            now = NOW,
        )

        assertFalse(result)
    }

    @Test
    fun subscriptionUnlocksPremium() {
        val result = policy.canAccess(
            questAccess = QuestAccess.Premium(FAMILY_PLUS),
            accessSnapshot = snapshot(
                FamilyAccess.subscription(expiresAt = NOW + 1.hours, willRenew = true),
            ),
            now = NOW,
        )

        assertTrue(result)
    }

    @Test
    fun lifetimeUnlocksPremium() {
        val result = policy.canAccess(
            questAccess = QuestAccess.Premium(FAMILY_PLUS),
            accessSnapshot = snapshot(FamilyAccess.lifetime()),
            now = NOW,
        )

        assertTrue(result)
    }

    @Test
    fun promotionalAccessUnlocksPremiumWhileValid() {
        val indefinite = policy.canAccess(
            questAccess = QuestAccess.Premium(FAMILY_PLUS),
            accessSnapshot = snapshot(FamilyAccess.promotional(expiresAt = null)),
            now = NOW,
        )
        val stillValid = policy.canAccess(
            questAccess = QuestAccess.Premium(FAMILY_PLUS),
            accessSnapshot = snapshot(FamilyAccess.promotional(expiresAt = NOW + 1.hours)),
            now = NOW,
        )

        assertTrue(indefinite)
        assertTrue(stillValid)
    }

    @Test
    fun expiredSubscriptionDoesNotUnlockPremium() {
        val result = policy.canAccess(
            questAccess = QuestAccess.Premium(FAMILY_PLUS),
            accessSnapshot = snapshot(
                FamilyAccess.subscription(expiresAt = NOW - 1.hours, willRenew = false),
            ),
            now = NOW,
        )

        assertFalse(result)
    }

    @Test
    fun cachedPremiumWithinGracePeriodUnlocksPremium() {
        val result = policy.canAccess(
            questAccess = QuestAccess.Premium(FAMILY_PLUS),
            accessSnapshot = snapshot(
                familyAccess = FamilyAccess.cached(isPlus = true, expiresAt = null),
                verifiedAt = NOW - 71.hours,
            ),
            now = NOW,
        )

        assertTrue(result)
    }

    @Test
    fun cachedPremiumOutsideGracePeriodDoesNotUnlockPremium() {
        val result = policy.canAccess(
            questAccess = QuestAccess.Premium(FAMILY_PLUS),
            accessSnapshot = snapshot(
                familyAccess = FamilyAccess.cached(isPlus = true, expiresAt = null),
                verifiedAt = NOW - 73.hours,
            ),
            now = NOW,
        )

        assertFalse(result)
    }

    @Test
    fun wrongEntitlementDoesNotUnlockAPremiumQuest() {
        val result = policy.canAccess(
            questAccess = QuestAccess.Premium(FAMILY_PLUS),
            accessSnapshot = snapshot(
                familyAccess = FamilyAccess.lifetime(),
                activeEntitlements = setOf(EntitlementId("some_other_entitlement")),
            ),
            now = NOW,
        )

        assertFalse(result)
    }

    @Test
    fun policyUsesSuppliedNowAndRemainsDeterministic() {
        val snapshot = snapshot(FamilyAccess.subscription(expiresAt = NOW, willRenew = true))

        val justBeforeExpiry = policy.canAccess(QuestAccess.Premium(FAMILY_PLUS), snapshot, now = NOW - 1.hours)
        val atExpiry = policy.canAccess(QuestAccess.Premium(FAMILY_PLUS), snapshot, now = NOW)
        val afterExpiry = policy.canAccess(QuestAccess.Premium(FAMILY_PLUS), snapshot, now = NOW + 1.hours)

        assertTrue(justBeforeExpiry)
        assertFalse(atExpiry)
        assertFalse(afterExpiry)
    }

    @Test
    fun isFamilyPlusActiveIsEntitlementAgnostic() {
        // No entitlement id in activeEntitlements at all — isFamilyPlusActive only cares whether
        // *some* Family Plus access is currently active, not which entitlement backs it (that's
        // canAccess's own job, exercised separately above).
        val result = policy.isFamilyPlusActive(
            snapshot(FamilyAccess.lifetime(), activeEntitlements = emptySet()),
            now = NOW,
        )

        assertTrue(result)
    }

    @Test
    fun isFamilyPlusActiveIsFalseForAFreeFamily() {
        val result = policy.isFamilyPlusActive(snapshot(FamilyAccess.free()), now = NOW)

        assertFalse(result)
    }

    @Test
    fun isFamilyPlusActiveIsFalseForAnExpiredSubscription() {
        val result = policy.isFamilyPlusActive(
            snapshot(FamilyAccess.subscription(expiresAt = NOW - 1.hours, willRenew = false)),
            now = NOW,
        )

        assertFalse(result)
    }
}

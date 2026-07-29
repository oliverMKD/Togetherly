package com.togetherly.domain.purchase

import com.togetherly.domain.quest.QuestAccess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * The single, coherent source of truth for whether a quest is accessible — [com.togetherly.domain.quest.FamilyQuest]
 * intentionally does not carry a competing access check of its own.
 *
 * Last-known cached premium access remains valid for [offlineGracePeriod] after it was last
 * verified; after that window, premium access requires a fresh verification. Free content is
 * always accessible regardless of purchase state, so a purchase-initialization failure can
 * never block it.
 */
class QuestAccessPolicy(
    private val offlineGracePeriod: Duration = 72.hours,
) {
    fun canAccess(
        questAccess: QuestAccess,
        accessSnapshot: AccessSnapshot,
        now: Instant,
    ): Boolean = when (questAccess) {
        is QuestAccess.Free -> true
        is QuestAccess.Premium ->
            isFamilyPlusActive(accessSnapshot, now) &&
                questAccess.requiredEntitlement in accessSnapshot.activeEntitlements
    }

    /**
     * Entitlement-agnostic — "is *some* Family Plus access currently active" regardless of which
     * entitlement id [canAccess] separately checks for. Reused by anything that needs the same
     * cached/lifetime/expiry reasoning without being about a specific quest or pack — e.g. the
     * daily reroll allowance's `hasFamilyPlus` (see [com.togetherly.domain.daily.RerollAllowancePolicy]),
     * so that logic isn't duplicated at every call site.
     */
    fun isFamilyPlusActive(accessSnapshot: AccessSnapshot, now: Instant): Boolean {
        val access = accessSnapshot.familyAccess
        if (!access.isPlus) return false

        return when (access.source) {
            AccessSource.FREE -> false
            AccessSource.LIFETIME -> true
            AccessSource.SUBSCRIPTION, AccessSource.PROMOTIONAL ->
                access.expiresAt == null || now < access.expiresAt
            AccessSource.CACHED ->
                (access.expiresAt == null || now < access.expiresAt) &&
                    now < accessSnapshot.verifiedAt + offlineGracePeriod
        }
    }
}

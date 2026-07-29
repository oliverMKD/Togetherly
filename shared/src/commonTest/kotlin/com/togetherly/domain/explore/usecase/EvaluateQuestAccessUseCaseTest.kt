package com.togetherly.domain.explore.usecase

import com.togetherly.core.datetime.TestAppClock
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.validFamilyQuest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")
private val FAMILY_PLUS = EntitlementId("family_plus")

class EvaluateQuestAccessUseCaseTest {

    private fun useCase(access: FamilyAccess, activeEntitlements: Set<EntitlementId> = if (access.isPlus) setOf(FAMILY_PLUS) else emptySet()) =
        EvaluateQuestAccessUseCase(
            entitlementRepository = FakeEntitlementRepository(AccessSnapshot(access, activeEntitlements, NOW)),
            questAccessPolicy = QuestAccessPolicy(),
            clock = TestAppClock(NOW),
        )

    @Test
    fun freeQuestIsAlwaysAccessibleForAFreeFamily() = runTest {
        val quest = validFamilyQuest(access = QuestAccess.Free)

        val result = useCase(FamilyAccess.free())(quest)

        assertTrue(result)
    }

    @Test
    fun premiumQuestIsNotAccessibleForAFreeFamily() = runTest {
        val quest = validFamilyQuest(access = QuestAccess.Premium(FAMILY_PLUS))

        val result = useCase(FamilyAccess.free())(quest)

        assertFalse(result)
    }

    @Test
    fun premiumQuestIsAccessibleForAFamilyPlusFamily() = runTest {
        val quest = validFamilyQuest(access = QuestAccess.Premium(FAMILY_PLUS))

        val result = useCase(FamilyAccess.lifetime())(quest)

        assertTrue(result)
    }

    @Test
    fun freeQuestIsAlwaysAccessibleEvenForAFamilyPlusFamily() = runTest {
        val quest = validFamilyQuest(access = QuestAccess.Free)

        val result = useCase(FamilyAccess.lifetime())(quest)

        assertTrue(result)
    }

    @Test
    fun expiredSubscriptionNoLongerUnlocksPremiumContent() = runTest {
        val quest = validFamilyQuest(access = QuestAccess.Premium(FAMILY_PLUS))
        val expired = FamilyAccess.subscription(expiresAt = NOW - 1.hours, willRenew = false)

        val result = useCase(expired)(quest)

        assertFalse(result)
    }
}

package com.togetherly.domain.explore.usecase

import com.togetherly.core.datetime.TestAppClock
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.validQuestPack
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")
private val FAMILY_PLUS = EntitlementId("family_plus")

class EvaluatePackAccessUseCaseTest {

    private fun useCase(access: FamilyAccess, activeEntitlements: Set<EntitlementId> = if (access.isPlus) setOf(FAMILY_PLUS) else emptySet()) =
        EvaluatePackAccessUseCase(
            entitlementRepository = FakeEntitlementRepository(AccessSnapshot(access, activeEntitlements, NOW)),
            questAccessPolicy = QuestAccessPolicy(),
            clock = TestAppClock(NOW),
        )

    @Test
    fun freePackIsAlwaysAccessibleForAFreeFamily() = runTest {
        val pack = validQuestPack(access = QuestAccess.Free)

        val result = useCase(FamilyAccess.free())(pack)

        assertTrue(result)
    }

    @Test
    fun premiumPackIsNotAccessibleForAFreeFamily() = runTest {
        val pack = validQuestPack(access = QuestAccess.Premium(FAMILY_PLUS))

        val result = useCase(FamilyAccess.free())(pack)

        assertFalse(result)
    }

    @Test
    fun premiumPackIsAccessibleForAFamilyPlusFamily() = runTest {
        val pack = validQuestPack(access = QuestAccess.Premium(FAMILY_PLUS))

        val result = useCase(FamilyAccess.lifetime())(pack)

        assertTrue(result)
    }
}

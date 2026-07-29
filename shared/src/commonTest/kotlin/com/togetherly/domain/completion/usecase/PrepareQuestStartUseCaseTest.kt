package com.togetherly.domain.completion.usecase

import com.togetherly.core.datetime.TestAppClock
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.validFamilyQuest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")
private const val FAMILY_PLUS = "family_plus"

/**
 * This is the same gate [com.togetherly.feature.questdetail.presentation.QuestDetailViewModel]
 * calls before every start attempt — regardless of whether the user reached Quest Detail from
 * Today, Explore, Pack Details, Saved quests, a deep link, or restored navigation state, they all
 * land on the same [com.togetherly.navigation.destination.RootDestination.QuestDetail] screen and
 * go through this exact use case, so testing it directly covers every one of those entry points.
 */
class PrepareQuestStartUseCaseTest {

    private fun useCase(
        questRepository: FakeQuestRepository,
        entitlementRepository: FakeEntitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)),
    ) = PrepareQuestStartUseCase(questRepository, entitlementRepository, QuestAccessPolicy(), TestAppClock(NOW))

    @Test
    fun freeQuestIsAlwaysAllowed() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Free)
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }

        val result = useCase(questRepository)(quest.id)

        assertEquals(PrepareQuestStartResult.Allowed(quest), result)
    }

    @Test
    fun premiumQuestIsRequiresFamilyPlusForAFreeFamily() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)))
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }

        val result = useCase(questRepository)(quest.id)

        assertEquals(PrepareQuestStartResult.RequiresFamilyPlus(quest.id), result)
    }

    @Test
    fun premiumQuestIsAllowedForAFamilyWithTheEntitlement() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)))
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }
        val entitlementRepository = FakeEntitlementRepository(
            AccessSnapshot(FamilyAccess.lifetime(), setOf(EntitlementId(FAMILY_PLUS)), NOW),
        )

        val result = useCase(questRepository, entitlementRepository)(quest.id)

        assertEquals(PrepareQuestStartResult.Allowed(quest), result)
    }

    @Test
    fun missingQuestIsNotFound() = runTest {
        val questRepository = FakeQuestRepository()

        val result = useCase(questRepository)(QuestId("missing"))

        assertEquals(PrepareQuestStartResult.NotFound, result)
    }

    @Test
    fun aFailedEntitlementReadNeverUnlocksAPremiumQuest() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)))
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }
        val entitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))

        val result = useCase(questRepository, entitlementRepository)(quest.id)

        assertEquals(PrepareQuestStartResult.RequiresFamilyPlus(quest.id), result)
    }
}

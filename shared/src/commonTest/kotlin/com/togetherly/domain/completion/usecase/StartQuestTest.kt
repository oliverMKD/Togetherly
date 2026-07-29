package com.togetherly.domain.completion.usecase

import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.id.SequentialIdGenerator
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.repository.FakeQuestSessionTransaction
import com.togetherly.domain.completion.validActiveQuestSession
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.family.FamilyProfile
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.family.repository.FakeFamilyRepository
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.validFamilyQuest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")
private const val FAMILY_PLUS = "family_plus"

private fun validProfile() = FamilyProfile(
    id = FamilyId("family-1"),
    displayName = null,
    childAgeBands = setOf(AgeBand.AGE_6_TO_8),
    interests = setOf(QuestCategory.CREATE),
    preferredDurations = setOf(DurationBand.TEN_MINUTES),
    locationPreference = LocationPreference.BOTH,
    preparationPreference = PreparationPreference.SIMPLE_MATERIALS,
    reminderPreference = null,
    createdAt = NOW,
    updatedAt = NOW,
)

class StartQuestTest {

    @Test
    fun startCreatesAndSavesSession() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), version = 2)
        val familyRepository = FakeFamilyRepository().apply { saveProfile(validProfile()) }
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }
        val completionRepository = FakeCompletionRepository()
        val questSessionTransaction = FakeQuestSessionTransaction(completionRepository)
        val entitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val useCase = StartQuest(
            familyRepository, questRepository, questSessionTransaction,
            entitlementRepository, QuestAccessPolicy(), TestAppClock(NOW), SequentialIdGenerator("completion"),
        )

        val result = useCase(quest.id)

        val session = (result as DataResult.Success).value
        assertEquals(CompletionId("completion-0"), session.completionId)
        assertEquals(quest.id, session.questId)
        assertEquals(2, session.questVersion)
        assertEquals(NOW, session.startedAt)
        assertEquals(DataResult.Success(session), completionRepository.getActiveSession())
    }

    @Test
    fun startingWhileAnotherSessionExistsFollowsTheDocumentedConflictRule() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val familyRepository = FakeFamilyRepository().apply { saveProfile(validProfile()) }
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }
        val completionRepository = FakeCompletionRepository().apply {
            saveActiveSession(validActiveQuestSession())
        }
        val questSessionTransaction = FakeQuestSessionTransaction(completionRepository)
        val entitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val useCase = StartQuest(
            familyRepository, questRepository, questSessionTransaction,
            entitlementRepository, QuestAccessPolicy(), TestAppClock(NOW), SequentialIdGenerator(),
        )

        val result = useCase(quest.id)

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.ACTIVE_SESSION_CONFLICT)), result)
    }

    @Test
    fun lockedPremiumQuestStartIsRejectedAndNoSessionIsCreated() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)))
        val familyRepository = FakeFamilyRepository().apply { saveProfile(validProfile()) }
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }
        val completionRepository = FakeCompletionRepository()
        val questSessionTransaction = FakeQuestSessionTransaction(completionRepository)
        val entitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val useCase = StartQuest(
            familyRepository, questRepository, questSessionTransaction,
            entitlementRepository, QuestAccessPolicy(), TestAppClock(NOW), SequentialIdGenerator(),
        )

        val result = useCase(quest.id)

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.PREMIUM_ACCESS_REQUIRED)), result)
        assertNull((completionRepository.getActiveSession() as DataResult.Success).value)
    }

    @Test
    fun unlockedPremiumQuestStartsNormally() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)))
        val familyRepository = FakeFamilyRepository().apply { saveProfile(validProfile()) }
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }
        val completionRepository = FakeCompletionRepository()
        val questSessionTransaction = FakeQuestSessionTransaction(completionRepository)
        val entitlementRepository = FakeEntitlementRepository(
            AccessSnapshot(FamilyAccess.lifetime(), setOf(EntitlementId(FAMILY_PLUS)), NOW),
        )
        val useCase = StartQuest(
            familyRepository, questRepository, questSessionTransaction,
            entitlementRepository, QuestAccessPolicy(), TestAppClock(NOW), SequentialIdGenerator(),
        )

        val result = useCase(quest.id)

        assertTrue(result is DataResult.Success)
    }
}

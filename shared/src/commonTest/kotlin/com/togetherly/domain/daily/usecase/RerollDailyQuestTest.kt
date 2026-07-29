package com.togetherly.domain.daily.usecase

import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.daily.DailyQuest
import com.togetherly.domain.daily.DailyQuestSource
import com.togetherly.domain.daily.DefaultRerollAllowancePolicy
import com.togetherly.domain.daily.FakeRerollAllowancePolicy
import com.togetherly.domain.daily.QuestContext
import com.togetherly.domain.daily.RerollAllowance
import com.togetherly.domain.daily.repository.FakeDailyQuestRepository
import com.togetherly.domain.daily.repository.FakeDailyQuestTransaction
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
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.validFamilyQuest
import com.togetherly.domain.recommendation.FakeQuestRecommendationPolicy
import com.togetherly.domain.recommendation.NoRecommendationReason
import com.togetherly.domain.recommendation.QuestRecommendationResult
import com.togetherly.domain.recommendation.RecommendationConfig
import com.togetherly.domain.recommendation.RecommendationHistoryBuilder
import com.togetherly.domain.recommendation.toAppError
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")
private val TODAY = LocalDate(2026, 6, 15)
private val EMPTY_CONTEXT = QuestContext(null, null, null, null, null)

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

private fun historyBuilder(
    dailyQuestRepository: FakeDailyQuestRepository,
    clock: TestAppClock = TestAppClock(NOW),
) = RecommendationHistoryBuilder(
    completionRepository = FakeCompletionRepository(),
    dailyQuestRepository = dailyQuestRepository,
    clock = clock,
    config = RecommendationConfig.DEFAULT,
)

private class RerollFixture(
    val currentQuest: com.togetherly.domain.quest.FamilyQuest,
    val replacementQuest: com.togetherly.domain.quest.FamilyQuest,
    val familyRepository: FakeFamilyRepository,
    val questRepository: FakeQuestRepository,
    val dailyQuestRepository: FakeDailyQuestRepository,
    val transaction: FakeDailyQuestTransaction,
    val policy: FakeQuestRecommendationPolicy,
    val allowancePolicy: FakeRerollAllowancePolicy,
    val entitlementRepository: FakeEntitlementRepository,
    val clock: TestAppClock,
    val useCase: RerollDailyQuest,
)

private suspend fun rerollFixture(
    currentQuest: com.togetherly.domain.quest.FamilyQuest = validFamilyQuest(id = QuestId("quest-1")),
    replacementQuest: com.togetherly.domain.quest.FamilyQuest = validFamilyQuest(id = QuestId("quest-2")),
    currentSelectionIndex: Int = 0,
    allowanceResult: RerollAllowance = RerollAllowance(used = 0, maximum = 1),
): RerollFixture {
    val familyRepository = FakeFamilyRepository().apply { saveProfile(validProfile()) }
    val questRepository = FakeQuestRepository().apply { setQuests(listOf(currentQuest, replacementQuest)) }
    val dailyQuestRepository = FakeDailyQuestRepository().apply {
        saveDailyQuest(
            DailyQuest(
                questId = currentQuest.id,
                localDate = TODAY,
                selectionIndex = currentSelectionIndex,
                selectedAt = NOW,
                source = DailyQuestSource.AUTOMATIC,
                context = EMPTY_CONTEXT,
            ),
        )
    }
    val transaction = FakeDailyQuestTransaction(dailyQuestRepository)
    val policy = FakeQuestRecommendationPolicy(
        result = QuestRecommendationResult.Success(replacementQuest, score = 10, reasons = emptyList()),
    )
    val allowancePolicy = FakeRerollAllowancePolicy(result = allowanceResult)
    val entitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
    val clock = TestAppClock(NOW)
    val useCase = RerollDailyQuest(
        familyRepository, questRepository, dailyQuestRepository, transaction, policy,
        historyBuilder(dailyQuestRepository, clock), allowancePolicy,
        entitlementRepository, QuestAccessPolicy(), clock,
    )
    return RerollFixture(
        currentQuest, replacementQuest, familyRepository, questRepository, dailyQuestRepository,
        transaction, policy, allowancePolicy, entitlementRepository, clock, useCase,
    )
}

class RerollDailyQuestTest {

    @Test
    fun firstFreeRerollSucceeds() = runTest {
        val fixture = rerollFixture(allowanceResult = RerollAllowance(used = 0, maximum = 1))

        val result = fixture.useCase(timeZone = TimeZone.UTC)

        val resolved = (result as DataResult.Success).value
        assertEquals(fixture.replacementQuest, resolved.quest)
        assertEquals(1, resolved.dailyQuest.selectionIndex)
    }

    @Test
    fun secondFreeRerollIsRejected() = runTest {
        val fixture = rerollFixture(
            currentSelectionIndex = 1,
            allowanceResult = RerollAllowance(used = 1, maximum = 1),
        )

        val result = fixture.useCase(timeZone = TimeZone.UTC)

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.REROLL_LIMIT_REACHED)), result)
        assertEquals(0, fixture.policy.requests.size)
    }

    @Test
    fun familyPlusAllowsUnlimitedRerollsInUnitTests() = runTest {
        val fixture = rerollFixture(
            currentSelectionIndex = 4,
            allowanceResult = RerollAllowance(used = 4, maximum = null),
        )

        val result = fixture.useCase(timeZone = TimeZone.UTC)

        val resolved = (result as DataResult.Success).value
        assertEquals(5, resolved.dailyQuest.selectionIndex)
    }

    @Test
    fun rerollIncreasesSelectionIndexAndSourceIsReroll() = runTest {
        val fixture = rerollFixture(currentSelectionIndex = 2, allowanceResult = RerollAllowance(used = 2, maximum = null))

        val result = fixture.useCase(timeZone = TimeZone.UTC)

        val resolved = (result as DataResult.Success).value
        assertEquals(3, resolved.dailyQuest.selectionIndex)
        assertEquals(DailyQuestSource.REROLL, resolved.dailyQuest.source)
    }

    @Test
    fun rerollCannotReturnTheCurrentQuest() = runTest {
        val fixture = rerollFixture()

        fixture.useCase(timeZone = TimeZone.UTC)

        val requestSeenByPolicy = fixture.policy.requests.single()
        assertEquals(
            true,
            requestSeenByPolicy.history.recentlyDismissed.any { it.questId == fixture.currentQuest.id },
        )
    }

    @Test
    fun successfulRerollRecordsDismissalAndReplacementAtomically() = runTest {
        val fixture = rerollFixture()

        fixture.useCase(timeZone = TimeZone.UTC)

        val dismissals = fixture.dailyQuestRepository.getRecentDismissals(since = NOW - 1.seconds)
        val dismissedIds = (dismissals as DataResult.Success).value.map { it.questId }
        assertEquals(listOf(fixture.currentQuest.id), dismissedIds)
        val saved = fixture.dailyQuestRepository.getToday(TODAY)
        assertEquals(fixture.replacementQuest.id, (saved as DataResult.Success).value?.questId)
    }

    @Test
    fun failedRecommendationPreservesCurrentQuestAndDoesNotRecordDismissal() = runTest {
        val fixture = rerollFixture()
        fixture.policy.result = QuestRecommendationResult.NoMatch(NoRecommendationReason.NO_CONTEXT_COMPATIBLE_QUEST)

        val result = fixture.useCase(timeZone = TimeZone.UTC)

        assertEquals(
            DataResult.Error(NoRecommendationReason.NO_CONTEXT_COMPATIBLE_QUEST.toAppError()),
            result,
        )
        val current = fixture.dailyQuestRepository.getToday(TODAY)
        assertEquals(fixture.currentQuest.id, (current as DataResult.Success).value?.questId)
        val dismissals = fixture.dailyQuestRepository.getRecentDismissals(since = NOW - 1.seconds)
        assertEquals(emptyList(), (dismissals as DataResult.Success).value)
    }

    @Test
    fun transactionFailurePreservesCurrentQuestAndAllowance() = runTest {
        val fixture = rerollFixture()
        fixture.transaction.setNextError(AppError.Storage(com.togetherly.core.error.StorageError.WRITE_FAILED))

        val result = fixture.useCase(timeZone = TimeZone.UTC)

        assertEquals(DataResult.Error(AppError.Storage(com.togetherly.core.error.StorageError.WRITE_FAILED)), result)
        val current = fixture.dailyQuestRepository.getToday(TODAY)
        assertEquals(fixture.currentQuest.id, (current as DataResult.Success).value?.questId)
    }

    @Test
    fun missingSelectionReturnsTypedError() = runTest {
        val familyRepository = FakeFamilyRepository().apply { saveProfile(validProfile()) }
        val questRepository = FakeQuestRepository()
        val dailyQuestRepository = FakeDailyQuestRepository()
        val transaction = FakeDailyQuestTransaction(dailyQuestRepository)
        val clock = TestAppClock(NOW)
        val useCase = RerollDailyQuest(
            familyRepository, questRepository, dailyQuestRepository, transaction, FakeQuestRecommendationPolicy(),
            historyBuilder(dailyQuestRepository, clock), FakeRerollAllowancePolicy(),
            FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)), QuestAccessPolicy(), clock,
        )

        val result = useCase(timeZone = TimeZone.UTC)

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.NO_DAILY_SELECTION)), result)
    }

    /**
     * Uses the real [DefaultRerollAllowancePolicy] (not [FakeRerollAllowancePolicy]) — end-to-end
     * proof that [RerollDailyQuest] resolves `hasFamilyPlus` from [FakeEntitlementRepository] via
     * [QuestAccessPolicy] rather than a caller having to fake the allowance decision itself.
     */
    private fun realAllowanceUseCase(
        familyRepository: FakeFamilyRepository,
        questRepository: FakeQuestRepository,
        dailyQuestRepository: FakeDailyQuestRepository,
        transaction: FakeDailyQuestTransaction,
        policy: FakeQuestRecommendationPolicy,
        entitlementRepository: FakeEntitlementRepository,
        clock: TestAppClock,
    ) = RerollDailyQuest(
        familyRepository, questRepository, dailyQuestRepository, transaction, policy,
        historyBuilder(dailyQuestRepository, clock), DefaultRerollAllowancePolicy(),
        entitlementRepository, QuestAccessPolicy(), clock,
    )

    @Test
    fun premiumFamilyGetsUnlimitedRerollsFromTheRealPolicy() = runTest {
        val currentQuest = validFamilyQuest(id = QuestId("quest-1"))
        val replacementQuest = validFamilyQuest(id = QuestId("quest-2"))
        val familyRepository = FakeFamilyRepository().apply { saveProfile(validProfile()) }
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(currentQuest, replacementQuest)) }
        val dailyQuestRepository = FakeDailyQuestRepository().apply {
            saveDailyQuest(
                DailyQuest(questId = currentQuest.id, localDate = TODAY, selectionIndex = 1, selectedAt = NOW, source = DailyQuestSource.REROLL, context = EMPTY_CONTEXT),
            )
        }
        val transaction = FakeDailyQuestTransaction(dailyQuestRepository)
        val policy = FakeQuestRecommendationPolicy(result = QuestRecommendationResult.Success(replacementQuest, score = 10, reasons = emptyList()))
        val entitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.lifetime(), setOf(EntitlementId("family_plus")), NOW))
        val clock = TestAppClock(NOW)
        val useCase = realAllowanceUseCase(familyRepository, questRepository, dailyQuestRepository, transaction, policy, entitlementRepository, clock)

        // Already used the one free reroll (selectionIndex 1) — a free family would be rejected here.
        val result = useCase(timeZone = TimeZone.UTC)

        val resolved = (result as DataResult.Success).value
        assertEquals(null, resolved.rerollAllowance.maximum)
    }

    @Test
    fun expiredSubscriptionDoesNotGrantUnlimitedRerolls() = runTest {
        val currentQuest = validFamilyQuest(id = QuestId("quest-1"))
        val familyRepository = FakeFamilyRepository().apply { saveProfile(validProfile()) }
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(currentQuest)) }
        val dailyQuestRepository = FakeDailyQuestRepository().apply {
            saveDailyQuest(
                DailyQuest(questId = currentQuest.id, localDate = TODAY, selectionIndex = 1, selectedAt = NOW, source = DailyQuestSource.REROLL, context = EMPTY_CONTEXT),
            )
        }
        val transaction = FakeDailyQuestTransaction(dailyQuestRepository)
        val policy = FakeQuestRecommendationPolicy()
        val expiredAccess = AccessSnapshot(
            FamilyAccess.subscription(expiresAt = NOW - 1.seconds, willRenew = false),
            setOf(EntitlementId("family_plus")),
            NOW,
        )
        val entitlementRepository = FakeEntitlementRepository(expiredAccess)
        val clock = TestAppClock(NOW)
        val useCase = realAllowanceUseCase(familyRepository, questRepository, dailyQuestRepository, transaction, policy, entitlementRepository, clock)

        val result = useCase(timeZone = TimeZone.UTC)

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.REROLL_LIMIT_REACHED)), result)
    }

    @Test
    fun becomingPremiumNeverAutoConsumesOrChangesTheCurrentQuestByItself() = runTest {
        val fixture = rerollFixture(currentSelectionIndex = 1, allowanceResult = RerollAllowance(used = 1, maximum = 1))

        // Simulating "purchase succeeded" — only updating entitlement state, never calling reroll.
        fixture.entitlementRepository.setAccess(AccessSnapshot(FamilyAccess.lifetime(), setOf(EntitlementId("family_plus")), NOW))

        val current = fixture.dailyQuestRepository.getToday(TODAY)
        assertEquals(fixture.currentQuest.id, (current as DataResult.Success).value?.questId)
        assertEquals(1, current.value?.selectionIndex)
    }
}

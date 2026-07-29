package com.togetherly.domain.daily.usecase

import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.daily.DailyQuest
import com.togetherly.domain.daily.DailyQuestSource
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
private val DURATION_CONTEXT = QuestContext(DurationBand.FIVE_MINUTES, null, null, null, null)

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

private fun historyBuilder(dailyQuestRepository: FakeDailyQuestRepository, clock: TestAppClock) =
    RecommendationHistoryBuilder(
        completionRepository = FakeCompletionRepository(),
        dailyQuestRepository = dailyQuestRepository,
        clock = clock,
        config = RecommendationConfig.DEFAULT,
    )

class SelectDailyQuestForContextTest {

    @Test
    fun unchangedContextIsANoOpAndReturnsTheCurrentQuest() = runTest {
        val currentQuest = validFamilyQuest(id = QuestId("quest-1"))
        val familyRepository = FakeFamilyRepository().apply { saveProfile(validProfile()) }
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(currentQuest)) }
        val dailyQuestRepository = FakeDailyQuestRepository().apply {
            saveDailyQuest(
                DailyQuest(
                    questId = currentQuest.id,
                    localDate = TODAY,
                    selectionIndex = 0,
                    selectedAt = NOW,
                    source = DailyQuestSource.AUTOMATIC,
                    context = DURATION_CONTEXT,
                ),
            )
        }
        val transaction = FakeDailyQuestTransaction(dailyQuestRepository)
        val policy = FakeQuestRecommendationPolicy()
        val clock = TestAppClock(NOW)
        val useCase = SelectDailyQuestForContext(
            familyRepository, questRepository, dailyQuestRepository, transaction, policy,
            historyBuilder(dailyQuestRepository, clock), FakeRerollAllowancePolicy(), FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)), QuestAccessPolicy(), clock,
        )

        val result = useCase(context = DURATION_CONTEXT, timeZone = TimeZone.UTC)

        val resolved = (result as DataResult.Success).value
        assertEquals(currentQuest, resolved.quest)
        assertEquals(0, policy.requests.size)
        val dismissals = dailyQuestRepository.getRecentDismissals(since = NOW - 1.seconds)
        assertEquals(emptyList(), (dismissals as DataResult.Success).value)
    }

    @Test
    fun changedContextExplicitlySelectsAndDismissesThePreviousQuest() = runTest {
        val currentQuest = validFamilyQuest(id = QuestId("quest-1"))
        val replacementQuest = validFamilyQuest(id = QuestId("quest-2"))
        val familyRepository = FakeFamilyRepository().apply { saveProfile(validProfile()) }
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(currentQuest, replacementQuest)) }
        val dailyQuestRepository = FakeDailyQuestRepository().apply {
            saveDailyQuest(
                DailyQuest(
                    questId = currentQuest.id,
                    localDate = TODAY,
                    selectionIndex = 0,
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
        val clock = TestAppClock(NOW)
        val useCase = SelectDailyQuestForContext(
            familyRepository, questRepository, dailyQuestRepository, transaction, policy,
            historyBuilder(dailyQuestRepository, clock), FakeRerollAllowancePolicy(), FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)), QuestAccessPolicy(), clock,
        )

        val result = useCase(context = DURATION_CONTEXT, timeZone = TimeZone.UTC)

        val resolved = (result as DataResult.Success).value
        assertEquals(replacementQuest, resolved.quest)
        assertEquals(DailyQuestSource.CONTEXTUAL, resolved.dailyQuest.source)
        assertEquals(0, resolved.dailyQuest.selectionIndex)
        val dismissals = dailyQuestRepository.getRecentDismissals(since = NOW - 1.seconds)
        assertEquals(listOf(currentQuest.id), (dismissals as DataResult.Success).value.map { it.questId })
    }

    @Test
    fun applyingFiltersDoesNotConsumeRerollAllowance() = runTest {
        val currentQuest = validFamilyQuest(id = QuestId("quest-1"))
        val replacementQuest = validFamilyQuest(id = QuestId("quest-2"))
        val familyRepository = FakeFamilyRepository().apply { saveProfile(validProfile()) }
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(currentQuest, replacementQuest)) }
        val dailyQuestRepository = FakeDailyQuestRepository().apply {
            saveDailyQuest(
                DailyQuest(
                    questId = currentQuest.id,
                    localDate = TODAY,
                    selectionIndex = 0,
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
        val allowancePolicy = FakeRerollAllowancePolicy(result = RerollAllowance(used = 0, maximum = 1))
        val clock = TestAppClock(NOW)
        val useCase = SelectDailyQuestForContext(
            familyRepository, questRepository, dailyQuestRepository, transaction, policy,
            historyBuilder(dailyQuestRepository, clock), allowancePolicy, FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)), QuestAccessPolicy(), clock,
        )

        val result = useCase(context = DURATION_CONTEXT, timeZone = TimeZone.UTC)

        val resolved = (result as DataResult.Success).value
        assertEquals(0, resolved.dailyQuest.selectionIndex)
        assertEquals(listOf(0 to false), allowancePolicy.calls)
    }

    @Test
    fun applyFailurePreservesCurrentQuest() = runTest {
        val currentQuest = validFamilyQuest(id = QuestId("quest-1"))
        val familyRepository = FakeFamilyRepository().apply { saveProfile(validProfile()) }
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(currentQuest)) }
        val dailyQuestRepository = FakeDailyQuestRepository().apply {
            saveDailyQuest(
                DailyQuest(
                    questId = currentQuest.id,
                    localDate = TODAY,
                    selectionIndex = 0,
                    selectedAt = NOW,
                    source = DailyQuestSource.AUTOMATIC,
                    context = EMPTY_CONTEXT,
                ),
            )
        }
        val transaction = FakeDailyQuestTransaction(dailyQuestRepository)
        val policy = FakeQuestRecommendationPolicy(
            result = QuestRecommendationResult.NoMatch(NoRecommendationReason.NO_CONTEXT_COMPATIBLE_QUEST),
        )
        val clock = TestAppClock(NOW)
        val useCase = SelectDailyQuestForContext(
            familyRepository, questRepository, dailyQuestRepository, transaction, policy,
            historyBuilder(dailyQuestRepository, clock), FakeRerollAllowancePolicy(), FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)), QuestAccessPolicy(), clock,
        )

        val result = useCase(context = DURATION_CONTEXT, timeZone = TimeZone.UTC)

        assertEquals(DataResult.Error(NoRecommendationReason.NO_CONTEXT_COMPATIBLE_QUEST.toAppError()), result)
        val current = dailyQuestRepository.getToday(TODAY)
        assertEquals(currentQuest.id, (current as DataResult.Success).value?.questId)
        val dismissals = dailyQuestRepository.getRecentDismissals(since = NOW - 1.seconds)
        assertEquals(emptyList(), (dismissals as DataResult.Success).value)
    }

    @Test
    fun missingSelectionReturnsTypedError() = runTest {
        val familyRepository = FakeFamilyRepository().apply { saveProfile(validProfile()) }
        val questRepository = FakeQuestRepository()
        val dailyQuestRepository = FakeDailyQuestRepository()
        val transaction = FakeDailyQuestTransaction(dailyQuestRepository)
        val clock = TestAppClock(NOW)
        val useCase = SelectDailyQuestForContext(
            familyRepository, questRepository, dailyQuestRepository, transaction, FakeQuestRecommendationPolicy(),
            historyBuilder(dailyQuestRepository, clock), FakeRerollAllowancePolicy(), FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)), QuestAccessPolicy(), clock,
        )

        val result = useCase(context = DURATION_CONTEXT, timeZone = TimeZone.UTC)

        assertEquals(
            DataResult.Error(AppError.Validation(com.togetherly.core.error.ValidationError.NO_DAILY_SELECTION)),
            result,
        )
    }
}

package com.togetherly.domain.daily.usecase

import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.daily.FakeRerollAllowancePolicy
import com.togetherly.domain.daily.QuestContext
import com.togetherly.domain.daily.repository.FakeDailyQuestRepository
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
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.validFamilyQuest
import com.togetherly.domain.recommendation.DeterministicQuestRecommendationPolicy
import com.togetherly.domain.recommendation.RecommendationConfig
import com.togetherly.domain.recommendation.RecommendationHistoryBuilder
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")
private val EMPTY_CONTEXT = QuestContext(null, null, null, null, null)

private fun neutralProfile(preferredEnergyLevels: Set<EnergyLevel>) = FamilyProfile(
    id = FamilyId("family-1"),
    displayName = null,
    childAgeBands = setOf(AgeBand.AGE_6_TO_8),
    interests = setOf(QuestCategory.MEMORIES),
    preferredDurations = setOf(DurationBand.TEN_MINUTES),
    locationPreference = LocationPreference.BOTH,
    preparationPreference = PreparationPreference.ANY,
    preferredEnergyLevels = preferredEnergyLevels,
    reminderPreference = null,
    createdAt = NOW,
    updatedAt = NOW,
)

/**
 * Step 13.3's own required scenarios, exercised against the *real* [DeterministicQuestRecommendationPolicy]
 * (unlike [com.togetherly.integration.DailyQuestContentIntegrationTest], which deliberately fakes
 * the policy for plumbing-only tests — here the policy's own preference-driven behavior is exactly
 * what's under test): editing quest preferences must never change a day's already-persisted
 * selection, but must influence a *future* day's fresh selection.
 */
class QuestPreferencesDailySelectionIntegrationTest {

    @Test
    fun editingPreferencesNeverChangesTodaysAlreadySelectedQuest() = runTest {
        val calmQuest = validFamilyQuest(id = QuestId("calm-quest"), energy = EnergyLevel.CALM, durationMinutes = 10)
        val activeQuest = validFamilyQuest(id = QuestId("active-quest"), energy = EnergyLevel.ACTIVE, durationMinutes = 10)
        val familyRepository = FakeFamilyRepository()
        familyRepository.saveProfile(neutralProfile(preferredEnergyLevels = setOf(EnergyLevel.CALM)))
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(calmQuest, activeQuest)) }
        val dailyQuestRepository = FakeDailyQuestRepository()
        val clock = TestAppClock(NOW)
        val useCase = dailySelectionUseCase(familyRepository, questRepository, dailyQuestRepository, clock)

        val firstSelection = (useCase(context = EMPTY_CONTEXT, timeZone = TimeZone.UTC) as DataResult.Success).value.quest.id
        assertEquals(calmQuest.id, firstSelection)

        // The family changes their preference after today's quest is already selected.
        familyRepository.saveProfile(neutralProfile(preferredEnergyLevels = setOf(EnergyLevel.ACTIVE)))

        val secondCallSameDay = (useCase(context = EMPTY_CONTEXT, timeZone = TimeZone.UTC) as DataResult.Success).value.quest.id
        assertEquals(calmQuest.id, secondCallSameDay)
    }

    @Test
    fun aFutureDaysFreshSelectionReflectsTheUpdatedPreference() = runTest {
        val calmQuest = validFamilyQuest(id = QuestId("calm-quest"), energy = EnergyLevel.CALM, durationMinutes = 10)
        val activeQuest = validFamilyQuest(id = QuestId("active-quest"), energy = EnergyLevel.ACTIVE, durationMinutes = 10)
        val familyRepository = FakeFamilyRepository()
        familyRepository.saveProfile(neutralProfile(preferredEnergyLevels = setOf(EnergyLevel.CALM)))
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(calmQuest, activeQuest)) }
        val dailyQuestRepository = FakeDailyQuestRepository()
        val todayClock = TestAppClock(NOW)
        val todayUseCase = dailySelectionUseCase(familyRepository, questRepository, dailyQuestRepository, todayClock)
        val todaysSelection = (todayUseCase(context = EMPTY_CONTEXT, timeZone = TimeZone.UTC) as DataResult.Success).value.quest.id
        assertEquals(calmQuest.id, todaysSelection)

        familyRepository.saveProfile(neutralProfile(preferredEnergyLevels = setOf(EnergyLevel.ACTIVE)))
        val tomorrowClock = TestAppClock(NOW + 1.days)
        val tomorrowUseCase = dailySelectionUseCase(familyRepository, questRepository, dailyQuestRepository, tomorrowClock)

        val tomorrowsSelection = (tomorrowUseCase(context = EMPTY_CONTEXT, timeZone = TimeZone.UTC) as DataResult.Success).value.quest.id

        assertEquals(activeQuest.id, tomorrowsSelection)
    }

    private fun dailySelectionUseCase(
        familyRepository: FakeFamilyRepository,
        questRepository: FakeQuestRepository,
        dailyQuestRepository: FakeDailyQuestRepository,
        clock: TestAppClock,
    ) = GetOrSelectDailyQuest(
        familyRepository,
        questRepository,
        dailyQuestRepository,
        DeterministicQuestRecommendationPolicy(RecommendationConfig.DEFAULT),
        RecommendationHistoryBuilder(FakeCompletionRepository(), dailyQuestRepository, clock, RecommendationConfig.DEFAULT),
        FakeRerollAllowancePolicy(),
        FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)),
        QuestAccessPolicy(),
        clock,
    )
}

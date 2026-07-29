package com.togetherly.feature.today.presentation

import app.cash.turbine.test
import com.togetherly.core.datetime.AppTimeZoneProvider
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.DailyQuestRerollBlocked
import com.togetherly.core.telemetry.DailyQuestRerolled
import com.togetherly.core.telemetry.DailyQuestRevealed
import com.togetherly.core.telemetry.FakeProductAnalytics
import com.togetherly.core.ui.toUiText
import com.togetherly.feature.today.mapper.toTodayUiText
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.validQuestCompletion
import com.togetherly.domain.daily.FakeRerollAllowancePolicy
import com.togetherly.domain.daily.RerollAllowance
import com.togetherly.domain.daily.repository.FakeDailyQuestRepository
import com.togetherly.domain.daily.repository.FakeDailyQuestTransaction
import com.togetherly.domain.daily.usecase.GetOrSelectDailyQuest
import com.togetherly.domain.daily.usecase.RerollDailyQuest
import com.togetherly.domain.daily.usecase.SelectDailyQuestForContext
import com.togetherly.domain.family.repository.FakeFamilyRepository
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.validFamilyQuest
import com.togetherly.domain.recommendation.FakeQuestRecommendationPolicy
import com.togetherly.domain.recommendation.NoRecommendationReason
import com.togetherly.domain.recommendation.QuestRecommendationResult
import com.togetherly.domain.recommendation.RecommendationConfig
import com.togetherly.domain.recommendation.RecommendationHistoryBuilder
import com.togetherly.domain.saved.repository.FakeSavedQuestRepository
import com.togetherly.domain.saved.usecase.SetQuestSaved
import com.togetherly.domain.family.usecase.UpdateFamilyProfile
import com.togetherly.feature.family.model.FamilyProfileAction
import com.togetherly.feature.family.presentation.FamilyProfileEditorViewModel
import com.togetherly.feature.family.presentation.FamilySaveMessageStore
import com.togetherly.feature.today.model.TodayContentState
import com.togetherly.integration.testFamilyProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")
private val UTC_PROVIDER = object : AppTimeZoneProvider {
    override fun current(): TimeZone = TimeZone.UTC
}

private class TodayFixture(
    val familyRepository: FakeFamilyRepository = FakeFamilyRepository(),
    val questRepository: FakeQuestRepository = FakeQuestRepository(),
    val dailyQuestRepository: FakeDailyQuestRepository = FakeDailyQuestRepository(),
    val completionRepository: FakeCompletionRepository = FakeCompletionRepository(),
    val savedQuestRepository: FakeSavedQuestRepository = FakeSavedQuestRepository(),
    val transaction: FakeDailyQuestTransaction = FakeDailyQuestTransaction(dailyQuestRepository),
    val policy: FakeQuestRecommendationPolicy = FakeQuestRecommendationPolicy(),
    val allowancePolicy: FakeRerollAllowancePolicy = FakeRerollAllowancePolicy(),
    val entitlementRepository: FakeEntitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)),
    val questAccessPolicy: QuestAccessPolicy = QuestAccessPolicy(),
    val clock: TestAppClock = TestAppClock(NOW),
    val analytics: FakeProductAnalytics = FakeProductAnalytics().apply { setCollectionEnabled(true) },
) {
    private val historyBuilder = RecommendationHistoryBuilder(completionRepository, dailyQuestRepository, clock, RecommendationConfig.DEFAULT)
    private val getOrSelectDailyQuest = GetOrSelectDailyQuest(
        familyRepository, questRepository, dailyQuestRepository, policy, historyBuilder, allowancePolicy, entitlementRepository, questAccessPolicy, clock,
    )
    private val selectDailyQuestForContext = SelectDailyQuestForContext(
        familyRepository, questRepository, dailyQuestRepository, transaction, policy, historyBuilder, allowancePolicy, entitlementRepository, questAccessPolicy, clock,
    )
    private val rerollDailyQuest = RerollDailyQuest(
        familyRepository, questRepository, dailyQuestRepository, transaction, policy, historyBuilder, allowancePolicy, entitlementRepository, questAccessPolicy, clock,
    )
    private val setQuestSaved = SetQuestSaved(savedQuestRepository, questRepository, clock)

    val viewModel = TodayViewModel(
        getOrSelectDailyQuest, selectDailyQuestForContext, rerollDailyQuest, setQuestSaved,
        savedQuestRepository, completionRepository, familyRepository, clock, UTC_PROVIDER, analytics,
    )
}

private suspend fun readyFixture(): TodayFixture {
    val quest = validFamilyQuest(id = QuestId("quest-1"))
    val fixture = TodayFixture(
        policy = FakeQuestRecommendationPolicy(
            result = QuestRecommendationResult.Success(quest, score = 10, reasons = emptyList()),
        ),
    )
    fixture.familyRepository.saveProfile(testFamilyProfile())
    fixture.questRepository.setQuests(listOf(quest))
    return fixture
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun screenStartedShowsLoadingThenMystery() = runTest {
        val fixture = readyFixture()

        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        assertEquals(TodayContentState.Loading, fixture.viewModel.uiState.value.content)

        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value.content
        assertTrue(content is TodayContentState.Mystery)
        assertEquals(QuestId("quest-1"), content.quest.id)
    }

    @Test
    fun familyNameIsPopulatedFromTheFamilyObserver() = runTest {
        val fixture = readyFixture()
        fixture.familyRepository.saveProfile(
            testFamilyProfile().copy(displayName = com.togetherly.domain.family.FamilyDisplayName("The Firefly Family")),
        )

        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("The Firefly Family", fixture.viewModel.uiState.value.familyName)
    }

    /**
     * Step 13.2's own requirement: editing the family profile must let Today's observed state
     * react (here, [fixture]'s shared [FakeFamilyRepository] emission updates `familyName`) without
     * regenerating the daily quest already selected for today — see [TodayViewModel]'s own
     * `onScreenStarted` KDoc: `loadDailyQuest()` runs once at screen start, entirely independent of
     * the ongoing `observeProfile()` collection.
     */
    @Test
    fun todayObservesAnUpdatedFamilyProfileWithoutChangingTodaysQuest() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        val questBefore = (fixture.viewModel.uiState.value.content as TodayContentState.Mystery).quest.id

        // A dedicated, later clock — not fixture.clock (NOW, needed elsewhere for reroll-cooldown
        // math): testFamilyProfile()'s own createdAt is later than this file's NOW, and
        // FamilyProfile's init block rejects updatedAt < createdAt.
        val editorViewModel = FamilyProfileEditorViewModel(
            familyRepository = fixture.familyRepository,
            updateFamilyProfile = UpdateFamilyProfile(fixture.familyRepository, TestAppClock(NOW.plus(60.days))),
            saveMessageStore = FamilySaveMessageStore(),
        )
        editorViewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        editorViewModel.onAction(FamilyProfileAction.FamilyNameChanged("The Firefly Family"))
        editorViewModel.onAction(FamilyProfileAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertEquals("The Firefly Family", state.familyName)
        val content = state.content
        assertTrue(content is TodayContentState.Mystery)
        assertEquals(questBefore, content.quest.id)
    }

    @Test
    fun missingProfileMapsToTypedError() = runTest {
        val fixture = TodayFixture()

        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value.content
        assertTrue(content is TodayContentState.Error)
        assertEquals(AppError.Validation(ValidationError.MISSING_FAMILY_PROFILE).toUiText(), content.message)
        assertTrue(content.retryAvailable)
    }

    @Test
    fun rawDomainErrorsAreNeverExposed() = runTest {
        val fixture = TodayFixture()
        fixture.familyRepository.saveProfile(testFamilyProfile())
        fixture.policy.result = QuestRecommendationResult.NoMatch(NoRecommendationReason.NO_CONTEXT_COMPATIBLE_QUEST)

        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value.content
        assertTrue(content is TodayContentState.Error)
        // The message is the shared generic UiText — never a raw AppError/exception surfaced as text.
        assertEquals(AppError.Content(ContentError.NO_CONTEXT_COMPATIBLE_QUEST).toUiText(), content.message)
    }

    @Test
    fun revealShowsRevealedAndEmitsNoEvent() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(TodayAction.RevealClicked)
            testDispatcher.scheduler.advanceUntilIdle()
            expectNoEvents()
        }

        val content = fixture.viewModel.uiState.value.content
        assertTrue(content is TodayContentState.Revealed)
        assertEquals(QuestId("quest-1"), content.quest.id)
    }

    @Test
    fun revealedStateIsStableAcrossRepeatedReads() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(TodayAction.RevealClicked)

        val first = fixture.viewModel.uiState.value.content
        val second = fixture.viewModel.uiState.value.content
        assertEquals(first, second)
        assertTrue(second is TodayContentState.Revealed)
    }

    @Test
    fun retryReloadsAfterAnEarlierFailure() = runTest {
        val fixture = TodayFixture()
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fixture.viewModel.uiState.value.content is TodayContentState.Error)

        val quest = validFamilyQuest(id = QuestId("quest-1"))
        fixture.familyRepository.saveProfile(testFamilyProfile())
        fixture.questRepository.setQuests(listOf(quest))
        fixture.policy.result = QuestRecommendationResult.Success(quest, score = 10, reasons = emptyList())

        fixture.viewModel.onAction(TodayAction.RetryClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fixture.viewModel.uiState.value.content is TodayContentState.Mystery)
    }

    @Test
    fun filtersOpenAndClose() = runTest {
        val fixture = readyFixture()

        fixture.viewModel.onAction(TodayAction.FiltersClicked)
        assertTrue(fixture.viewModel.uiState.value.filtersVisible)

        fixture.viewModel.onAction(TodayAction.FiltersDismissed)
        assertFalse(fixture.viewModel.uiState.value.filtersVisible)
    }

    @Test
    fun activeFilterCountReflectsHowManyGroupsAreSet() = runTest {
        val fixture = readyFixture()

        assertEquals(0, fixture.viewModel.uiState.value.filters.activeCount)

        fixture.viewModel.onAction(TodayAction.DurationFilterChanged(com.togetherly.domain.family.DurationBand.FIVE_MINUTES))
        fixture.viewModel.onAction(TodayAction.LocationFilterChanged(com.togetherly.domain.quest.QuestLocation.INDOOR))

        assertEquals(2, fixture.viewModel.uiState.value.filters.activeCount)
    }

    @Test
    fun clearAllResetsEveryFilterGroup() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onAction(TodayAction.DurationFilterChanged(com.togetherly.domain.family.DurationBand.FIVE_MINUTES))
        fixture.viewModel.onAction(TodayAction.CategoryFilterChanged(com.togetherly.domain.quest.QuestCategory.SILLY))

        fixture.viewModel.onAction(TodayAction.ClearFiltersClicked)

        assertEquals(0, fixture.viewModel.uiState.value.filters.activeCount)
        assertEquals(null, fixture.viewModel.uiState.value.filters.duration)
        assertEquals(null, fixture.viewModel.uiState.value.filters.category)
    }

    @Test
    fun filterChangesOnlyUpdateDraftAndNeverCallThePolicy() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        val requestsBefore = fixture.policy.requests.size

        fixture.viewModel.onAction(TodayAction.DurationFilterChanged(com.togetherly.domain.family.DurationBand.FIVE_MINUTES))

        assertEquals(com.togetherly.domain.family.DurationBand.FIVE_MINUTES, fixture.viewModel.uiState.value.filters.duration)
        assertEquals(requestsBefore, fixture.policy.requests.size)
        assertTrue(fixture.viewModel.uiState.value.content is TodayContentState.Mystery)
    }

    @Test
    fun dismissingFiltersDiscardsDraftChanges() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onAction(TodayAction.FiltersClicked)
        fixture.viewModel.onAction(TodayAction.DurationFilterChanged(com.togetherly.domain.family.DurationBand.FIVE_MINUTES))

        fixture.viewModel.onAction(TodayAction.FiltersDismissed)

        assertEquals(null, fixture.viewModel.uiState.value.filters.duration)
    }

    @Test
    fun applySuccessCallsContextualSelectionAndReturnsToMystery() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(TodayAction.RevealClicked)

        val replacement = validFamilyQuest(id = QuestId("quest-2"))
        fixture.questRepository.setQuests(listOf(validFamilyQuest(id = QuestId("quest-1")), replacement))
        fixture.policy.result = QuestRecommendationResult.Success(replacement, score = 10, reasons = emptyList())

        fixture.viewModel.onAction(TodayAction.DurationFilterChanged(com.togetherly.domain.family.DurationBand.FIVE_MINUTES))
        fixture.viewModel.onAction(TodayAction.ApplyFiltersClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value.content
        assertTrue(content is TodayContentState.Mystery)
        assertEquals(QuestId("quest-2"), content.quest.id)
        assertFalse(fixture.viewModel.uiState.value.isApplyingFilters)
        assertFalse(fixture.viewModel.uiState.value.filtersVisible)
    }

    @Test
    fun applyFailurePreservesCurrentQuest() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(TodayAction.RevealClicked)
        val before = fixture.viewModel.uiState.value.content

        fixture.policy.result = QuestRecommendationResult.NoMatch(NoRecommendationReason.NO_CONTEXT_COMPATIBLE_QUEST)
        fixture.viewModel.onAction(TodayAction.DurationFilterChanged(com.togetherly.domain.family.DurationBand.FIVE_MINUTES))
        fixture.viewModel.onAction(TodayAction.ApplyFiltersClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(before, fixture.viewModel.uiState.value.content)
        assertEquals(
            AppError.Content(ContentError.NO_CONTEXT_COMPATIBLE_QUEST).toTodayUiText(),
            fixture.viewModel.uiState.value.transientError,
        )
    }

    @Test
    fun rerollClickedOpensConfirmationWithoutCallingThePolicy() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        val requestsBefore = fixture.policy.requests.size

        fixture.viewModel.onAction(TodayAction.RerollClicked)

        assertTrue(fixture.viewModel.uiState.value.rerollConfirmationVisible)
        assertEquals(requestsBefore, fixture.policy.requests.size)
    }

    @Test
    fun rerollCancelledKeepsTheCurrentQuest() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(TodayAction.RevealClicked)
        val before = fixture.viewModel.uiState.value.content

        fixture.viewModel.onAction(TodayAction.RerollClicked)
        fixture.viewModel.onAction(TodayAction.RerollCancelled)

        assertFalse(fixture.viewModel.uiState.value.rerollConfirmationVisible)
        assertEquals(before, fixture.viewModel.uiState.value.content)
    }

    @Test
    fun rerollSuccessReturnsToMystery() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(TodayAction.RevealClicked)

        val replacement = validFamilyQuest(id = QuestId("quest-2"))
        fixture.questRepository.setQuests(listOf(validFamilyQuest(id = QuestId("quest-1")), replacement))
        fixture.policy.result = QuestRecommendationResult.Success(replacement, score = 10, reasons = emptyList())

        fixture.viewModel.onAction(TodayAction.RerollClicked)
        fixture.viewModel.onAction(TodayAction.RerollConfirmed)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value.content
        assertTrue(content is TodayContentState.Mystery)
        assertEquals(QuestId("quest-2"), content.quest.id)
        assertFalse(fixture.viewModel.uiState.value.isRerolling)
        assertFalse(fixture.viewModel.uiState.value.rerollConfirmationVisible)
    }

    @Test
    fun rerollFailurePreservesCurrentQuestAndAllowance() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(TodayAction.RevealClicked)
        val before = fixture.viewModel.uiState.value.content
        val allowanceBefore = fixture.viewModel.uiState.value.rerollsRemaining

        fixture.policy.result = QuestRecommendationResult.NoMatch(NoRecommendationReason.ALL_CANDIDATES_IN_COOLDOWN)

        fixture.viewModel.onAction(TodayAction.RerollClicked)
        fixture.viewModel.onAction(TodayAction.RerollConfirmed)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(before, fixture.viewModel.uiState.value.content)
        assertEquals(allowanceBefore, fixture.viewModel.uiState.value.rerollsRemaining)
    }

    @Test
    fun rerollLimitReachedEmitsEventWithoutShowingConfirmation() = runTest {
        val fixture = readyFixture()
        fixture.allowancePolicy.result = RerollAllowance(used = 1, maximum = 1)
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(TodayAction.RerollClicked)
            assertEquals(TodayEvent.RerollLimitReached, awaitItem())
        }
        assertFalse(fixture.viewModel.uiState.value.rerollConfirmationVisible)
    }

    @Test
    fun saveTogglesDesiredStateAndPersists() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(TodayAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value.content
        assertTrue(content is TodayContentState.Mystery)
        assertTrue(content.quest.isSaved)
        assertEquals(true, (fixture.savedQuestRepository.isSaved(QuestId("quest-1"))).let { (it as com.togetherly.core.result.DataResult.Success).value })
    }

    @Test
    fun startClickedEmitsExactlyOneOpenQuestDetailEvent() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(TodayAction.StartClicked)
            assertEquals(TodayEvent.OpenQuestDetail(QuestId("quest-1")), awaitItem())
        }
    }

    @Test
    fun alreadyCompletedDailyQuestShowsCompletedState() = runTest {
        val fixture = readyFixture()
        fixture.completionRepository.saveCompletion(
            validQuestCompletion(questId = QuestId("quest-1"), completedAt = NOW),
        )

        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value.content
        assertTrue(content is TodayContentState.Completed)
        assertEquals(QuestId("quest-1"), content.quest.id)
    }

    @Test
    fun completedQuestOnADifferentDayIsNotTreatedAsCompletedToday() = runTest {
        val fixture = readyFixture()
        fixture.completionRepository.saveCompletion(
            validQuestCompletion(questId = QuestId("quest-1"), completedAt = NOW - 24.hours),
        )

        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value.content
        assertTrue(content is TodayContentState.Mystery)
    }

    @Test
    fun duplicateScreenStartedDoesNotDuplicateLoad() = runTest {
        val fixture = readyFixture()

        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fixture.policy.requests.size)
    }

    // -- Analytics --------------------------------------------------------------------------

    @Test
    fun screenStartedCapturesTheTodayScreenExactlyOnce() = runTest {
        val fixture = readyFixture()

        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(AnalyticsScreen.TODAY), fixture.analytics.screensViewed)
    }

    @Test
    fun revealCapturesDailyQuestRevealedWithTheQuestsRealProperties() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val fixture = readyFixture()
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(TodayAction.RevealClicked)

        val event = fixture.analytics.capturedEvents.single() as DailyQuestRevealed
        assertEquals(quest.category, event.category)
        assertEquals(quest.energy, event.energyLevel)
        assertEquals(quest.access, event.accessRequired)
    }

    @Test
    fun revealingTwiceNeverCapturesTwice() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(TodayAction.RevealClicked)
        fixture.viewModel.onAction(TodayAction.RevealClicked)

        assertEquals(1, fixture.analytics.capturedEvents.count { it is DailyQuestRevealed })
    }

    @Test
    fun rerollSuccessCapturesDailyQuestRerolled() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val replacement = validFamilyQuest(id = QuestId("quest-2"))
        fixture.questRepository.setQuests(listOf(validFamilyQuest(id = QuestId("quest-1")), replacement))
        fixture.policy.result = QuestRecommendationResult.Success(replacement, score = 10, reasons = emptyList())

        fixture.viewModel.onAction(TodayAction.RerollClicked)
        fixture.viewModel.onAction(TodayAction.RerollConfirmed)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = fixture.analytics.capturedEvents.single { it is DailyQuestRerolled } as DailyQuestRerolled
        assertEquals(replacement.category, event.category)
        assertEquals(replacement.access, event.accessRequired)
    }

    @Test
    fun rerollFailureCapturesNoRerolledEvent() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.policy.result = QuestRecommendationResult.NoMatch(NoRecommendationReason.ALL_CANDIDATES_IN_COOLDOWN)
        fixture.viewModel.onAction(TodayAction.RerollClicked)
        fixture.viewModel.onAction(TodayAction.RerollConfirmed)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fixture.analytics.capturedEvents.none { it is DailyQuestRerolled })
    }

    @Test
    fun rerollBlockedByAllowanceCapturesRerollBlockedRegardlessOfWhichCallSiteBlockedIt() = runTest {
        val fixture = readyFixture()
        fixture.allowancePolicy.result = RerollAllowance(used = 1, maximum = 1)
        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(TodayAction.RerollClicked)

        assertEquals(listOf<com.togetherly.core.telemetry.AnalyticsEvent>(DailyQuestRerollBlocked), fixture.analytics.capturedEvents)
    }

    @Test
    fun noEventsAreCapturedWithoutConsent() = runTest {
        val fixture = readyFixture()
        fixture.analytics.setCollectionEnabled(false)

        fixture.viewModel.onAction(TodayAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(TodayAction.RevealClicked)

        assertTrue(fixture.analytics.capturedEvents.isEmpty())
        assertTrue(fixture.analytics.screensViewed.isEmpty())
    }
}

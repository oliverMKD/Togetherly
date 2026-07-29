package com.togetherly.feature.explore.presentation

import app.cash.turbine.test
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.ExploreFiltered
import com.togetherly.core.telemetry.ExploreSearched
import com.togetherly.core.telemetry.FakeProductAnalytics
import com.togetherly.core.telemetry.QuestSaved
import com.togetherly.domain.explore.usecase.EvaluatePackAccessUseCase
import com.togetherly.domain.explore.usecase.EvaluateQuestAccessUseCase
import com.togetherly.domain.explore.usecase.FilterQuestsUseCase
import com.togetherly.domain.explore.usecase.ObserveExploreCatalogueUseCase
import com.togetherly.domain.explore.usecase.ObserveSavedQuestIdsUseCase
import com.togetherly.domain.explore.usecase.SearchQuestsUseCase
import com.togetherly.domain.explore.usecase.ToggleSavedQuestUseCase
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.domain.quest.QuestTitle
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.validFamilyQuest
import com.togetherly.domain.quest.validQuestPack
import com.togetherly.domain.saved.repository.FakeSavedQuestRepository
import com.togetherly.domain.saved.usecase.SetQuestSaved
import com.togetherly.feature.explore.model.ExploreEmptyState
import com.togetherly.feature.today.model.QuestCategoryUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")
private const val FAMILY_PLUS = "family_plus"

private class ExploreFixture(
    val questRepository: FakeQuestRepository = FakeQuestRepository(),
    val savedQuestRepository: FakeSavedQuestRepository = FakeSavedQuestRepository(),
    val entitlementRepository: FakeEntitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)),
    val questAccessPolicy: QuestAccessPolicy = QuestAccessPolicy(),
    val clock: TestAppClock = TestAppClock(NOW),
    val filterStore: ExploreFilterStore = ExploreFilterStore(),
    val analytics: FakeProductAnalytics = FakeProductAnalytics().apply { setCollectionEnabled(true) },
) {
    private val setQuestSaved = SetQuestSaved(savedQuestRepository, questRepository, clock)

    val viewModel = ExploreViewModel(
        ObserveExploreCatalogueUseCase(questRepository),
        SearchQuestsUseCase(),
        FilterQuestsUseCase(),
        ObserveSavedQuestIdsUseCase(savedQuestRepository),
        ToggleSavedQuestUseCase(savedQuestRepository, setQuestSaved),
        EvaluateQuestAccessUseCase(entitlementRepository, questAccessPolicy, clock),
        EvaluatePackAccessUseCase(entitlementRepository, questAccessPolicy, clock),
        entitlementRepository,
        filterStore,
        analytics,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun readyFixture(): ExploreFixture {
        val fixture = ExploreFixture()
        val questOne = validFamilyQuest(id = QuestId("quest-1"), title = QuestTitle("Backyard Scavenger Hunt"), category = QuestCategory.DISCOVER)
        val questTwo = validFamilyQuest(
            id = QuestId("quest-2"),
            title = QuestTitle("Draw a Shared Creature"),
            category = QuestCategory.CREATE,
            access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)),
        )
        val pack = validQuestPack(id = QuestPackId("pack-1"), questIds = listOf(questOne.id, questTwo.id))
        fixture.questRepository.setQuests(listOf(questOne, questTwo))
        fixture.questRepository.setPacks(listOf(pack))
        return fixture
    }

    @Test
    fun screenStartedShowsBothQuestsAndTheFreeAccessSnapshot() = runTest {
        val fixture = readyFixture()

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.quests.size)
        assertFalse(state.access.isPlus)
    }

    @Test
    fun freeQuestIsNeverLocked() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        val freeQuest = fixture.viewModel.uiState.value.quests.first { it.id == QuestId("quest-1") }
        assertFalse(freeQuest.locked)
    }

    @Test
    fun premiumQuestIsLockedForAFreeFamily() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        val premiumQuest = fixture.viewModel.uiState.value.quests.first { it.id == QuestId("quest-2") }
        assertTrue(premiumQuest.locked)
        assertTrue(premiumQuest.isPremium)
    }

    @Test
    fun searchImmediatelyUpdatesTheVisibleQueryBeforeTheDebounceSettles() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(ExploreAction.SearchChanged("draw"))

        assertEquals("draw", fixture.viewModel.uiState.value.searchQuery)
        assertTrue(fixture.viewModel.uiState.value.isSearchActive)
    }

    @Test
    fun searchNarrowsQuestsAfterTheDebounceSettles() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(ExploreAction.SearchChanged("draw"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertEquals(1, state.quests.size)
        assertEquals(QuestId("quest-2"), state.quests.single().id)
    }

    @Test
    fun clearingSearchReturnsToTheBrowseState() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(ExploreAction.SearchChanged("draw"))
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(ExploreAction.SearchCleared)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertFalse(state.isSearchActive)
        assertEquals(2, state.quests.size)
    }

    @Test
    fun searchForANonMatchingWordShowsTheSearchEmptyState() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(ExploreAction.SearchChanged("xyzzy"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertTrue(state.quests.isEmpty())
        assertEquals(ExploreEmptyState.SEARCH, state.emptyState)
    }

    @Test
    fun categorySelectedNarrowsQuestsToThatCategory() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(ExploreAction.CategorySelected(QuestCategoryUi.CREATE))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertEquals(QuestCategoryUi.CREATE, state.selectedCategory)
        assertEquals(1, state.quests.size)
        assertEquals(QuestId("quest-2"), state.quests.single().id)
    }

    @Test
    fun categoryWithNoMatchesShowsTheCategoryEmptyState() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(ExploreAction.CategorySelected(QuestCategoryUi.SILLY))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertTrue(state.quests.isEmpty())
        assertEquals(ExploreEmptyState.FILTER, state.emptyState)
    }

    @Test
    fun selectingTheSameCategoryTwiceClearsIt() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(ExploreAction.CategorySelected(QuestCategoryUi.CREATE))

        fixture.viewModel.onAction(ExploreAction.CategorySelected(QuestCategoryUi.CREATE))

        assertNull(fixture.viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun categoryClearedResetsToAllCategories() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(ExploreAction.CategorySelected(QuestCategoryUi.CREATE))

        fixture.viewModel.onAction(ExploreAction.CategoryCleared)

        assertNull(fixture.viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun saveClickedTogglesSavedState() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(ExploreAction.SaveClicked(QuestId("quest-1")))
        testDispatcher.scheduler.advanceUntilIdle()

        val saved = fixture.viewModel.uiState.value.quests.first { it.id == QuestId("quest-1") }
        assertTrue(saved.isSaved)

        fixture.viewModel.onAction(ExploreAction.SaveClicked(QuestId("quest-1")))
        testDispatcher.scheduler.advanceUntilIdle()

        val unsaved = fixture.viewModel.uiState.value.quests.first { it.id == QuestId("quest-1") }
        assertFalse(unsaved.isSaved)
    }

    @Test
    fun packClickedEmitsOpenPackDetailsEvent() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(ExploreAction.PackClicked(QuestPackId("pack-1")))
            assertEquals(ExploreEvent.OpenPackDetails(QuestPackId("pack-1")), awaitItem())
        }
    }

    @Test
    fun questClickedOnAFreeQuestEmitsOpenQuestDetailEvent() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(ExploreAction.QuestClicked(QuestId("quest-1")))
            assertEquals(ExploreEvent.OpenQuestDetail(QuestId("quest-1")), awaitItem())
        }
    }

    @Test
    fun questClickedOnALockedPremiumQuestStillEmitsOpenQuestDetailEvent() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fixture.viewModel.uiState.value.quests.first { it.id == QuestId("quest-2") }.locked)

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(ExploreAction.QuestClicked(QuestId("quest-2")))
            assertEquals(ExploreEvent.OpenQuestDetail(QuestId("quest-2")), awaitItem())
        }
    }

    @Test
    fun filtersClickedEmitsOpenFiltersEvent() = runTest {
        val fixture = readyFixture()

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(ExploreAction.FiltersClicked)
            assertEquals(ExploreEvent.OpenFilters, awaitItem())
        }
    }

    @Test
    fun accessUpgradeWhileScreenIsVisibleUnlocksThePremiumQuestReactively() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fixture.viewModel.uiState.value.quests.first { it.id == QuestId("quest-2") }.locked)

        fixture.entitlementRepository.setAccess(AccessSnapshot(FamilyAccess.lifetime(), setOf(EntitlementId(FAMILY_PLUS)), NOW))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertTrue(state.access.isPlus)
        assertFalse(state.quests.first { it.id == QuestId("quest-2") }.locked)
    }

    @Test
    fun retryReloadsAfterAnEarlierFailure() = runTest {
        val fixture = ExploreFixture()
        fixture.questRepository.setQuestsError(AppError.Content(ContentError.CATALOGUE_UNAVAILABLE))
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fixture.viewModel.uiState.value.error != null)

        val quest = validFamilyQuest(id = QuestId("quest-1"))
        fixture.questRepository.setQuests(listOf(quest))
        fixture.viewModel.onAction(ExploreAction.RetryClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(1, state.quests.size)
    }

    @Test
    fun duplicateScreenStartedDoesNotDuplicateLoad() = runTest {
        val fixture = readyFixture()

        fixture.viewModel.onScreenStarted()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, fixture.viewModel.uiState.value.quests.size)
    }

    // -- Analytics --------------------------------------------------------------------------

    @Test
    fun screenStartedCapturesTheExploreScreenExactlyOnce() = runTest {
        val fixture = readyFixture()

        fixture.viewModel.onScreenStarted()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(AnalyticsScreen.EXPLORE), fixture.analytics.screensViewed)
    }

    @Test
    fun searchBecomingActiveCapturesExploreSearchedExactlyOnce() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(ExploreAction.SearchChanged("d"))
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(ExploreAction.SearchChanged("dr"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fixture.analytics.capturedEvents.count { it is ExploreSearched })
    }

    @Test
    fun clearingThenRetypingCapturesExploreSearchedAgain() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(ExploreAction.SearchChanged("draw"))
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(ExploreAction.SearchCleared)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(ExploreAction.SearchChanged("hunt"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, fixture.analytics.capturedEvents.count { it is ExploreSearched })
    }

    @Test
    fun searchedEventNeverCarriesTheQueryTextItself() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(ExploreAction.SearchChanged("Backyard Scavenger Hunt"))
        testDispatcher.scheduler.advanceUntilIdle()

        val event = fixture.analytics.capturedEvents.single { it is ExploreSearched }
        assertTrue(event.properties().isEmpty())
    }

    @Test
    fun categorySelectedCapturesExploreFilteredWithThatCategory() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(ExploreAction.CategorySelected(QuestCategoryUi.CREATE))
        testDispatcher.scheduler.advanceUntilIdle()

        val event = fixture.analytics.capturedEvents.single() as ExploreFiltered
        assertEquals(QuestCategory.CREATE, event.category)
    }

    @Test
    fun categoryClearedCapturesExploreFilteredWithNoCategory() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(ExploreAction.CategorySelected(QuestCategoryUi.CREATE))
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(ExploreAction.CategoryCleared)

        val event = fixture.analytics.capturedEvents.last() as ExploreFiltered
        assertEquals(null, event.category)
    }

    @Test
    fun successfulSaveCapturesQuestSavedWithTheNewState() = runTest {
        val fixture = readyFixture()
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(ExploreAction.SaveClicked(QuestId("quest-1")))
        testDispatcher.scheduler.advanceUntilIdle()

        val event = fixture.analytics.capturedEvents.single() as QuestSaved
        assertEquals(QuestId("quest-1"), event.questId)
        assertTrue(event.isSaved)
    }

    @Test
    fun noEventsAreCapturedWithoutConsent() = runTest {
        val fixture = readyFixture()
        fixture.analytics.setCollectionEnabled(false)

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(ExploreAction.SearchChanged("draw"))
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(ExploreAction.CategorySelected(QuestCategoryUi.CREATE))
        fixture.viewModel.onAction(ExploreAction.SaveClicked(QuestId("quest-1")))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fixture.analytics.screensViewed.isEmpty())
        assertTrue(fixture.analytics.capturedEvents.isEmpty())
    }
}

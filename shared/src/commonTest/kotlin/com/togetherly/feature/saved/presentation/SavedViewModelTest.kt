package com.togetherly.feature.saved.presentation

import app.cash.turbine.test
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.domain.explore.usecase.EvaluateQuestAccessUseCase
import com.togetherly.domain.explore.usecase.ObserveSavedQuestsUseCase
import com.togetherly.domain.explore.usecase.ToggleSavedQuestUseCase
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.validFamilyQuest
import com.togetherly.domain.saved.SavedQuest
import com.togetherly.domain.saved.repository.FakeSavedQuestRepository
import com.togetherly.domain.saved.usecase.SetQuestSaved
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
import kotlin.test.assertTrue
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")
private const val FAMILY_PLUS = "family_plus"

private class SavedFixture(
    val questRepository: FakeQuestRepository = FakeQuestRepository(),
    val savedQuestRepository: FakeSavedQuestRepository = FakeSavedQuestRepository(),
    val entitlementRepository: FakeEntitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)),
    val questAccessPolicy: QuestAccessPolicy = QuestAccessPolicy(),
    val clock: TestAppClock = TestAppClock(NOW),
) {
    private val setQuestSaved = SetQuestSaved(savedQuestRepository, questRepository, clock)

    val viewModel = SavedViewModel(
        ObserveSavedQuestsUseCase(savedQuestRepository, questRepository),
        ToggleSavedQuestUseCase(savedQuestRepository, setQuestSaved),
        EvaluateQuestAccessUseCase(entitlementRepository, questAccessPolicy, clock),
        entitlementRepository,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class SavedViewModelTest {

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
    fun emptySavedListShowsNoQuests() = runTest {
        val fixture = SavedFixture()

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.quests.isEmpty())
    }

    @Test
    fun savedListUpdatesReactivelyWhenAQuestIsSaved() = runTest {
        val fixture = SavedFixture()
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        fixture.questRepository.setQuests(listOf(quest))
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fixture.viewModel.uiState.value.quests.isEmpty())

        fixture.savedQuestRepository.save(SavedQuest(QuestId("quest-1"), NOW))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertEquals(1, state.quests.size)
        assertTrue(state.quests.single().isSaved)
    }

    @Test
    fun aPremiumQuestCanBeSavedByAFreeFamily() = runTest {
        val fixture = SavedFixture()
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)))
        fixture.questRepository.setQuests(listOf(quest))
        fixture.savedQuestRepository.save(SavedQuest(QuestId("quest-1"), NOW))

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertEquals(1, state.quests.size)
        val item = state.quests.single()
        assertTrue(item.isPremium)
        assertTrue(item.locked)
    }

    @Test
    fun unsavingRemovesTheQuestFromTheListImmediately() = runTest {
        val fixture = SavedFixture()
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        fixture.questRepository.setQuests(listOf(quest))
        fixture.savedQuestRepository.save(SavedQuest(QuestId("quest-1"), NOW))
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fixture.viewModel.uiState.value.quests.size)

        fixture.viewModel.onAction(SavedAction.SaveClicked(QuestId("quest-1")))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fixture.viewModel.uiState.value.quests.isEmpty())
    }

    @Test
    fun entitlementActivationWhileSavedPremiumContentIsVisibleUnlocksItReactively() = runTest {
        val fixture = SavedFixture()
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)))
        fixture.questRepository.setQuests(listOf(quest))
        fixture.savedQuestRepository.save(SavedQuest(QuestId("quest-1"), NOW))
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fixture.viewModel.uiState.value.quests.single().locked)

        fixture.entitlementRepository.setAccess(AccessSnapshot(FamilyAccess.lifetime(), setOf(EntitlementId(FAMILY_PLUS)), NOW))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(fixture.viewModel.uiState.value.quests.single().locked)
    }

    @Test
    fun questClickedEmitsOpenQuestDetailEvent() = runTest {
        val fixture = SavedFixture()

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(SavedAction.QuestClicked(QuestId("quest-1")))
            assertEquals(SavedEvent.OpenQuestDetail(QuestId("quest-1")), awaitItem())
        }
    }

    @Test
    fun backClickedEmitsNavigateBackEvent() = runTest {
        val fixture = SavedFixture()

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(SavedAction.BackClicked)
            assertEquals(SavedEvent.NavigateBack, awaitItem())
        }
    }
}

package com.togetherly.feature.packdetails.presentation

import app.cash.turbine.test
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.telemetry.FakeProductAnalytics
import com.togetherly.core.telemetry.PackViewed
import com.togetherly.core.telemetry.PremiumContentUnlocked
import com.togetherly.core.telemetry.PremiumContentViewed
import com.togetherly.core.telemetry.QuestSaved
import com.togetherly.domain.explore.usecase.EvaluatePackAccessUseCase
import com.togetherly.domain.explore.usecase.EvaluateQuestAccessUseCase
import com.togetherly.domain.explore.usecase.GetQuestPackUseCase
import com.togetherly.domain.explore.usecase.ObserveSavedQuestIdsUseCase
import com.togetherly.domain.explore.usecase.ToggleSavedQuestUseCase
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.validFamilyQuest
import com.togetherly.domain.quest.validQuestPack
import com.togetherly.domain.saved.repository.FakeSavedQuestRepository
import com.togetherly.domain.saved.usecase.SetQuestSaved
import com.togetherly.feature.packdetails.model.ContentAccessState
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")
private const val FAMILY_PLUS = "family_plus"

private class PackDetailsFixture(
    val packId: QuestPackId,
    val questRepository: FakeQuestRepository = FakeQuestRepository(),
    val savedQuestRepository: FakeSavedQuestRepository = FakeSavedQuestRepository(),
    val entitlementRepository: FakeEntitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)),
    val questAccessPolicy: QuestAccessPolicy = QuestAccessPolicy(),
    val clock: TestAppClock = TestAppClock(NOW),
    val analytics: FakeProductAnalytics = FakeProductAnalytics().apply { setCollectionEnabled(true) },
) {
    private val setQuestSaved = SetQuestSaved(savedQuestRepository, questRepository, clock)

    val viewModel = PackDetailsViewModel(
        packId,
        GetQuestPackUseCase(questRepository),
        ObserveSavedQuestIdsUseCase(savedQuestRepository),
        ToggleSavedQuestUseCase(savedQuestRepository, setQuestSaved),
        EvaluateQuestAccessUseCase(entitlementRepository, questAccessPolicy, clock),
        EvaluatePackAccessUseCase(entitlementRepository, questAccessPolicy, clock),
        entitlementRepository,
        analytics,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class PackDetailsViewModelTest {

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
    fun validFreePackLoadsWithFreeAccessState() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), packId = QuestPackId("pack-1"))
        val pack = validQuestPack(id = QuestPackId("pack-1"), questIds = listOf(quest.id))
        val fixture = PackDetailsFixture(packId = pack.id)
        fixture.questRepository.setQuests(listOf(quest))
        fixture.questRepository.setPacks(listOf(pack))

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.pack)
        assertEquals(1, state.quests.size)
        assertEquals(ContentAccessState.FREE, state.accessState)
        assertNull(state.error)
    }

    @Test
    fun invalidPackIdProducesARecoverableError() = runTest {
        val fixture = PackDetailsFixture(packId = QuestPackId("missing"))

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.pack)
        assertNotNull(state.error)
    }

    @Test
    fun lockedPremiumPackShowsLockedAccessState() = runTest {
        val quest = validFamilyQuest(
            id = QuestId("quest-1"),
            packId = QuestPackId("pack-1"),
            access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)),
        )
        val pack = validQuestPack(
            id = QuestPackId("pack-1"),
            questIds = listOf(quest.id),
            access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)),
        )
        val fixture = PackDetailsFixture(packId = pack.id)
        fixture.questRepository.setQuests(listOf(quest))
        fixture.questRepository.setPacks(listOf(pack))

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertEquals(ContentAccessState.LOCKED, state.accessState)
        assertTrue(state.pack!!.locked)
        assertTrue(state.quests.single().locked)
    }

    @Test
    fun premiumPackBecomesUnlockedAfterEntitlementActivation() = runTest {
        val quest = validFamilyQuest(
            id = QuestId("quest-1"),
            packId = QuestPackId("pack-1"),
            access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)),
        )
        val pack = validQuestPack(
            id = QuestPackId("pack-1"),
            questIds = listOf(quest.id),
            access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)),
        )
        val fixture = PackDetailsFixture(packId = pack.id)
        fixture.questRepository.setQuests(listOf(quest))
        fixture.questRepository.setPacks(listOf(pack))
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ContentAccessState.LOCKED, fixture.viewModel.uiState.value.accessState)

        fixture.entitlementRepository.setAccess(AccessSnapshot(FamilyAccess.lifetime(), setOf(EntitlementId(FAMILY_PLUS)), NOW))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertEquals(ContentAccessState.UNLOCKED, state.accessState)
        assertFalse(state.pack!!.locked)
        assertFalse(state.quests.single().locked)
    }

    @Test
    fun saveClickedTogglesSavedState() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), packId = QuestPackId("pack-1"))
        val pack = validQuestPack(id = QuestPackId("pack-1"), questIds = listOf(quest.id))
        val fixture = PackDetailsFixture(packId = pack.id)
        fixture.questRepository.setQuests(listOf(quest))
        fixture.questRepository.setPacks(listOf(pack))
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(PackDetailsAction.SaveClicked(QuestId("quest-1")))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fixture.viewModel.uiState.value.quests.single().isSaved)
    }

    @Test
    fun questClickedEmitsOpenQuestDetailEvent() = runTest {
        val fixture = PackDetailsFixture(packId = QuestPackId("pack-1"))

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(PackDetailsAction.QuestClicked(QuestId("quest-1")))
            assertEquals(PackDetailsEvent.OpenQuestDetail(QuestId("quest-1")), awaitItem())
        }
    }

    @Test
    fun unlockClickedEmitsOpenPaywallWithThisPacksId() = runTest {
        val fixture = PackDetailsFixture(packId = QuestPackId("pack-1"))

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(PackDetailsAction.UnlockClicked)
            assertEquals(PackDetailsEvent.OpenPaywall(QuestPackId("pack-1")), awaitItem())
        }
    }

    @Test
    fun backClickedEmitsNavigateBackEvent() = runTest {
        val fixture = PackDetailsFixture(packId = QuestPackId("pack-1"))

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(PackDetailsAction.BackClicked)
            assertEquals(PackDetailsEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun retryReloadsAfterAnEarlierFailure() = runTest {
        val fixture = PackDetailsFixture(packId = QuestPackId("pack-1"))
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(fixture.viewModel.uiState.value.error)

        val quest = validFamilyQuest(id = QuestId("quest-1"), packId = QuestPackId("pack-1"))
        val pack = validQuestPack(id = QuestPackId("pack-1"), questIds = listOf(quest.id))
        fixture.questRepository.setQuests(listOf(quest))
        fixture.questRepository.setPacks(listOf(pack))

        fixture.viewModel.onAction(PackDetailsAction.RetryClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertNull(state.error)
        assertNotNull(state.pack)
    }

    // -- Analytics --------------------------------------------------------------------------

    @Test
    fun loadingAFreePackCapturesPackViewedWithoutPremiumContentViewed() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), packId = QuestPackId("pack-1"))
        val pack = validQuestPack(id = QuestPackId("pack-1"), questIds = listOf(quest.id))
        val fixture = PackDetailsFixture(packId = pack.id)
        fixture.questRepository.setQuests(listOf(quest))
        fixture.questRepository.setPacks(listOf(pack))

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        val event = fixture.analytics.capturedEvents.single { it is PackViewed } as PackViewed
        assertEquals(pack.id, event.packId)
        assertEquals(QuestAccess.Free, event.accessRequired)
        assertTrue(fixture.analytics.capturedEvents.none { it is PremiumContentViewed })
    }

    @Test
    fun loadingALockedPremiumPackAlsoCapturesPremiumContentViewed() = runTest {
        val quest = validFamilyQuest(
            id = QuestId("quest-1"),
            packId = QuestPackId("pack-1"),
            access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)),
        )
        val pack = validQuestPack(
            id = QuestPackId("pack-1"),
            questIds = listOf(quest.id),
            access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)),
        )
        val fixture = PackDetailsFixture(packId = pack.id)
        fixture.questRepository.setQuests(listOf(quest))
        fixture.questRepository.setPacks(listOf(pack))

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        val event = fixture.analytics.capturedEvents.single { it is PremiumContentViewed } as PremiumContentViewed
        assertEquals(pack.id, event.packId)
        assertEquals(null, event.questId)
    }

    @Test
    fun invalidPackIdCapturesNoViewedEvent() = runTest {
        val fixture = PackDetailsFixture(packId = QuestPackId("missing"))

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fixture.analytics.capturedEvents.isEmpty())
    }

    @Test
    fun successfulSaveCapturesQuestSavedWithTheNewState() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), packId = QuestPackId("pack-1"))
        val pack = validQuestPack(id = QuestPackId("pack-1"), questIds = listOf(quest.id))
        val fixture = PackDetailsFixture(packId = pack.id)
        fixture.questRepository.setQuests(listOf(quest))
        fixture.questRepository.setPacks(listOf(pack))
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        val eventCountBefore = fixture.analytics.capturedEvents.size

        fixture.viewModel.onAction(PackDetailsAction.SaveClicked(QuestId("quest-1")))
        testDispatcher.scheduler.advanceUntilIdle()

        val event = fixture.analytics.capturedEvents.drop(eventCountBefore).single() as QuestSaved
        assertEquals(QuestId("quest-1"), event.questId)
        assertTrue(event.isSaved)
    }

    @Test
    fun noEventsAreCapturedWithoutConsent() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), packId = QuestPackId("pack-1"))
        val pack = validQuestPack(id = QuestPackId("pack-1"), questIds = listOf(quest.id))
        val analytics = FakeProductAnalytics()
        val fixture = PackDetailsFixture(packId = pack.id, analytics = analytics)
        fixture.questRepository.setQuests(listOf(quest))
        fixture.questRepository.setPacks(listOf(pack))

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(PackDetailsAction.SaveClicked(QuestId("quest-1")))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(analytics.capturedEvents.isEmpty())
    }

    @Test
    fun entitlementActivationWhileScreenIsOpenCapturesPremiumContentUnlocked() = runTest {
        val quest = validFamilyQuest(
            id = QuestId("quest-1"),
            packId = QuestPackId("pack-1"),
            access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)),
        )
        val pack = validQuestPack(
            id = QuestPackId("pack-1"),
            questIds = listOf(quest.id),
            access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)),
        )
        val fixture = PackDetailsFixture(packId = pack.id)
        fixture.questRepository.setQuests(listOf(quest))
        fixture.questRepository.setPacks(listOf(pack))
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.analytics.capturedEvents.clear()

        fixture.entitlementRepository.setAccess(AccessSnapshot(FamilyAccess.lifetime(), setOf(EntitlementId(FAMILY_PLUS)), NOW))
        testDispatcher.scheduler.advanceUntilIdle()

        val event = fixture.analytics.capturedEvents.single() as PremiumContentUnlocked
        assertEquals(pack.id, event.packId)
        assertEquals(null, event.questId)
    }

    @Test
    fun freePackNeverCapturesPremiumContentUnlocked() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), packId = QuestPackId("pack-1"))
        val pack = validQuestPack(id = QuestPackId("pack-1"), questIds = listOf(quest.id))
        val fixture = PackDetailsFixture(packId = pack.id)
        fixture.questRepository.setQuests(listOf(quest))
        fixture.questRepository.setPacks(listOf(pack))
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.analytics.capturedEvents.clear()

        fixture.entitlementRepository.setAccess(AccessSnapshot(FamilyAccess.lifetime(), setOf(EntitlementId(FAMILY_PLUS)), NOW))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fixture.analytics.capturedEvents.none { it is PremiumContentUnlocked })
    }
}

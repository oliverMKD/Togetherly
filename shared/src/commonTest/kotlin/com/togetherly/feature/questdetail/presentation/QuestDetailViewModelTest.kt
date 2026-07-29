package com.togetherly.feature.questdetail.presentation

import app.cash.turbine.test
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.id.SequentialIdGenerator
import com.togetherly.core.telemetry.FakeProductAnalytics
import com.togetherly.core.telemetry.PremiumContentUnlocked
import com.togetherly.core.telemetry.PremiumContentViewed
import com.togetherly.core.telemetry.QuestSaved
import com.togetherly.core.telemetry.QuestStarted
import com.togetherly.core.telemetry.QuestViewed
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.repository.FakeQuestSessionTransaction
import com.togetherly.domain.completion.usecase.PrepareQuestStartUseCase
import com.togetherly.domain.completion.usecase.ReplaceActiveQuestSession
import com.togetherly.domain.completion.usecase.StartQuest
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
import com.togetherly.domain.saved.repository.FakeSavedQuestRepository
import com.togetherly.domain.saved.usecase.SetQuestSaved
import com.togetherly.feature.questdetail.model.QuestDetailUiState
import com.togetherly.feature.questdetail.model.QuestOpenSource
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

private class Fixture(
    val questId: QuestId,
    val questRepository: FakeQuestRepository,
    val savedQuestRepository: FakeSavedQuestRepository,
    val familyRepository: FakeFamilyRepository,
    val completionRepository: FakeCompletionRepository,
    val entitlementRepository: FakeEntitlementRepository,
    val source: QuestOpenSource = QuestOpenSource.TODAY,
    val analytics: FakeProductAnalytics = FakeProductAnalytics().apply { setCollectionEnabled(true) },
) {
    val questAccessPolicy = QuestAccessPolicy()
    val transaction = FakeQuestSessionTransaction(completionRepository)
    val startQuest = StartQuest(
        familyRepository, questRepository, transaction, entitlementRepository, questAccessPolicy, TestAppClock(NOW), SequentialIdGenerator("completion"),
    )
    val prepareQuestStart = PrepareQuestStartUseCase(questRepository, entitlementRepository, questAccessPolicy, TestAppClock(NOW))
    val replaceActiveQuestSession = ReplaceActiveQuestSession(familyRepository, questRepository, transaction, TestAppClock(NOW), SequentialIdGenerator("completion"))
    val setQuestSaved = SetQuestSaved(savedQuestRepository, questRepository, TestAppClock(NOW))
    val viewModel = QuestDetailViewModel(
        questId, source, questRepository, savedQuestRepository, setQuestSaved, startQuest, prepareQuestStart, replaceActiveQuestSession,
        entitlementRepository, questAccessPolicy, TestAppClock(NOW), analytics,
    )
}

private suspend fun fixture(
    questId: QuestId = QuestId("quest-1"),
    questRepository: FakeQuestRepository = FakeQuestRepository(),
    savedQuestRepository: FakeSavedQuestRepository = FakeSavedQuestRepository(),
    completionRepository: FakeCompletionRepository = FakeCompletionRepository(),
    entitlementRepository: FakeEntitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)),
    source: QuestOpenSource = QuestOpenSource.TODAY,
    analytics: FakeProductAnalytics = FakeProductAnalytics().apply { setCollectionEnabled(true) },
): Fixture {
    val familyRepository = FakeFamilyRepository().apply { saveProfile(validProfile()) }
    return Fixture(questId, questRepository, savedQuestRepository, familyRepository, completionRepository, entitlementRepository, source, analytics)
}

@OptIn(ExperimentalCoroutinesApi::class)
class QuestDetailViewModelTest {

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
    fun loadsAKnownQuest() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val fixture = fixture(questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) })

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value as QuestDetailUiState.Content
        assertEquals(quest.id, content.quest.id)
        assertFalse(content.isSaved)
        assertFalse(content.isStarting)
    }

    @Test
    fun unknownQuestReturnsTypedError() = runTest {
        val fixture = fixture(questRepository = FakeQuestRepository())

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertTrue(state is QuestDetailUiState.Error)
    }

    @Test
    fun instructionsRemainOrdered() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val fixture = fixture(questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) })

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value as QuestDetailUiState.Content
        val expected = quest.instructions.sortedBy { it.order }.map { it.text.value }
        assertEquals(expected, content.quest.instructions)
    }

    @Test
    fun saveTogglesDesiredState() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val fixture = fixture(questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) })
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(QuestDetailAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value as QuestDetailUiState.Content
        assertTrue(content.isSaved)
    }

    @Test
    fun startLoadingPreventsDuplicateAction() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val fixture = fixture(questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) })
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(QuestDetailAction.StartClicked)
        fixture.viewModel.onAction(QuestDetailAction.StartClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val activeSessionResult = fixture.completionRepository.getActiveSession()
        val activeSession = (activeSessionResult as com.togetherly.core.result.DataResult.Success).value
        assertEquals(quest.id, activeSession?.questId)
    }

    @Test
    fun startSuccessNavigatesToQuestMode() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val fixture = fixture(questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) })
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.events.test {
            fixture.viewModel.onAction(QuestDetailAction.StartClicked)
            assertTrue(awaitItem() is QuestDetailEvent.NavigatedToQuestMode)
        }
    }

    @Test
    fun saveFailureRevertsAndDoesNotNavigate() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val savedQuestRepository = FakeSavedQuestRepository()
        val fixture = fixture(
            questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) },
            savedQuestRepository = savedQuestRepository,
        )
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        savedQuestRepository.setNextError(AppError.Storage(com.togetherly.core.error.StorageError.WRITE_FAILED))
        fixture.viewModel.onAction(QuestDetailAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value as QuestDetailUiState.Content
        assertFalse(content.isSaved)
    }

    @Test
    fun existingActiveSessionShowsConflict() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-2"))
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(validActiveQuestSession()) }
        val fixture = fixture(
            questId = quest.id,
            questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) },
            completionRepository = completionRepository,
        )
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(QuestDetailAction.StartClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value as QuestDetailUiState.Content
        assertTrue(content.activeSessionConflict)
    }

    @Test
    fun cancellingConflictPreservesTheExistingSession() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-2"))
        val previousSession = validActiveQuestSession()
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(previousSession) }
        val fixture = fixture(
            questId = quest.id,
            questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) },
            completionRepository = completionRepository,
        )
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(QuestDetailAction.StartClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(QuestDetailAction.KeepActiveSessionClicked)

        val content = fixture.viewModel.uiState.value as QuestDetailUiState.Content
        assertFalse(content.activeSessionConflict)
        val activeSessionResult = fixture.completionRepository.getActiveSession()
        val activeSession = (activeSessionResult as com.togetherly.core.result.DataResult.Success).value
        assertEquals(previousSession, activeSession)
    }

    @Test
    fun replacingUsesTheExplicitReplaceTransaction() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-2"))
        val previousSession = validActiveQuestSession()
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(previousSession) }
        val fixture = fixture(
            questId = quest.id,
            questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) },
            completionRepository = completionRepository,
        )
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(QuestDetailAction.StartClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(QuestDetailAction.ReplaceActiveSessionConfirmed)
        testDispatcher.scheduler.advanceUntilIdle()

        val activeSessionResult = fixture.completionRepository.getActiveSession()
        val activeSession = (activeSessionResult as com.togetherly.core.result.DataResult.Success).value
        assertEquals(quest.id, activeSession?.questId)
        assertTrue(activeSession != previousSession)
    }

    @Test
    fun freeQuestIsNeverLockedForAFreeFamily() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Free)
        val fixture = fixture(questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) })

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value as QuestDetailUiState.Content
        assertFalse(content.locked)
    }

    @Test
    fun premiumQuestIsLockedForAFreeFamily() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)))
        val fixture = fixture(questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) })

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value as QuestDetailUiState.Content
        assertTrue(content.locked)
    }

    @Test
    fun premiumQuestIsUnlockedForAPremiumFamily() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)))
        val premiumAccess = AccessSnapshot(
            familyAccess = FamilyAccess.lifetime(),
            activeEntitlements = setOf(EntitlementId(FAMILY_PLUS)),
            verifiedAt = NOW,
        )
        val fixture = fixture(
            questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) },
            entitlementRepository = FakeEntitlementRepository(premiumAccess),
        )

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value as QuestDetailUiState.Content
        assertFalse(content.locked)
    }

    @Test
    fun lockedStateUpdatesLiveWhenEntitlementChangesWhileScreenIsOpen() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)))
        val entitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val fixture = fixture(
            questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) },
            entitlementRepository = entitlementRepository,
        )
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue((fixture.viewModel.uiState.value as QuestDetailUiState.Content).locked)

        entitlementRepository.setAccess(
            AccessSnapshot(FamilyAccess.lifetime(), setOf(EntitlementId(FAMILY_PLUS)), NOW),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse((fixture.viewModel.uiState.value as QuestDetailUiState.Content).locked)
    }

    @Test
    fun lockedPremiumPreviewNeverExposesInstructionsOrHints() = runTest {
        val quest = validFamilyQuest(
            id = QuestId("quest-1"),
            access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)),
        )
        val fixture = fixture(questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) })

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value as QuestDetailUiState.Content
        assertTrue(content.locked)
        assertTrue(content.quest.instructions.isEmpty())
        assertTrue(content.quest.hints.isEmpty())
        assertTrue(content.quest.isPremium)
    }

    @Test
    fun startClickedOnALockedQuestNeverCreatesASessionOrNavigatesToQuestMode() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)))
        val fixture = fixture(questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) })
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue((fixture.viewModel.uiState.value as QuestDetailUiState.Content).locked)

        // The real UI never renders a "Start" action while locked (only "Unlock"), but this
        // asserts the ViewModel itself refuses to act on StartClicked regardless — "Do not rely
        // only on a disabled button" (this feature's own task spec).
        fixture.viewModel.events.test {
            fixture.viewModel.onAction(QuestDetailAction.StartClicked)
            expectNoEvents()
        }
        val activeSessionResult = fixture.completionRepository.getActiveSession()
        assertEquals(null, (activeSessionResult as com.togetherly.core.result.DataResult.Success).value)
    }

    @Test
    fun prepareQuestStartRejectsAPremiumQuestEvenIfCalledDirectly() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)))
        val fixture = fixture(questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) })

        val result = fixture.prepareQuestStart(quest.id)

        assertEquals(com.togetherly.domain.completion.usecase.PrepareQuestStartResult.RequiresFamilyPlus(quest.id), result)
    }

    @Test
    fun entitlementActivationRevealsFullInstructionsWithoutAutoStarting() = runTest {
        val quest = validFamilyQuest(
            id = QuestId("quest-1"),
            access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)),
            instructions = listOf(com.togetherly.domain.quest.InstructionStep(1, com.togetherly.domain.quest.InstructionText("Do the thing."))),
        )
        val entitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val fixture = fixture(
            questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) },
            entitlementRepository = entitlementRepository,
        )
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue((fixture.viewModel.uiState.value as QuestDetailUiState.Content).quest.instructions.isEmpty())

        entitlementRepository.setAccess(AccessSnapshot(FamilyAccess.lifetime(), setOf(EntitlementId(FAMILY_PLUS)), NOW))
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value as QuestDetailUiState.Content
        assertFalse(content.locked)
        assertEquals(listOf("Do the thing."), content.quest.instructions)
        // Purchase success never auto-starts — no active session exists purely from the entitlement flip.
        val activeSessionResult = fixture.completionRepository.getActiveSession()
        assertEquals(null, (activeSessionResult as com.togetherly.core.result.DataResult.Success).value)
    }

    @Test
    fun savingALockedPremiumQuestStillWorks() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)))
        val fixture = fixture(questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) })
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        fixture.viewModel.onAction(QuestDetailAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = fixture.viewModel.uiState.value as QuestDetailUiState.Content
        assertTrue(content.isSaved)
    }

    // -- Analytics --------------------------------------------------------------------------

    @Test
    fun loadingAKnownQuestCapturesQuestViewedWithTheCorrectSource() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Free)
        val fixture = fixture(
            questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) },
            source = QuestOpenSource.EXPLORE,
        )

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        val event = fixture.analytics.capturedEvents.single { it is QuestViewed } as QuestViewed
        assertEquals(quest.id, event.questId)
        assertEquals(QuestOpenSource.EXPLORE, event.source)
        assertEquals(quest.category, event.category)
        assertEquals(QuestAccess.Free, event.accessRequired)
        assertTrue(fixture.analytics.capturedEvents.none { it is PremiumContentViewed })
    }

    @Test
    fun loadingALockedPremiumQuestAlsoCapturesPremiumContentViewed() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)))
        val fixture = fixture(questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) })

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        val event = fixture.analytics.capturedEvents.single { it is PremiumContentViewed } as PremiumContentViewed
        assertEquals(quest.id, event.questId)
        assertEquals(null, event.packId)
    }

    @Test
    fun unknownQuestCapturesNoViewedEvent() = runTest {
        val fixture = fixture(questRepository = FakeQuestRepository())

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fixture.analytics.capturedEvents.isEmpty())
    }

    @Test
    fun successfulSaveCapturesQuestSavedWithTheNewState() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val fixture = fixture(questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) })
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.analytics.capturedEvents.clear()

        fixture.viewModel.onAction(QuestDetailAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = fixture.analytics.capturedEvents.single() as QuestSaved
        assertEquals(quest.id, event.questId)
        assertTrue(event.isSaved)
    }

    @Test
    fun failedSaveCapturesNoQuestSavedEvent() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val savedQuestRepository = FakeSavedQuestRepository()
        val fixture = fixture(
            questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) },
            savedQuestRepository = savedQuestRepository,
        )
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.analytics.capturedEvents.clear()

        savedQuestRepository.setNextError(AppError.Storage(com.togetherly.core.error.StorageError.WRITE_FAILED))
        fixture.viewModel.onAction(QuestDetailAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fixture.analytics.capturedEvents.none { it is QuestSaved })
    }

    @Test
    fun successfulStartCapturesQuestStartedWithTheOriginalSource() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Free)
        val fixture = fixture(
            questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) },
            source = QuestOpenSource.SAVED,
        )
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.analytics.capturedEvents.clear()

        fixture.viewModel.onAction(QuestDetailAction.StartClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = fixture.analytics.capturedEvents.single() as QuestStarted
        assertEquals(quest.id, event.questId)
        assertEquals(QuestOpenSource.SAVED, event.source)
    }

    @Test
    fun lockedQuestNeverCapturesQuestStarted() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)))
        val fixture = fixture(questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) })
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.analytics.capturedEvents.clear()

        fixture.viewModel.onAction(QuestDetailAction.StartClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fixture.analytics.capturedEvents.none { it is QuestStarted })
    }

    @Test
    fun replacingAnActiveSessionAlsoCapturesQuestStarted() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-2"))
        val previousSession = validActiveQuestSession()
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(previousSession) }
        val fixture = fixture(
            questId = quest.id,
            questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) },
            completionRepository = completionRepository,
        )
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(QuestDetailAction.StartClicked)
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.analytics.capturedEvents.clear()

        fixture.viewModel.onAction(QuestDetailAction.ReplaceActiveSessionConfirmed)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fixture.analytics.capturedEvents.count { it is QuestStarted })
    }

    @Test
    fun noEventsAreCapturedWithoutConsent() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val analytics = FakeProductAnalytics()
        val fixture = fixture(questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }, analytics = analytics)

        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.viewModel.onAction(QuestDetailAction.SaveClicked)
        fixture.viewModel.onAction(QuestDetailAction.StartClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(analytics.capturedEvents.isEmpty())
    }

    @Test
    fun entitlementActivationWhileScreenIsOpenCapturesPremiumContentUnlocked() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)))
        val entitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val fixture = fixture(
            questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) },
            entitlementRepository = entitlementRepository,
        )
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.analytics.capturedEvents.clear()

        entitlementRepository.setAccess(AccessSnapshot(FamilyAccess.lifetime(), setOf(EntitlementId(FAMILY_PLUS)), NOW))
        testDispatcher.scheduler.advanceUntilIdle()

        val event = fixture.analytics.capturedEvents.single() as PremiumContentUnlocked
        assertEquals(quest.id, event.questId)
        assertEquals(null, event.packId)
    }

    @Test
    fun freeQuestNeverCapturesPremiumContentUnlocked() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), access = QuestAccess.Free)
        val fixture = fixture(questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) })
        fixture.viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        fixture.analytics.capturedEvents.clear()

        fixture.entitlementRepository.setAccess(AccessSnapshot(FamilyAccess.lifetime(), setOf(EntitlementId(FAMILY_PLUS)), NOW))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fixture.analytics.capturedEvents.none { it is PremiumContentUnlocked })
    }
}

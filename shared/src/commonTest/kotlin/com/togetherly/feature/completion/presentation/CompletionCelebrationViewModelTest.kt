package com.togetherly.feature.completion.presentation

import app.cash.turbine.test
import com.togetherly.core.datetime.AppTimeZoneProvider
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.FakeProductAnalytics
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.validQuestCompletion
import com.togetherly.domain.family.repository.FakeFamilySettingsRepository
import com.togetherly.domain.family.testFamilySettings
import com.togetherly.domain.family.usecase.ObserveFamilySettings
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.validFamilyQuest
import com.togetherly.feature.completion.model.CompletionCelebrationUiState
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private val UTC_PROVIDER = object : AppTimeZoneProvider {
    override fun current(): TimeZone = TimeZone.UTC
}

@OptIn(ExperimentalCoroutinesApi::class)
class CompletionCelebrationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        completionId: CompletionId = CompletionId("completion-1"),
        completionRepository: FakeCompletionRepository = FakeCompletionRepository(),
        questRepository: FakeQuestRepository = FakeQuestRepository(),
        familySettingsRepository: FakeFamilySettingsRepository = FakeFamilySettingsRepository(),
        analytics: FakeProductAnalytics = FakeProductAnalytics().apply { setCollectionEnabled(true) },
    ) = CompletionCelebrationViewModel(
        completionId,
        completionRepository,
        questRepository,
        ObserveFamilySettings(familySettingsRepository),
        UTC_PROVIDER,
        analytics,
    )

    @Test
    fun loadsExistingCompletionWithQuestContent() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(
                validQuestCompletion(
                    id = CompletionId("completion-1"),
                    questId = QuestId("quest-1"),
                    completedAt = Instant.parse("2026-06-15T09:05:00Z"),
                ),
            )
        }
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }
        val model = viewModel(completionRepository = completionRepository, questRepository = questRepository)

        model.onAction(CompletionCelebrationAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = model.uiState.value as CompletionCelebrationUiState.Content
        assertEquals(quest.title.value, content.quest?.title)
    }

    @Test
    fun showAddMemoryPromptReflectsMemoryPreferences() = runTest {
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(validQuestCompletion(id = CompletionId("completion-1"), completedAt = Instant.parse("2026-06-15T09:05:00Z")))
        }
        val familySettingsRepository = FakeFamilySettingsRepository()
        familySettingsRepository.setSettings(
            testFamilySettings(
                profile = testFamilyProfile(),
                memoryPreferences = com.togetherly.domain.family.MemoryPreferences.defaults().copy(showMemoryPromptAfterQuests = false),
            ),
        )
        val model = viewModel(completionRepository = completionRepository, familySettingsRepository = familySettingsRepository)

        model.onAction(CompletionCelebrationAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = model.uiState.value as CompletionCelebrationUiState.Content
        assertFalse(content.showAddMemoryPrompt)
    }

    @Test
    fun missingQuestContentStillShowsSuccess() = runTest {
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(
                validQuestCompletion(id = CompletionId("completion-1"), questId = QuestId("quest-missing")),
            )
        }
        val model = viewModel(completionRepository = completionRepository)

        model.onAction(CompletionCelebrationAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = model.uiState.value as CompletionCelebrationUiState.Content
        assertNull(content.quest)
    }

    @Test
    fun missingCompletionShowsError() = runTest {
        val model = viewModel(completionRepository = FakeCompletionRepository())

        model.onAction(CompletionCelebrationAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val error = model.uiState.value as CompletionCelebrationUiState.Error
        assertEquals(AppError.Validation(ValidationError.COMPLETION_NOT_FOUND).toUiText(), error.message)
    }

    @Test
    fun repositoryFailureShowsError() = runTest {
        val completionRepository = FakeCompletionRepository()
        completionRepository.setNextError(AppError.Storage(StorageError.READ_FAILED))
        val model = viewModel(completionRepository = completionRepository)

        model.onAction(CompletionCelebrationAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val error = model.uiState.value as CompletionCelebrationUiState.Error
        assertEquals(AppError.Storage(StorageError.READ_FAILED).toUiText(), error.message)
    }

    @Test
    fun continueClickedEmitsNavigateToToday() = runTest {
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(validQuestCompletion(id = CompletionId("completion-1"), questId = QuestId("quest-1")))
        }
        val model = viewModel(completionRepository = completionRepository)
        model.onAction(CompletionCelebrationAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.events.test {
            model.onAction(CompletionCelebrationAction.ContinueClicked)
            assertEquals(CompletionCelebrationEvent.NavigateToToday, awaitItem())
        }
    }

    @Test
    fun addMemoryClickedEmitsNavigateToMemory() = runTest {
        val completionId = CompletionId("completion-1")
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(validQuestCompletion(id = completionId, questId = QuestId("quest-1")))
        }
        val model = viewModel(completionId = completionId, completionRepository = completionRepository)
        model.onAction(CompletionCelebrationAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.events.test {
            model.onAction(CompletionCelebrationAction.AddMemoryClicked)
            assertEquals(CompletionCelebrationEvent.NavigateToMemory(completionId), awaitItem())
        }
    }

    @Test
    fun screenStartedCapturesTheCompletionScreenExactlyOnce() = runTest {
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(validQuestCompletion(id = CompletionId("completion-1")))
        }
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(completionRepository = completionRepository, analytics = analytics)

        model.onAction(CompletionCelebrationAction.ScreenStarted)
        model.onAction(CompletionCelebrationAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(AnalyticsScreen.COMPLETION), analytics.screensViewed)
    }

    @Test
    fun noScreenViewIsCapturedWithoutConsent() = runTest {
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(validQuestCompletion(id = CompletionId("completion-1")))
        }
        val analytics = FakeProductAnalytics()
        val model = viewModel(completionRepository = completionRepository, analytics = analytics)

        model.onAction(CompletionCelebrationAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(analytics.screensViewed.isEmpty())
    }
}

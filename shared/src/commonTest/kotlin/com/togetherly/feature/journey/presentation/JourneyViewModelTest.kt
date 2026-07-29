package com.togetherly.feature.journey.presentation

import app.cash.turbine.test
import com.togetherly.core.datetime.AppTimeZoneProvider
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.FakeProductAnalytics
import com.togetherly.core.telemetry.MemoryDeleted
import com.togetherly.core.telemetry.MemoryOpened
import com.togetherly.core.ui.toUiText
import com.togetherly.data.media.FakeVoicePlaybackController
import com.togetherly.data.media.VoicePlaybackState
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.repository.FakePrivateMediaCommitter
import com.togetherly.domain.completion.usecase.DeleteCompletion
import com.togetherly.domain.journey.JourneyConstellationPolicy
import com.togetherly.domain.journey.JourneyEntry
import com.togetherly.domain.journey.repository.FakeJourneyRepository
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.validFamilyQuest
import com.togetherly.feature.journey.model.JourneyUiState
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val UTC_PROVIDER = object : AppTimeZoneProvider {
    override fun current(): TimeZone = TimeZone.UTC
}

@OptIn(ExperimentalCoroutinesApi::class)
class JourneyViewModelTest {

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
        journeyRepository: FakeJourneyRepository = FakeJourneyRepository(),
        voicePlaybackController: FakeVoicePlaybackController = FakeVoicePlaybackController(),
        completionRepository: FakeCompletionRepository = FakeCompletionRepository(),
        mediaCommitter: FakePrivateMediaCommitter = FakePrivateMediaCommitter(),
        timeZoneProvider: AppTimeZoneProvider = UTC_PROVIDER,
        analytics: FakeProductAnalytics = FakeProductAnalytics().apply { setCollectionEnabled(true) },
    ) = JourneyViewModel(
        journeyRepository,
        JourneyConstellationPolicy(),
        voicePlaybackController,
        DeleteCompletion(completionRepository, mediaCommitter),
        timeZoneProvider,
        analytics,
    )

    private fun entryAt(id: String, completedAt: Instant, quest: com.togetherly.domain.quest.FamilyQuest? = null, media: List<MemoryMedia> = emptyList()) =
        JourneyEntry(
            com.togetherly.domain.completion.validQuestCompletion(
                id = CompletionId(id),
                questId = quest?.id ?: QuestId("quest-$id"),
                startedAt = null,
                completedAt = completedAt,
                media = media,
            ),
            quest = quest,
        )

    @Test
    fun deleteClickedShowsTheConfirmationDialogWithoutDeletingYet() = runTest {
        val journeyRepository = FakeJourneyRepository().apply { setEntries(listOf(entryAt("c1", Instant.fromEpochSeconds(1_000)))) }
        val completionRepository = FakeCompletionRepository().apply { saveCompletion(com.togetherly.domain.completion.validQuestCompletion(id = CompletionId("c1"))) }
        val model = viewModel(journeyRepository = journeyRepository, completionRepository = completionRepository)
        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(JourneyAction.DeleteClicked(CompletionId("c1")))

        val content = model.uiState.value as JourneyUiState.Content
        assertEquals(CompletionId("c1"), content.pendingDeleteCompletionId)
        val stillThere = (completionRepository.getCompletion(CompletionId("c1")) as com.togetherly.core.result.DataResult.Success).value
        assertTrue(stillThere != null)
    }

    @Test
    fun dismissDeleteDialogClearsThePendingDeletionWithoutDeleting() = runTest {
        val journeyRepository = FakeJourneyRepository().apply { setEntries(listOf(entryAt("c1", Instant.fromEpochSeconds(1_000)))) }
        val completionRepository = FakeCompletionRepository().apply { saveCompletion(com.togetherly.domain.completion.validQuestCompletion(id = CompletionId("c1"))) }
        val model = viewModel(journeyRepository = journeyRepository, completionRepository = completionRepository)
        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(JourneyAction.DeleteClicked(CompletionId("c1")))

        model.onAction(JourneyAction.DismissDeleteDialog)

        val content = model.uiState.value as JourneyUiState.Content
        assertEquals(null, content.pendingDeleteCompletionId)
        val stillThere = (completionRepository.getCompletion(CompletionId("c1")) as com.togetherly.core.result.DataResult.Success).value
        assertTrue(stillThere != null)
    }

    @Test
    fun confirmDeleteClickedActuallyDeletesTheCompletion() = runTest {
        val journeyRepository = FakeJourneyRepository().apply { setEntries(listOf(entryAt("c1", Instant.fromEpochSeconds(1_000)))) }
        val completionRepository = FakeCompletionRepository().apply { saveCompletion(com.togetherly.domain.completion.validQuestCompletion(id = CompletionId("c1"))) }
        val model = viewModel(journeyRepository = journeyRepository, completionRepository = completionRepository)
        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(JourneyAction.DeleteClicked(CompletionId("c1")))

        model.onAction(JourneyAction.ConfirmDeleteClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val deleted = (completionRepository.getCompletion(CompletionId("c1")) as com.togetherly.core.result.DataResult.Success).value
        assertEquals(null, deleted)
    }

    @Test
    fun confirmDeleteFailureSurfacesATransientErrorAndClearsThePendingDialog() = runTest {
        val journeyRepository = FakeJourneyRepository().apply { setEntries(listOf(entryAt("c1", Instant.fromEpochSeconds(1_000)))) }
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(com.togetherly.domain.completion.validQuestCompletion(id = CompletionId("c1")))
            setDeleteCompletionError(AppError.Storage(StorageError.DELETE_FAILED))
        }
        val model = viewModel(journeyRepository = journeyRepository, completionRepository = completionRepository)
        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(JourneyAction.DeleteClicked(CompletionId("c1")))

        model.onAction(JourneyAction.ConfirmDeleteClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = model.uiState.value as JourneyUiState.Content
        assertEquals(null, content.pendingDeleteCompletionId)
        assertTrue(content.transientError != null)
    }

    @Test
    fun emptyJourneyShowsEmptyState() = runTest {
        val model = viewModel()
        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(JourneyUiState.Empty, model.uiState.value)
    }

    @Test
    fun contentOrdersEntriesNewestFirst() = runTest {
        val repository = FakeJourneyRepository().apply {
            setEntries(
                listOf(
                    entryAt("older", Instant.fromEpochSeconds(1_000)),
                    entryAt("newer", Instant.fromEpochSeconds(2_000)),
                ),
            )
        }
        val model = viewModel(journeyRepository = repository)

        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = model.uiState.value as JourneyUiState.Content
        assertEquals(listOf(CompletionId("newer"), CompletionId("older")), content.entries.map { it.completionId })
    }

    @Test
    fun completionWithoutMemoryStillProducesAnEntry() = runTest {
        val repository = FakeJourneyRepository().apply {
            setEntries(listOf(entryAt("c1", Instant.fromEpochSeconds(1_000))))
        }
        val model = viewModel(journeyRepository = repository)

        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = model.uiState.value as JourneyUiState.Content
        val entry = content.entries.single()
        assertNull(entry.note)
        assertNull(entry.photo)
        assertNull(entry.voice)
        assertTrue(entry.reactions.isEmpty())
    }

    @Test
    fun missingQuestRemainsVisibleWithNullTitleAndCategory() = runTest {
        val repository = FakeJourneyRepository().apply {
            setEntries(listOf(entryAt("c1", Instant.fromEpochSeconds(1_000), quest = null)))
        }
        val model = viewModel(journeyRepository = repository)

        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = model.uiState.value as JourneyUiState.Content
        val entry = content.entries.single()
        assertNull(entry.questTitle)
        assertNull(entry.category)
    }

    @Test
    fun starsAreDerivedForEveryEntryUnderTheCap() = runTest {
        val repository = FakeJourneyRepository().apply {
            setEntries(
                (1..5).map { entryAt("c$it", Instant.fromEpochSeconds(it.toLong() * 1_000)) },
            )
        }
        val model = viewModel(journeyRepository = repository)

        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = model.uiState.value as JourneyUiState.Content
        assertEquals(5, content.stars.size)
    }

    @Test
    fun playVoiceClickedPlaysAndTracksPlayingId() = runTest {
        val mediaId = MemoryMediaId("voice-1")
        val reference = MediaReference("completions/c1/voice-1.m4a")
        val repository = FakeJourneyRepository().apply {
            setEntries(
                listOf(
                    entryAt(
                        "c1",
                        Instant.fromEpochSeconds(1_000),
                        media = listOf(MemoryMedia.Voice(mediaId, reference, 8.seconds)),
                    ),
                ),
            )
        }
        val voicePlaybackController = FakeVoicePlaybackController()
        val model = viewModel(journeyRepository = repository, voicePlaybackController = voicePlaybackController)
        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(JourneyAction.PlayVoiceClicked(mediaId, reference))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(reference), voicePlaybackController.playCalls)
        assertNull((model.uiState.value as JourneyUiState.Content).playingVoiceId) // Idle until the controller reports Playing.

        voicePlaybackController.setState(VoicePlaybackState.Playing(0.seconds, 8.seconds))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(mediaId, (model.uiState.value as JourneyUiState.Content).playingVoiceId)
    }

    @Test
    fun pauseVoiceClickedCallsControllerPauseAndClearsPlayingId() = runTest {
        val mediaId = MemoryMediaId("voice-1")
        val reference = MediaReference("completions/c1/voice-1.m4a")
        val repository = FakeJourneyRepository().apply {
            setEntries(listOf(entryAt("c1", Instant.fromEpochSeconds(1_000), media = listOf(MemoryMedia.Voice(mediaId, reference, 8.seconds)))))
        }
        val voicePlaybackController = FakeVoicePlaybackController()
        val model = viewModel(journeyRepository = repository, voicePlaybackController = voicePlaybackController)
        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(JourneyAction.PlayVoiceClicked(mediaId, reference))
        voicePlaybackController.setState(VoicePlaybackState.Playing(0.seconds, 8.seconds))
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(JourneyAction.PauseVoiceClicked)
        voicePlaybackController.setState(VoicePlaybackState.Paused(2.seconds, 8.seconds))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, voicePlaybackController.pauseCalls.size)
        assertNull((model.uiState.value as JourneyUiState.Content).playingVoiceId)
    }

    @Test
    fun startingAnotherClipTracksTheNewIdOnly() = runTest {
        val firstId = MemoryMediaId("voice-1")
        val firstReference = MediaReference("completions/c1/voice-1.m4a")
        val secondId = MemoryMediaId("voice-2")
        val secondReference = MediaReference("completions/c2/voice-2.m4a")
        val repository = FakeJourneyRepository().apply {
            setEntries(
                listOf(
                    entryAt("c1", Instant.fromEpochSeconds(1_000), media = listOf(MemoryMedia.Voice(firstId, firstReference, 8.seconds))),
                    entryAt("c2", Instant.fromEpochSeconds(2_000), media = listOf(MemoryMedia.Voice(secondId, secondReference, 5.seconds))),
                ),
            )
        }
        val voicePlaybackController = FakeVoicePlaybackController()
        val model = viewModel(journeyRepository = repository, voicePlaybackController = voicePlaybackController)
        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(JourneyAction.PlayVoiceClicked(firstId, firstReference))
        voicePlaybackController.setState(VoicePlaybackState.Playing(0.seconds, 8.seconds))
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(JourneyAction.PlayVoiceClicked(secondId, secondReference))
        voicePlaybackController.setState(VoicePlaybackState.Playing(0.seconds, 5.seconds))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(secondId, (model.uiState.value as JourneyUiState.Content).playingVoiceId)
        assertEquals(listOf(firstReference, secondReference), voicePlaybackController.playCalls)
    }

    @Test
    fun stopVoiceClickedCallsControllerStop() = runTest {
        val voicePlaybackController = FakeVoicePlaybackController()
        val model = viewModel(voicePlaybackController = voicePlaybackController)
        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(JourneyAction.StopVoiceClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, voicePlaybackController.stopCalls.size)
    }

    @Test
    fun playVoiceFailureSetsTransientErrorAndClearsPlayingId() = runTest {
        val mediaId = MemoryMediaId("voice-1")
        val reference = MediaReference("completions/c1/voice-1.m4a")
        val repository = FakeJourneyRepository().apply {
            setEntries(listOf(entryAt("c1", Instant.fromEpochSeconds(1_000), media = listOf(MemoryMedia.Voice(mediaId, reference, 8.seconds)))))
        }
        val voicePlaybackController = FakeVoicePlaybackController()
        voicePlaybackController.nextResult = com.togetherly.core.result.DataResult.Error(AppError.Storage(StorageError.READ_FAILED))
        val model = viewModel(journeyRepository = repository, voicePlaybackController = voicePlaybackController)
        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(JourneyAction.PlayVoiceClicked(mediaId, reference))
        testDispatcher.scheduler.advanceUntilIdle()

        val content = model.uiState.value as JourneyUiState.Content
        assertNull(content.playingVoiceId)
        assertEquals(AppError.Storage(StorageError.READ_FAILED).toUiText(), content.transientError)
    }

    @Test
    fun transientErrorDismissedClearsTheField() = runTest {
        val mediaId = MemoryMediaId("voice-1")
        val reference = MediaReference("completions/c1/voice-1.m4a")
        val repository = FakeJourneyRepository().apply {
            setEntries(listOf(entryAt("c1", Instant.fromEpochSeconds(1_000), media = listOf(MemoryMedia.Voice(mediaId, reference, 8.seconds)))))
        }
        val voicePlaybackController = FakeVoicePlaybackController()
        voicePlaybackController.nextResult = com.togetherly.core.result.DataResult.Error(AppError.Storage(StorageError.READ_FAILED))
        val model = viewModel(journeyRepository = repository, voicePlaybackController = voicePlaybackController)
        model.onAction(JourneyAction.ScreenStarted)
        model.onAction(JourneyAction.PlayVoiceClicked(mediaId, reference))
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(JourneyAction.TransientErrorDismissed)

        assertNull((model.uiState.value as JourneyUiState.Content).transientError)
    }

    @Test
    fun goToTodayClickedEmitsNavigateToToday() = runTest {
        val model = viewModel()
        model.events.test {
            model.onAction(JourneyAction.GoToTodayClicked)
            assertEquals(JourneyEvent.NavigateToToday, awaitItem())
        }
    }

    @Test
    fun repositoryErrorShowsRetryableErrorState() = runTest {
        val repository = FakeJourneyRepository().apply { setError(AppError.Storage(StorageError.READ_FAILED)) }
        val model = viewModel(journeyRepository = repository)

        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val error = model.uiState.value as JourneyUiState.Error
        assertEquals(AppError.Storage(StorageError.READ_FAILED).toUiText(), error.message)
        assertTrue(error.canRetry)
    }

    @Test
    fun retryClickedReloadsAfterAFixedError() = runTest {
        val repository = FakeJourneyRepository().apply { setError(AppError.Storage(StorageError.READ_FAILED)) }
        val model = viewModel(journeyRepository = repository)
        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(model.uiState.value is JourneyUiState.Error)

        repository.setEntries(listOf(entryAt("c1", Instant.fromEpochSeconds(1_000))))
        model.onAction(JourneyAction.RetryClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(model.uiState.value is JourneyUiState.Content)
    }

    // -- Analytics --------------------------------------------------------------------------

    @Test
    fun screenStartedCapturesTheJourneyScreenExactlyOnce() = runTest {
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(analytics = analytics)

        model.onAction(JourneyAction.ScreenStarted)
        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(AnalyticsScreen.JOURNEY), analytics.screensViewed)
    }

    @Test
    fun successfulPlayVoiceCapturesMemoryOpened() = runTest {
        val mediaId = MemoryMediaId("voice-1")
        val reference = MediaReference("completions/c1/voice-1.m4a")
        val repository = FakeJourneyRepository().apply {
            setEntries(listOf(entryAt("c1", Instant.fromEpochSeconds(1_000), media = listOf(MemoryMedia.Voice(mediaId, reference, 8.seconds)))))
        }
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(journeyRepository = repository, analytics = analytics)
        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(JourneyAction.PlayVoiceClicked(mediaId, reference))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, analytics.capturedEvents.count { it is MemoryOpened })
    }

    @Test
    fun failedPlayVoiceCapturesNoMemoryOpenedEvent() = runTest {
        val mediaId = MemoryMediaId("voice-1")
        val reference = MediaReference("completions/c1/voice-1.m4a")
        val repository = FakeJourneyRepository().apply {
            setEntries(listOf(entryAt("c1", Instant.fromEpochSeconds(1_000), media = listOf(MemoryMedia.Voice(mediaId, reference, 8.seconds)))))
        }
        val voicePlaybackController = FakeVoicePlaybackController()
        voicePlaybackController.nextResult = com.togetherly.core.result.DataResult.Error(AppError.Storage(StorageError.READ_FAILED))
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(journeyRepository = repository, voicePlaybackController = voicePlaybackController, analytics = analytics)
        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(JourneyAction.PlayVoiceClicked(mediaId, reference))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(analytics.capturedEvents.none { it is MemoryOpened })
    }

    @Test
    fun successfulDeleteCapturesMemoryDeleted() = runTest {
        val journeyRepository = FakeJourneyRepository().apply { setEntries(listOf(entryAt("c1", Instant.fromEpochSeconds(1_000)))) }
        val completionRepository = FakeCompletionRepository().apply { saveCompletion(com.togetherly.domain.completion.validQuestCompletion(id = CompletionId("c1"))) }
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(journeyRepository = journeyRepository, completionRepository = completionRepository, analytics = analytics)
        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(JourneyAction.DeleteClicked(CompletionId("c1")))

        model.onAction(JourneyAction.ConfirmDeleteClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, analytics.capturedEvents.count { it is MemoryDeleted })
    }

    @Test
    fun failedDeleteCapturesNoMemoryDeletedEvent() = runTest {
        val journeyRepository = FakeJourneyRepository().apply { setEntries(listOf(entryAt("c1", Instant.fromEpochSeconds(1_000)))) }
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(com.togetherly.domain.completion.validQuestCompletion(id = CompletionId("c1")))
            setDeleteCompletionError(AppError.Storage(StorageError.DELETE_FAILED))
        }
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(journeyRepository = journeyRepository, completionRepository = completionRepository, analytics = analytics)
        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(JourneyAction.DeleteClicked(CompletionId("c1")))

        model.onAction(JourneyAction.ConfirmDeleteClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(analytics.capturedEvents.none { it is MemoryDeleted })
    }

    @Test
    fun noEventsAreCapturedWithoutConsent() = runTest {
        val journeyRepository = FakeJourneyRepository().apply { setEntries(listOf(entryAt("c1", Instant.fromEpochSeconds(1_000)))) }
        val completionRepository = FakeCompletionRepository().apply { saveCompletion(com.togetherly.domain.completion.validQuestCompletion(id = CompletionId("c1"))) }
        val analytics = FakeProductAnalytics()
        val model = viewModel(journeyRepository = journeyRepository, completionRepository = completionRepository, analytics = analytics)

        model.onAction(JourneyAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(JourneyAction.DeleteClicked(CompletionId("c1")))
        model.onAction(JourneyAction.ConfirmDeleteClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(analytics.screensViewed.isEmpty())
        assertTrue(analytics.capturedEvents.isEmpty())
    }
}

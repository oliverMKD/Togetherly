package com.togetherly.feature.memory.presentation

import app.cash.turbine.test
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.id.SequentialIdGenerator
import com.togetherly.core.media.MicrophonePermissionResult
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.FakeProductAnalytics
import com.togetherly.core.telemetry.MemorySaved
import com.togetherly.core.ui.toUiText
import com.togetherly.data.media.FakeVoicePlaybackController
import com.togetherly.data.media.FakeVoiceRecorder
import com.togetherly.data.media.PendingPhoto
import com.togetherly.data.media.PendingVoiceRecording
import com.togetherly.data.media.PhotoPickerResult
import com.togetherly.data.media.VoicePlaybackState
import com.togetherly.data.media.VoiceRecorderState
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.FamilyReaction
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.MemoryNote
import com.togetherly.domain.completion.PendingMediaReference
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.repository.FakePrivateMediaCommitter
import com.togetherly.domain.completion.usecase.DiscardCompletionMemoryDraft
import com.togetherly.domain.completion.usecase.SaveCompletionMemory
import com.togetherly.domain.completion.validQuestCompletion
import com.togetherly.domain.family.MemoryPreferences
import com.togetherly.domain.family.repository.FakeFamilySettingsRepository
import com.togetherly.domain.family.testFamilySettings
import com.togetherly.domain.family.usecase.ObserveFamilySettings
import com.togetherly.integration.testFamilyProfile
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.validFamilyQuest
import com.togetherly.feature.memory.model.PhotoPreviewReference
import com.togetherly.feature.memory.model.VoicePreviewReference
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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val DEFAULT_COMPLETION_ID = CompletionId("completion-1")
private val DEFAULT_QUEST_ID = QuestId("quest-1")

@OptIn(ExperimentalCoroutinesApi::class)
class CompletionMemoryViewModelTest {

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
        completionId: CompletionId = DEFAULT_COMPLETION_ID,
        completionRepository: FakeCompletionRepository = FakeCompletionRepository(),
        questRepository: FakeQuestRepository = FakeQuestRepository(),
        mediaCommitter: FakePrivateMediaCommitter = FakePrivateMediaCommitter(),
        voiceRecorder: FakeVoiceRecorder = FakeVoiceRecorder(),
        voicePlaybackController: FakeVoicePlaybackController = FakeVoicePlaybackController(),
        familySettingsRepository: FakeFamilySettingsRepository = FakeFamilySettingsRepository(),
        analytics: FakeProductAnalytics = FakeProductAnalytics().apply { setCollectionEnabled(true) },
    ) = CompletionMemoryViewModel(
        completionId,
        completionRepository,
        questRepository,
        SaveCompletionMemory(completionRepository, mediaCommitter, SequentialIdGenerator()),
        DiscardCompletionMemoryDraft(mediaCommitter),
        ObserveFamilySettings(familySettingsRepository),
        mediaCommitter,
        voiceRecorder,
        voicePlaybackController,
        analytics,
    )

    private suspend fun FakeCompletionRepository.seed(
        completionId: CompletionId = DEFAULT_COMPLETION_ID,
        questId: QuestId = DEFAULT_QUEST_ID,
        note: MemoryNote? = null,
        reactions: Set<FamilyReaction> = emptySet(),
        media: List<MemoryMedia> = emptyList(),
    ) {
        saveCompletion(
            validQuestCompletion(id = completionId, questId = questId, note = note, reactions = reactions, media = media),
        )
    }

    @Test
    fun loadsExistingCompletionIntoState() = runTest {
        val completionRepository = FakeCompletionRepository()
        val existingPhoto = MemoryMedia.Photo(MemoryMediaId("media-1"), MediaReference("completions/c/photo-1.jpg"))
        val existingVoice = MemoryMedia.Voice(MemoryMediaId("media-2"), MediaReference("completions/c/voice-1.m4a"), 12.seconds)
        completionRepository.seed(
            note = MemoryNote("A lovely afternoon."),
            reactions = setOf(FamilyReaction.HAPPY, FamilyReaction.SILLY),
            media = listOf(existingPhoto, existingVoice),
        )
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(validFamilyQuest(id = DEFAULT_QUEST_ID))) }
        val model = viewModel(completionRepository = completionRepository, questRepository = questRepository)

        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = model.uiState.value
        assertEquals("A lovely afternoon.", state.note)
        assertEquals(setOf(FamilyReaction.HAPPY, FamilyReaction.SILLY), state.reactions)
        assertEquals(PhotoPreviewReference.Committed(existingPhoto.localReference), state.photo?.reference)
        assertEquals(VoicePreviewReference.Committed(existingVoice.localReference), state.voice?.reference)
        assertEquals(validFamilyQuest(id = DEFAULT_QUEST_ID).title.value, state.questTitle)
    }

    @Test
    fun completionUiReflectsMemoryPreferencesFromSettings() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val familySettingsRepository = FakeFamilySettingsRepository()
        familySettingsRepository.setSettings(
            testFamilySettings(
                profile = testFamilyProfile(),
                memoryPreferences = MemoryPreferences(allowPhotos = false, allowVoiceMemories = false, allowTextNotes = true, showMemoryPromptAfterQuests = true),
            ),
        )
        val model = viewModel(completionRepository = completionRepository, familySettingsRepository = familySettingsRepository)

        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = model.uiState.value
        assertFalse(state.allowPhotos)
        assertFalse(state.allowVoiceMemories)
        assertTrue(state.allowTextNotes)
    }

    @Test
    fun loadingNeverRequestsMicrophonePermissionRegardlessOfMemoryPreferences() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val familySettingsRepository = FakeFamilySettingsRepository()
        familySettingsRepository.setSettings(
            testFamilySettings(profile = testFamilyProfile(), memoryPreferences = MemoryPreferences.defaults().copy(allowVoiceMemories = false)),
        )
        val model = viewModel(completionRepository = completionRepository, familySettingsRepository = familySettingsRepository)

        model.events.test {
            model.onAction(CompletionMemoryAction.ScreenStarted)
            testDispatcher.scheduler.advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun microphonePermissionIsRequestedOnlyWhenRecordIsTapped() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val model = viewModel(completionRepository = completionRepository)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.events.test {
            model.onAction(CompletionMemoryAction.RecordVoiceClicked)
            assertEquals(CompletionMemoryEvent.RequestMicrophonePermission, awaitItem())
        }
    }

    @Test
    fun toggleReactionAddsThenRemoves() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val model = viewModel(completionRepository = completionRepository)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(CompletionMemoryAction.ReactionToggled(FamilyReaction.CALM))
        assertTrue(FamilyReaction.CALM in model.uiState.value.reactions)

        model.onAction(CompletionMemoryAction.ReactionToggled(FamilyReaction.CALM))
        assertFalse(FamilyReaction.CALM in model.uiState.value.reactions)
    }

    @Test
    fun noteTooLongFailsSaveWithNoteError() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val model = viewModel(completionRepository = completionRepository)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(CompletionMemoryAction.NoteChanged("a".repeat(1001)))
        model.onAction(CompletionMemoryAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = model.uiState.value
        assertFalse(state.isSaving)
        assertEquals(AppError.Validation(ValidationError.INVALID_INPUT).toUiText(), state.noteError)
        assertNull(state.saveError)
    }

    @Test
    fun addPhotoClickedEmitsLaunchPhotoPicker() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val model = viewModel(completionRepository = completionRepository)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.events.test {
            model.onAction(CompletionMemoryAction.AddPhotoClicked)
            assertEquals(CompletionMemoryEvent.LaunchPhotoPicker, awaitItem())
        }
    }

    @Test
    fun photoImportCancelledLeavesStateUnchanged() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val model = viewModel(completionRepository = completionRepository)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(CompletionMemoryAction.PhotoImportCompleted(PhotoPickerResult.Cancelled))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(model.uiState.value.photo)
    }

    @Test
    fun photoImportSuccessStagesPendingPhoto() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val model = viewModel(completionRepository = completionRepository)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val pendingReference = PendingMediaReference("pending-photo-1")
        model.onAction(
            CompletionMemoryAction.PhotoImportCompleted(
                PhotoPickerResult.Imported(PendingPhoto(pendingReference, width = 100, height = 200, sizeBytes = 42L)),
            ),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PhotoPreviewReference.Pending(pendingReference), model.uiState.value.photo?.reference)
    }

    @Test
    fun replacingPendingPhotoDeletesThePreviousPending() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val mediaCommitter = FakePrivateMediaCommitter()
        val model = viewModel(completionRepository = completionRepository, mediaCommitter = mediaCommitter)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val firstPending = PendingMediaReference("pending-photo-1")
        model.onAction(
            CompletionMemoryAction.PhotoImportCompleted(
                PhotoPickerResult.Imported(PendingPhoto(firstPending, width = 100, height = 200, sizeBytes = 42L)),
            ),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val secondPending = PendingMediaReference("pending-photo-2")
        model.onAction(
            CompletionMemoryAction.PhotoImportCompleted(
                PhotoPickerResult.Imported(PendingPhoto(secondPending, width = 100, height = 200, sizeBytes = 42L)),
            ),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(firstPending), mediaCommitter.deletePendingCalls)
        assertEquals(PhotoPreviewReference.Pending(secondPending), model.uiState.value.photo?.reference)
    }

    @Test
    fun removingCommittedPhotoClearsStateAndMarksExplicitRemoval() = runTest {
        val existingPhoto = MemoryMedia.Photo(MemoryMediaId("media-1"), MediaReference("completions/c/photo-1.jpg"))
        val completionRepository = FakeCompletionRepository().apply { seed(media = listOf(existingPhoto)) }
        val mediaCommitter = FakePrivateMediaCommitter()
        val model = viewModel(completionRepository = completionRepository, mediaCommitter = mediaCommitter)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(CompletionMemoryAction.RemovePhotoClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(model.uiState.value.photo)
        // Removal is only recorded as an explicit MediaEdit.Remove on Save/Skip — nothing is
        // deleted from storage yet since the existing photo was never pending.
        assertTrue(mediaCommitter.deletePendingCalls.isEmpty())
    }

    @Test
    fun microphonePermissionDeniedShowsMediaError() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val model = viewModel(completionRepository = completionRepository)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(CompletionMemoryAction.MicrophonePermissionResultReceived(MicrophonePermissionResult.Denied))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(model.uiState.value.mediaError != null)
        assertFalse(model.uiState.value.microphonePermissionPermanentlyDenied)
    }

    @Test
    fun microphonePermissionPermanentlyDeniedFlagsOpenSettingsAffordance() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val model = viewModel(completionRepository = completionRepository)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(CompletionMemoryAction.MicrophonePermissionResultReceived(MicrophonePermissionResult.PermanentlyDenied))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(model.uiState.value.microphonePermissionPermanentlyDenied)

        model.onAction(CompletionMemoryAction.RecordVoiceClicked)

        assertFalse(model.uiState.value.microphonePermissionPermanentlyDenied)
    }

    @Test
    fun microphonePermissionGrantedStartsRecording() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val voiceRecorder = FakeVoiceRecorder()
        val model = viewModel(completionRepository = completionRepository, voiceRecorder = voiceRecorder)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(CompletionMemoryAction.MicrophonePermissionResultReceived(MicrophonePermissionResult.Granted))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, voiceRecorder.startCalls.size)
    }

    @Test
    fun recorderStateUpdatesReflectInUiState() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val voiceRecorder = FakeVoiceRecorder()
        val model = viewModel(completionRepository = completionRepository, voiceRecorder = voiceRecorder)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        voiceRecorder.setState(VoiceRecorderState.Recording(Instant.parse("2026-01-01T00:00:00Z"), 5.seconds, 60.seconds))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = model.uiState.value
        assertTrue(state.isRecording)
        assertEquals("0:05", state.recordingElapsed)
    }

    @Test
    fun stopRecordingStagesPendingVoice() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val voiceRecorder = FakeVoiceRecorder()
        val pendingReference = PendingMediaReference("pending-voice-1")
        voiceRecorder.nextStopResult = DataResult.Success(
            PendingVoiceRecording(pendingReference, 8.seconds, 1234L),
        )
        val model = viewModel(completionRepository = completionRepository, voiceRecorder = voiceRecorder)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(CompletionMemoryAction.StopRecordingClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, voiceRecorder.stopCalls.size)
        assertEquals(VoicePreviewReference.Pending(pendingReference), model.uiState.value.voice?.reference)
    }

    @Test
    fun cancelRecordingCallsRecorderCancel() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val voiceRecorder = FakeVoiceRecorder()
        val model = viewModel(completionRepository = completionRepository, voiceRecorder = voiceRecorder)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(CompletionMemoryAction.CancelRecordingClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, voiceRecorder.cancelCalls.size)
    }

    @Test
    fun playVoiceClickedPlaysPendingReference() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val voiceRecorder = FakeVoiceRecorder()
        val voicePlaybackController = FakeVoicePlaybackController()
        val pendingReference = PendingMediaReference("pending-voice-1")
        voiceRecorder.nextStopResult = DataResult.Success(
            PendingVoiceRecording(pendingReference, 8.seconds, 1234L),
        )
        val model = viewModel(
            completionRepository = completionRepository,
            voiceRecorder = voiceRecorder,
            voicePlaybackController = voicePlaybackController,
        )
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(CompletionMemoryAction.StopRecordingClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(CompletionMemoryAction.PlayVoiceClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(pendingReference), voicePlaybackController.playPendingCalls)

        voicePlaybackController.setState(VoicePlaybackState.Playing(0.seconds, 8.seconds))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(model.uiState.value.isPlayingVoice)

        model.onAction(CompletionMemoryAction.PauseVoiceClicked)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, voicePlaybackController.pauseCalls.size)
    }

    @Test
    fun saveSucceedsAndEmitsMemorySaved() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val model = viewModel(completionRepository = completionRepository)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(CompletionMemoryAction.NoteChanged("What a day."))
        model.events.test {
            model.onAction(CompletionMemoryAction.SaveClicked)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(CompletionMemoryEvent.MemorySaved(DEFAULT_COMPLETION_ID), awaitItem())
        }
    }

    @Test
    fun saveFailurePreservesDraftAndShowsSaveError() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val model = viewModel(completionRepository = completionRepository)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(CompletionMemoryAction.NoteChanged("A note to keep."))
        model.onAction(CompletionMemoryAction.ReactionToggled(FamilyReaction.HAPPY))
        completionRepository.setSaveCompletionError(AppError.Storage(StorageError.WRITE_FAILED))

        model.onAction(CompletionMemoryAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = model.uiState.value
        assertFalse(state.isSaving)
        assertEquals(AppError.Storage(StorageError.WRITE_FAILED).toUiText(), state.saveError)
        assertEquals("A note to keep.", state.note)
        assertTrue(FamilyReaction.HAPPY in state.reactions)
    }

    @Test
    fun duplicateSaveClickedIsIgnoredWhileSaving() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val model = viewModel(completionRepository = completionRepository)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.events.test {
            model.onAction(CompletionMemoryAction.SaveClicked)
            model.onAction(CompletionMemoryAction.SaveClicked)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(CompletionMemoryEvent.MemorySaved(DEFAULT_COMPLETION_ID), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun skipDeletesPendingMediaAndEmitsMemorySkipped() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val mediaCommitter = FakePrivateMediaCommitter()
        val voiceRecorder = FakeVoiceRecorder()
        val photoPending = PendingMediaReference("pending-photo-1")
        val voicePending = PendingMediaReference("pending-voice-1")
        voiceRecorder.nextStopResult = DataResult.Success(
            PendingVoiceRecording(voicePending, 8.seconds, 1234L),
        )
        val model = viewModel(
            completionRepository = completionRepository,
            mediaCommitter = mediaCommitter,
            voiceRecorder = voiceRecorder,
        )
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(
            CompletionMemoryAction.PhotoImportCompleted(
                PhotoPickerResult.Imported(PendingPhoto(photoPending, width = 100, height = 200, sizeBytes = 42L)),
            ),
        )
        model.onAction(CompletionMemoryAction.StopRecordingClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        model.events.test {
            model.onAction(CompletionMemoryAction.SkipClicked)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(CompletionMemoryEvent.MemorySkipped, awaitItem())
        }

        assertTrue(photoPending in mediaCommitter.deletePendingCalls)
        assertTrue(voicePending in mediaCommitter.deletePendingCalls)
        // The completion itself is untouched by Skip — only pending files are cleaned up, nothing
        // is ever committed onto the persisted completion.
        val persisted = (completionRepository.getCompletion(DEFAULT_COMPLETION_ID) as DataResult.Success).value
        assertTrue(persisted?.media.isNullOrEmpty())
        assertTrue(mediaCommitter.committedPhotoCalls.isEmpty())
        assertTrue(mediaCommitter.committedVoiceCalls.isEmpty())
    }

    @Test
    fun backWithUnsavedChangesShowsDiscardConfirmation() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val model = viewModel(completionRepository = completionRepository)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(CompletionMemoryAction.NoteChanged("Unsaved edit"))
        model.events.test {
            model.onAction(CompletionMemoryAction.BackClicked)
            testDispatcher.scheduler.advanceUntilIdle()
            expectNoEvents()
        }
        assertTrue(model.uiState.value.showDiscardConfirmation)
    }

    @Test
    fun backWithoutChangesNavigatesBackImmediately() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val model = viewModel(completionRepository = completionRepository)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.events.test {
            model.onAction(CompletionMemoryAction.BackClicked)
            assertEquals(CompletionMemoryEvent.NavigateBack, awaitItem())
        }
        assertFalse(model.uiState.value.showDiscardConfirmation)
    }

    @Test
    fun discardConfirmedDeletesPendingMediaAndNavigatesBack() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val mediaCommitter = FakePrivateMediaCommitter()
        val model = viewModel(completionRepository = completionRepository, mediaCommitter = mediaCommitter)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val photoPending = PendingMediaReference("pending-photo-1")
        model.onAction(
            CompletionMemoryAction.PhotoImportCompleted(
                PhotoPickerResult.Imported(PendingPhoto(photoPending, width = 100, height = 200, sizeBytes = 42L)),
            ),
        )
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(CompletionMemoryAction.BackClicked)

        model.events.test {
            model.onAction(CompletionMemoryAction.DiscardConfirmed)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(CompletionMemoryEvent.NavigateBack, awaitItem())
        }
        assertTrue(photoPending in mediaCommitter.deletePendingCalls)
        assertFalse(model.uiState.value.showDiscardConfirmation)
    }

    @Test
    fun existingCommittedMediaSurvivesAFailedReplacementSave() = runTest {
        val existingPhoto = MemoryMedia.Photo(MemoryMediaId("media-1"), MediaReference("completions/c/photo-1.jpg"))
        val completionRepository = FakeCompletionRepository().apply { seed(media = listOf(existingPhoto)) }
        val mediaCommitter = FakePrivateMediaCommitter()
        val model = viewModel(completionRepository = completionRepository, mediaCommitter = mediaCommitter)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(
            CompletionMemoryAction.PhotoImportCompleted(
                PhotoPickerResult.Imported(
                    PendingPhoto(PendingMediaReference("pending-photo-1"), width = 100, height = 200, sizeBytes = 42L),
                ),
            ),
        )
        testDispatcher.scheduler.advanceUntilIdle()
        completionRepository.setSaveCompletionError(AppError.Storage(StorageError.WRITE_FAILED))

        model.onAction(CompletionMemoryAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(model.uiState.value.isSaving)
        assertTrue(model.uiState.value.saveError != null)
        // A failed save must never delete media that was already committed before this attempt.
        assertTrue(mediaCommitter.deleteCommittedCalls.none { it.id == existingPhoto.id })
    }

    @Test
    fun uiStateNeverHoldsRawPlatformMediaReferences() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val model = viewModel(completionRepository = completionRepository)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val pendingReference = PendingMediaReference("pending-photo-1")
        model.onAction(
            CompletionMemoryAction.PhotoImportCompleted(
                PhotoPickerResult.Imported(PendingPhoto(pendingReference, width = 100, height = 200, sizeBytes = 42L)),
            ),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // The only way to reach a raw platform URI/handle from here is through PendingMediaReference's
        // own opaque String — the UI state itself never carries anything platform-specific.
        val reference = model.uiState.value.photo?.reference
        assertEquals(PhotoPreviewReference.Pending(pendingReference), reference)
    }

    // -- Analytics --------------------------------------------------------------------------

    @Test
    fun successfulSaveCapturesMemorySavedWithTheRightMediaBooleans() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(completionRepository = completionRepository, analytics = analytics)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(CompletionMemoryAction.NoteChanged("What a day."))
        model.onAction(
            CompletionMemoryAction.PhotoImportCompleted(
                PhotoPickerResult.Imported(PendingPhoto(PendingMediaReference("pending-photo-1"), width = 100, height = 200, sizeBytes = 42L)),
            ),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(CompletionMemoryAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = analytics.capturedEvents.single() as MemorySaved
        assertTrue(event.hasNote)
        assertTrue(event.hasPhoto)
        assertFalse(event.hasVoice)
    }

    @Test
    fun memorySavedEventNeverCarriesTheNoteTextItself() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(completionRepository = completionRepository, analytics = analytics)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(CompletionMemoryAction.NoteChanged("A very specific and identifying afternoon story."))

        model.onAction(CompletionMemoryAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = analytics.capturedEvents.single() as MemorySaved
        val propertyValues = event.properties().values.map { it.toString() }
        assertTrue(propertyValues.none { it.contains("afternoon story") })
    }

    @Test
    fun failedSaveCapturesNoMemorySavedEvent() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(completionRepository = completionRepository, analytics = analytics)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        completionRepository.setSaveCompletionError(AppError.Storage(StorageError.WRITE_FAILED))
        model.onAction(CompletionMemoryAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(analytics.capturedEvents.isEmpty())
    }

    @Test
    fun duplicateSaveClicksNeverCaptureTwice() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(completionRepository = completionRepository, analytics = analytics)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(CompletionMemoryAction.SaveClicked)
        model.onAction(CompletionMemoryAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, analytics.capturedEvents.count { it is MemorySaved })
    }

    @Test
    fun noEventIsCapturedWithoutConsent() = runTest {
        val completionRepository = FakeCompletionRepository().apply { seed() }
        val analytics = FakeProductAnalytics()
        val model = viewModel(completionRepository = completionRepository, analytics = analytics)
        model.onAction(CompletionMemoryAction.ScreenStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(CompletionMemoryAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(analytics.capturedEvents.isEmpty())
    }
}

package com.togetherly.feature.memory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.media.MicrophonePermissionResult
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.MemorySaved
import com.togetherly.core.telemetry.ProductAnalytics
import com.togetherly.core.ui.UiText
import com.togetherly.core.ui.toUiText
import com.togetherly.data.media.PhotoPickerResult
import com.togetherly.data.media.VoicePlaybackController
import com.togetherly.data.media.VoicePlaybackState
import com.togetherly.data.media.VoiceRecorder
import com.togetherly.data.media.VoiceRecorderState
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.CompletionMemoryDraft
import com.togetherly.domain.completion.FamilyReaction
import com.togetherly.domain.completion.MediaEdit
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.PendingMediaReference
import com.togetherly.domain.completion.PendingVoiceReference
import com.togetherly.domain.completion.SaveCompletionMemoryCommand
import com.togetherly.domain.completion.repository.CompletionRepository
import com.togetherly.domain.completion.repository.PrivateMediaCommitter
import com.togetherly.domain.completion.usecase.DiscardCompletionMemoryDraft
import com.togetherly.domain.completion.usecase.SaveCompletionMemory
import com.togetherly.domain.family.MemoryPreferences
import com.togetherly.domain.family.usecase.ObserveFamilySettings
import com.togetherly.domain.quest.repository.QuestRepository
import com.togetherly.feature.memory.mapper.toVoiceDurationLabel
import com.togetherly.feature.memory.model.CompletionMemoryUiState
import com.togetherly.feature.memory.model.MemoryPhotoUi
import com.togetherly.feature.memory.model.MemoryVoiceUi
import com.togetherly.feature.memory.model.PhotoPreviewReference
import com.togetherly.feature.memory.model.VoicePreviewReference
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.memory_voice_permission_denied
import togetherly.shared.generated.resources.memory_voice_permission_permanently_denied

/**
 * [completionId] is supplied at construction (Koin `parametersOf`), the same pattern as
 * [com.togetherly.feature.completion.presentation.CompletionCelebrationViewModel]. Media
 * replacement/removal is tracked here, not just in [CompletionMemoryUiState] — [originalPhoto]/
 * [originalVoice] (what the completion already had when the screen opened) and
 * [pendingPhotoReplacement]/[pendingVoiceReplacement] (newly staged this session) together decide
 * whether [SaveCompletionMemoryCommand.photo]/`.voice` is [MediaEdit.Unchanged], [MediaEdit.Remove],
 * or [MediaEdit.Replace] — see [buildSaveCommand].
 */
class CompletionMemoryViewModel(
    private val completionId: CompletionId,
    private val completionRepository: CompletionRepository,
    private val questRepository: QuestRepository,
    private val saveCompletionMemory: SaveCompletionMemory,
    private val discardCompletionMemoryDraft: DiscardCompletionMemoryDraft,
    private val observeFamilySettings: ObserveFamilySettings,
    private val mediaCommitter: PrivateMediaCommitter,
    private val voiceRecorder: VoiceRecorder,
    private val voicePlaybackController: VoicePlaybackController,
    private val analytics: ProductAnalytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompletionMemoryUiState(completionId = completionId))
    val uiState: StateFlow<CompletionMemoryUiState> = _uiState.asStateFlow()

    private val _events = Channel<CompletionMemoryEvent>(Channel.BUFFERED)
    val events: Flow<CompletionMemoryEvent> = _events.receiveAsFlow()

    private var hasStarted = false
    private var originalNote = ""
    private var originalReactions = emptySet<FamilyReaction>()
    private var originalPhoto: MemoryMedia.Photo? = null
    private var originalVoice: MemoryMedia.Voice? = null
    private var pendingPhotoReplacement: PendingMediaReference? = null
    private var pendingVoiceReplacement: PendingVoiceReference? = null
    private var photoExplicitlyRemoved = false
    private var voiceExplicitlyRemoved = false

    init {
        viewModelScope.launch {
            voiceRecorder.observeState().collect { recorderState -> onRecorderState(recorderState) }
        }
        viewModelScope.launch {
            voicePlaybackController.observeState().collect { playbackState -> onPlaybackState(playbackState) }
        }
    }

    fun onAction(action: CompletionMemoryAction) {
        when (action) {
            CompletionMemoryAction.ScreenStarted -> onScreenStarted()
            is CompletionMemoryAction.NoteChanged -> onNoteChanged(action.value)
            is CompletionMemoryAction.ReactionToggled -> onReactionToggled(action.reaction)
            CompletionMemoryAction.AddPhotoClicked -> onAddPhotoClicked()
            is CompletionMemoryAction.PhotoImportCompleted -> onPhotoImportCompleted(action.result)
            CompletionMemoryAction.RemovePhotoClicked -> onRemovePhotoClicked()
            CompletionMemoryAction.RecordVoiceClicked -> onRecordVoiceClicked()
            is CompletionMemoryAction.MicrophonePermissionResultReceived -> onMicrophonePermissionResult(action.result)
            CompletionMemoryAction.StopRecordingClicked -> onStopRecordingClicked()
            CompletionMemoryAction.CancelRecordingClicked -> onCancelRecordingClicked()
            CompletionMemoryAction.PlayVoiceClicked -> onPlayVoiceClicked()
            CompletionMemoryAction.PauseVoiceClicked -> onPauseVoiceClicked()
            CompletionMemoryAction.RemoveVoiceClicked -> onRemoveVoiceClicked()
            CompletionMemoryAction.SaveClicked -> onSaveClicked()
            CompletionMemoryAction.SkipClicked -> onSkipClicked()
            CompletionMemoryAction.BackClicked -> onBackClicked()
            CompletionMemoryAction.DiscardConfirmed -> onDiscardConfirmed()
            CompletionMemoryAction.DialogDismissed -> onDialogDismissed()
        }
    }

    private fun onScreenStarted() {
        if (hasStarted) return
        hasStarted = true
        viewModelScope.launch {
            val completion = when (val result = completionRepository.getCompletion(completionId)) {
                is DataResult.Error -> {
                    _uiState.update { it.copy(saveError = result.error.toUiText()) }
                    return@launch
                }
                is DataResult.Success -> result.value ?: return@launch
            }

            originalNote = completion.note?.value ?: ""
            originalReactions = completion.reactions
            originalPhoto = completion.media.filterIsInstance<MemoryMedia.Photo>().firstOrNull()
            originalVoice = completion.media.filterIsInstance<MemoryMedia.Voice>().firstOrNull()

            val questTitle = when (val result = questRepository.getQuest(completion.questId)) {
                is DataResult.Success -> result.value?.title?.value
                is DataResult.Error -> null
            }

            // Loaded before presenting capture options (Step 13.5) — never touches an
            // already-saved note/photo/voice, only what's newly offered below.
            val memoryPreferences = when (val result = observeFamilySettings().first()) {
                is DataResult.Success -> result.value?.memoryPreferences ?: MemoryPreferences.defaults()
                is DataResult.Error -> MemoryPreferences.defaults()
            }

            _uiState.update {
                it.copy(
                    questTitle = questTitle,
                    note = originalNote,
                    reactions = originalReactions.toPersistentSet(),
                    photo = originalPhoto?.let { photo -> MemoryPhotoUi(PhotoPreviewReference.Committed(photo.localReference)) },
                    voice = originalVoice?.let { voice ->
                        MemoryVoiceUi(VoicePreviewReference.Committed(voice.localReference), voice.duration.toVoiceDurationLabel())
                    },
                    allowPhotos = memoryPreferences.allowPhotos,
                    allowVoiceMemories = memoryPreferences.allowVoiceMemories,
                    allowTextNotes = memoryPreferences.allowTextNotes,
                )
            }
        }
    }

    private fun onNoteChanged(value: String) {
        _uiState.update { it.copy(note = value, noteError = null) }
    }

    private fun onReactionToggled(reaction: FamilyReaction) {
        _uiState.update {
            val reactions = if (reaction in it.reactions) it.reactions.removing(reaction) else it.reactions.adding(reaction)
            it.copy(reactions = reactions)
        }
    }

    private fun onAddPhotoClicked() {
        _uiState.update { it.copy(mediaError = null) }
        viewModelScope.launch { _events.send(CompletionMemoryEvent.LaunchPhotoPicker) }
    }

    private fun onPhotoImportCompleted(result: PhotoPickerResult) {
        when (result) {
            is PhotoPickerResult.Imported -> {
                viewModelScope.launch {
                    // A previous pending photo staged this session, now superseded, is deleted
                    // immediately rather than left as an orphan until Save/Skip/Discard.
                    pendingPhotoReplacement?.let { mediaCommitter.deletePending(it) }
                    pendingPhotoReplacement = result.pendingPhoto.reference
                    photoExplicitlyRemoved = false
                    _uiState.update {
                        it.copy(photo = MemoryPhotoUi(PhotoPreviewReference.Pending(result.pendingPhoto.reference)), mediaError = null)
                    }
                }
            }
            PhotoPickerResult.Cancelled -> Unit
            is PhotoPickerResult.Failure -> _uiState.update { it.copy(mediaError = result.error.toUiText()) }
        }
    }

    private fun onRemovePhotoClicked() {
        viewModelScope.launch {
            pendingPhotoReplacement?.let { mediaCommitter.deletePending(it) }
            pendingPhotoReplacement = null
            photoExplicitlyRemoved = originalPhoto != null
            _uiState.update { it.copy(photo = null) }
        }
    }

    private fun onRecordVoiceClicked() {
        _uiState.update { it.copy(mediaError = null, microphonePermissionPermanentlyDenied = false) }
        viewModelScope.launch { _events.send(CompletionMemoryEvent.RequestMicrophonePermission) }
    }

    private fun onMicrophonePermissionResult(result: MicrophonePermissionResult) {
        when (result) {
            MicrophonePermissionResult.Granted -> startRecording()
            MicrophonePermissionResult.Denied -> _uiState.update {
                it.copy(
                    mediaError = UiText.Resource(Res.string.memory_voice_permission_denied),
                    microphonePermissionPermanentlyDenied = false,
                )
            }
            MicrophonePermissionResult.PermanentlyDenied -> _uiState.update {
                it.copy(
                    mediaError = UiText.Resource(Res.string.memory_voice_permission_permanently_denied),
                    microphonePermissionPermanentlyDenied = true,
                )
            }
        }
    }

    private fun startRecording() {
        viewModelScope.launch {
            val result = voiceRecorder.start()
            if (result is DataResult.Error) {
                _uiState.update { it.copy(mediaError = result.error.toUiText()) }
            }
        }
    }

    private fun onStopRecordingClicked() {
        viewModelScope.launch {
            when (val result = voiceRecorder.stop()) {
                is DataResult.Success -> {
                    pendingVoiceReplacement?.reference?.let { mediaCommitter.deletePending(it) }
                    val pendingVoice = PendingVoiceReference(result.value.reference, result.value.duration)
                    pendingVoiceReplacement = pendingVoice
                    voiceExplicitlyRemoved = false
                    _uiState.update {
                        it.copy(
                            voice = MemoryVoiceUi(VoicePreviewReference.Pending(pendingVoice.reference), pendingVoice.duration.toVoiceDurationLabel()),
                            mediaError = null,
                        )
                    }
                }
                is DataResult.Error -> _uiState.update { it.copy(mediaError = result.error.toUiText()) }
            }
        }
    }

    private fun onCancelRecordingClicked() {
        viewModelScope.launch { voiceRecorder.cancel() }
    }

    private fun onPlayVoiceClicked() {
        val voice = _uiState.value.voice ?: return
        viewModelScope.launch {
            val result = when (val reference = voice.reference) {
                is VoicePreviewReference.Pending -> voicePlaybackController.playPending(reference.reference)
                is VoicePreviewReference.Committed -> voicePlaybackController.play(reference.reference)
            }
            if (result is DataResult.Error) {
                _uiState.update { it.copy(mediaError = result.error.toUiText()) }
            }
        }
    }

    private fun onPauseVoiceClicked() {
        viewModelScope.launch { voicePlaybackController.pause() }
    }

    private fun onRemoveVoiceClicked() {
        viewModelScope.launch {
            voicePlaybackController.stop()
            pendingVoiceReplacement?.let { mediaCommitter.deletePending(it.reference) }
            pendingVoiceReplacement = null
            voiceExplicitlyRemoved = originalVoice != null
            _uiState.update { it.copy(voice = null) }
        }
    }

    private fun onSaveClicked() {
        val state = _uiState.value
        if (state.isSaving || state.isRecording) return

        _uiState.update { it.copy(isSaving = true, noteError = null, saveError = null) }
        viewModelScope.launch {
            voicePlaybackController.stop()

            when (val result = saveCompletionMemory(buildSaveCommand())) {
                is DataResult.Success -> {
                    analytics.capture(
                        MemorySaved(
                            hasNote = state.note.isNotBlank(),
                            hasPhoto = state.photo != null,
                            hasVoice = state.voice != null,
                        ),
                    )
                    _events.send(CompletionMemoryEvent.MemorySaved(completionId))
                }
                is DataResult.Error -> {
                    val isNoteError = (result.error as? AppError.Validation)?.reason == ValidationError.INVALID_INPUT
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            noteError = if (isNoteError) result.error.toUiText() else null,
                            saveError = if (isNoteError) null else result.error.toUiText(),
                        )
                    }
                }
            }
        }
    }

    private fun onSkipClicked() {
        viewModelScope.launch {
            discardCompletionMemoryDraft(currentDraft())
            _events.send(CompletionMemoryEvent.MemorySkipped)
        }
    }

    private fun onBackClicked() {
        if (hasUnsavedChanges()) {
            _uiState.update { it.copy(showDiscardConfirmation = true) }
        } else {
            viewModelScope.launch { _events.send(CompletionMemoryEvent.NavigateBack) }
        }
    }

    private fun onDiscardConfirmed() {
        viewModelScope.launch {
            discardCompletionMemoryDraft(currentDraft())
            _uiState.update { it.copy(showDiscardConfirmation = false) }
            _events.send(CompletionMemoryEvent.NavigateBack)
        }
    }

    private fun onDialogDismissed() {
        _uiState.update { it.copy(showDiscardConfirmation = false) }
    }

    private fun onRecorderState(state: VoiceRecorderState) {
        when (state) {
            VoiceRecorderState.Idle -> _uiState.update { it.copy(isRecording = false, recordingElapsed = null) }
            is VoiceRecorderState.Recording -> _uiState.update {
                it.copy(isRecording = true, recordingElapsed = state.elapsed.toVoiceDurationLabel())
            }
            VoiceRecorderState.Stopping -> _uiState.update { it.copy(isRecording = false) }
            is VoiceRecorderState.Failed -> _uiState.update {
                it.copy(isRecording = false, recordingElapsed = null, mediaError = state.error.toUiText())
            }
        }
    }

    private fun onPlaybackState(state: VoicePlaybackState) {
        _uiState.update { it.copy(isPlayingVoice = state is VoicePlaybackState.Playing) }
    }

    /** A draft only ever describes pending (not-yet-committed) media — see its own KDoc. */
    private fun currentDraft(): CompletionMemoryDraft {
        val state = _uiState.value
        return CompletionMemoryDraft(
            completionId = completionId,
            note = state.note,
            reactions = state.reactions,
            pendingPhoto = pendingPhotoReplacement,
            pendingVoice = pendingVoiceReplacement,
        )
    }

    private fun buildSaveCommand(): SaveCompletionMemoryCommand {
        val state = _uiState.value
        val photoEdit = when {
            pendingPhotoReplacement != null -> MediaEdit.Replace(pendingPhotoReplacement!!)
            photoExplicitlyRemoved -> MediaEdit.Remove
            else -> MediaEdit.Unchanged
        }
        val voiceEdit = when {
            pendingVoiceReplacement != null -> MediaEdit.Replace(pendingVoiceReplacement!!)
            voiceExplicitlyRemoved -> MediaEdit.Remove
            else -> MediaEdit.Unchanged
        }
        return SaveCompletionMemoryCommand(
            completionId = completionId,
            note = state.note,
            reactions = state.reactions,
            photo = photoEdit,
            voice = voiceEdit,
        )
    }

    private fun hasUnsavedChanges(): Boolean {
        val state = _uiState.value
        return state.note != originalNote ||
            state.reactions != originalReactions ||
            pendingPhotoReplacement != null ||
            pendingVoiceReplacement != null ||
            photoExplicitlyRemoved ||
            voiceExplicitlyRemoved
    }
}

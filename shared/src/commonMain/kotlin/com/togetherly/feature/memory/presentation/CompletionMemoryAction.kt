package com.togetherly.feature.memory.presentation

import com.togetherly.core.media.MicrophonePermissionResult
import com.togetherly.data.media.PhotoPickerResult
import com.togetherly.domain.completion.FamilyReaction

sealed interface CompletionMemoryAction {
    data object ScreenStarted : CompletionMemoryAction
    data class NoteChanged(val value: String) : CompletionMemoryAction
    data class ReactionToggled(val reaction: FamilyReaction) : CompletionMemoryAction
    data object AddPhotoClicked : CompletionMemoryAction
    data class PhotoImportCompleted(val result: PhotoPickerResult) : CompletionMemoryAction
    data object RemovePhotoClicked : CompletionMemoryAction
    data object RecordVoiceClicked : CompletionMemoryAction
    data class MicrophonePermissionResultReceived(
        val result: MicrophonePermissionResult,
    ) : CompletionMemoryAction
    data object StopRecordingClicked : CompletionMemoryAction
    data object CancelRecordingClicked : CompletionMemoryAction
    data object PlayVoiceClicked : CompletionMemoryAction
    data object PauseVoiceClicked : CompletionMemoryAction
    data object RemoveVoiceClicked : CompletionMemoryAction
    data object SaveClicked : CompletionMemoryAction
    data object SkipClicked : CompletionMemoryAction
    data object BackClicked : CompletionMemoryAction
    data object DiscardConfirmed : CompletionMemoryAction
    data object DialogDismissed : CompletionMemoryAction
}

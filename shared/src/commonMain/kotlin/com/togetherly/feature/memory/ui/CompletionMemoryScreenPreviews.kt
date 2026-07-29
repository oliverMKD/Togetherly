package com.togetherly.feature.memory.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.core.ui.UiText
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.FamilyReaction
import com.togetherly.feature.memory.model.CompletionMemoryUiState
import com.togetherly.feature.memory.model.MemoryPhotoUi
import com.togetherly.feature.memory.model.MemoryVoiceUi
import com.togetherly.feature.memory.model.PhotoPreviewReference
import com.togetherly.feature.memory.model.VoicePreviewReference
import com.togetherly.domain.completion.MediaReference
import kotlinx.collections.immutable.persistentSetOf
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.error_generic_message

private val PREVIEW_COMPLETION_ID = CompletionId("preview-completion")

private fun previewState(
    note: String = "",
    reactions: Set<FamilyReaction> = emptySet(),
    photo: MemoryPhotoUi? = null,
    voice: MemoryVoiceUi? = null,
    isRecording: Boolean = false,
    recordingElapsed: String? = null,
    isPlayingVoice: Boolean = false,
    isSaving: Boolean = false,
    mediaError: UiText? = null,
    microphonePermissionPermanentlyDenied: Boolean = false,
    saveError: UiText? = null,
    showDiscardConfirmation: Boolean = false,
) = CompletionMemoryUiState(
    completionId = PREVIEW_COMPLETION_ID,
    questTitle = "Build a blanket fort",
    note = note,
    reactions = reactions.toList().let { persistentSetOf(*it.toTypedArray()) },
    photo = photo,
    voice = voice,
    isRecording = isRecording,
    recordingElapsed = recordingElapsed,
    isPlayingVoice = isPlayingVoice,
    isSaving = isSaving,
    mediaError = mediaError,
    microphonePermissionPermanentlyDenied = microphonePermissionPermanentlyDenied,
    saveError = saveError,
    showDiscardConfirmation = showDiscardConfirmation,
)

@Composable
private fun PreviewScreen(state: CompletionMemoryUiState, darkTheme: Boolean = false) {
    TogetherlyTheme(darkTheme = darkTheme) {
        CompletionMemoryScreen(state = state, onAction = {}, loadPhotoBytes = { null })
    }
}

@Preview
@Composable
private fun EmptyDraftPreview() {
    PreviewScreen(previewState())
}

@Preview
@Composable
private fun ReactionsSelectedPreview() {
    PreviewScreen(previewState(reactions = setOf(FamilyReaction.HAPPY, FamilyReaction.SILLY)))
}

@Preview
@Composable
private fun NotePreview() {
    PreviewScreen(previewState(note = "We built a fort out of every blanket in the house."))
}

@Preview
@Composable
private fun PhotoAddedPreview() {
    PreviewScreen(previewState(photo = MemoryPhotoUi(PhotoPreviewReference.Committed(MediaReference("completions/c/photo-1.jpg")))))
}

@Preview
@Composable
private fun RecordingPreview() {
    PreviewScreen(previewState(isRecording = true, recordingElapsed = "0:12"))
}

@Preview
@Composable
private fun RecordedVoicePreview() {
    PreviewScreen(
        previewState(
            voice = MemoryVoiceUi(VoicePreviewReference.Committed(MediaReference("completions/c/voice-1.m4a")), "0:34"),
        ),
    )
}

@Preview
@Composable
private fun PermissionDeniedPreview() {
    PreviewScreen(previewState(mediaError = UiText.Resource(Res.string.error_generic_message)))
}

@Preview
@Composable
private fun PermissionPermanentlyDeniedPreview() {
    PreviewScreen(
        previewState(
            mediaError = UiText.Resource(Res.string.error_generic_message),
            microphonePermissionPermanentlyDenied = true,
        ),
    )
}

@Preview
@Composable
private fun SavingPreview() {
    PreviewScreen(previewState(isSaving = true))
}

@Preview
@Composable
private fun SaveFailurePreview() {
    PreviewScreen(previewState(saveError = UiText.Resource(Res.string.error_generic_message)))
}

@Preview
@Composable
private fun DiscardConfirmationPreview() {
    PreviewScreen(previewState(note = "A partial note", showDiscardConfirmation = true))
}

@Preview
@Composable
private fun MixedMemoryLightPreview() {
    PreviewScreen(
        darkTheme = false,
        state = previewState(
            note = "So much laughing today.",
            reactions = setOf(FamilyReaction.LOVED_IT, FamilyReaction.SURPRISED),
            photo = MemoryPhotoUi(PhotoPreviewReference.Committed(MediaReference("completions/c/photo-1.jpg"))),
            voice = MemoryVoiceUi(VoicePreviewReference.Committed(MediaReference("completions/c/voice-1.m4a")), "0:18"),
        ),
    )
}

@Preview
@Composable
private fun MixedMemoryDarkPreview() {
    PreviewScreen(
        darkTheme = true,
        state = previewState(
            note = "So much laughing today.",
            reactions = setOf(FamilyReaction.LOVED_IT, FamilyReaction.SURPRISED),
            photo = MemoryPhotoUi(PhotoPreviewReference.Committed(MediaReference("completions/c/photo-1.jpg"))),
            voice = MemoryVoiceUi(VoicePreviewReference.Committed(MediaReference("completions/c/voice-1.m4a")), "0:18"),
        ),
    )
}

package com.togetherly.feature.memory.model

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.FamilyReaction
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf

/**
 * [microphonePermissionPermanentlyDenied] is the one field beyond this screen's original spec
 * snippet (Step 10.7 privacy/accessibility audit): [mediaError] alone can't tell the UI *which*
 * failure it's displaying, so "Open Settings" (the only recourse once permission is permanently
 * denied — see [com.togetherly.core.media.MicrophonePermissionResult.PermanentlyDenied]'s own
 * KDoc) had a string defined since Step 10.4 (`memory_voice_open_settings_action`) but no button
 * ever wired to it. This flag is what that button now keys off.
 */
@Immutable
data class CompletionMemoryUiState(
    val completionId: CompletionId,
    val questTitle: String? = null,
    val note: String = "",
    val reactions: PersistentSet<FamilyReaction> = persistentSetOf(),
    val photo: MemoryPhotoUi? = null,
    val voice: MemoryVoiceUi? = null,
    /**
     * Loaded once, before capture options are presented (Step 13.5) — defaulting to `true` so a
     * screen state built before that load resolves (or a preview that never sets these) still shows
     * every capture option, matching [com.togetherly.domain.family.MemoryPreferences.defaults].
     * `false` hides (never merely disables) the corresponding section in [com.togetherly.feature.memory.ui.CompletionMemoryScreen]
     * — an already-captured note/photo/voice from before the family disabled it is never touched by
     * this flag; it only ever governs what's offered for a *new* capture.
     */
    val allowPhotos: Boolean = true,
    val allowVoiceMemories: Boolean = true,
    val allowTextNotes: Boolean = true,
    val isRecording: Boolean = false,
    val recordingElapsed: String? = null,
    val isPlayingVoice: Boolean = false,
    val isSaving: Boolean = false,
    val noteError: UiText? = null,
    val mediaError: UiText? = null,
    val microphonePermissionPermanentlyDenied: Boolean = false,
    val saveError: UiText? = null,
    val showDiscardConfirmation: Boolean = false,
)

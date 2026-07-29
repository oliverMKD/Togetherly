package com.togetherly.feature.memory.model

import androidx.compose.runtime.Immutable
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.PendingMediaReference

@Immutable
data class MemoryVoiceUi(
    val reference: VoicePreviewReference,
    val durationLabel: String,
)

/** See [PhotoPreviewReference][com.togetherly.feature.memory.model.PhotoPreviewReference]'s own KDoc — same reasoning, for voice. */
sealed interface VoicePreviewReference {
    data class Pending(val reference: PendingMediaReference) : VoicePreviewReference
    data class Committed(val reference: MediaReference) : VoicePreviewReference
}

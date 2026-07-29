package com.togetherly.feature.journey.model

import androidx.compose.runtime.Immutable
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMediaId

/** [mediaId] is how [com.togetherly.feature.journey.model.JourneyUiState.Content.playingVoiceId] identifies which entry's clip is active — a completion has at most one voice memory, but a timeline has many completions. */
@Immutable
data class VoiceMemoryUi(
    val mediaId: MemoryMediaId,
    val reference: MediaReference,
    val durationLabel: String,
)

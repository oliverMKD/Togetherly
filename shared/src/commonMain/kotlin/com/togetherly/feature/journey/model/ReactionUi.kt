package com.togetherly.feature.journey.model

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText

/** A resolved [com.togetherly.domain.completion.FamilyReaction] — label and emoji already mapped, so the timeline never imports the domain enum directly. */
@Immutable
data class ReactionUi(
    val label: UiText,
    val emoji: String,
)

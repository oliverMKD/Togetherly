package com.togetherly.feature.memory.mapper

import com.togetherly.core.ui.UiText
import com.togetherly.domain.completion.FamilyReaction
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.memory_reaction_calm
import togetherly.shared.generated.resources.memory_reaction_happy
import togetherly.shared.generated.resources.memory_reaction_loved_it
import togetherly.shared.generated.resources.memory_reaction_silly
import togetherly.shared.generated.resources.memory_reaction_surprised

fun FamilyReaction.label(): UiText = UiText.Resource(
    when (this) {
        FamilyReaction.HAPPY -> Res.string.memory_reaction_happy
        FamilyReaction.SILLY -> Res.string.memory_reaction_silly
        FamilyReaction.LOVED_IT -> Res.string.memory_reaction_loved_it
        FamilyReaction.CALM -> Res.string.memory_reaction_calm
        FamilyReaction.SURPRISED -> Res.string.memory_reaction_surprised
    },
)

/** Decorative presentation only — emoji never belongs on the domain enum itself. */
fun FamilyReaction.emoji(): String = when (this) {
    FamilyReaction.HAPPY -> "😊"
    FamilyReaction.SILLY -> "😄"
    FamilyReaction.LOVED_IT -> "🥰"
    FamilyReaction.CALM -> "😌"
    FamilyReaction.SURPRISED -> "😲"
}

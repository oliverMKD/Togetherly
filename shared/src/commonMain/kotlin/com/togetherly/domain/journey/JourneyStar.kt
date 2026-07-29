package com.togetherly.domain.journey

import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.validation.requireInRange
import kotlin.time.Instant

/**
 * One star per completed quest, always — see [JourneyStarPolicy] for why [hasNote]/[hasPhoto]/
 * [hasVoice] enrich a star but never gate its existence. Deliberately has no persisted identity
 * of its own: [completionId] *is* the star's identity, so a star is never stored, only derived
 * fresh from a [JourneyEntry] each time (see [JourneyRepository][com.togetherly.domain.journey.repository.JourneyRepository]'s
 * own KDoc on why there is no `journey_star` table). [category] is null exactly when
 * [JourneyEntry.quest] failed to resolve; the star still exists. Holds no Compose `Color`/`Dp` —
 * those are a UI-layer concern (Step 10.6), resolved from [category]/[visualVariant] there, never
 * baked into this read model.
 */
data class JourneyStar(
    val completionId: CompletionId,
    val questId: QuestId,
    val category: QuestCategory?,
    val completedAt: Instant,
    val position: StarPosition,
    val visualVariant: StarVisualVariant,
    val hasNote: Boolean,
    val hasPhoto: Boolean,
    val hasVoice: Boolean,
)

/**
 * Normalized `0f..1f` coordinates within a constellation's drawing area — never a pixel or `Dp`
 * value, so the same [JourneyStar] can be laid out at any screen size. [JourneyStarPolicy] keeps
 * its generated positions within a tighter safe margin than this full range allows; this
 * constructor only enforces the outer bound every [StarPosition] must satisfy.
 */
data class StarPosition(
    val x: Float,
    val y: Float,
) {
    init {
        requireInRange(x, 0f..1f)
        requireInRange(y, 0f..1f)
    }
}

/** A small, stable set of relative sizes — never a pixel/`Dp` value; the UI layer maps this to an actual size. */
enum class StarVisualVariant {
    SMALL,
    MEDIUM,
    LARGE,
}

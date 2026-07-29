package com.togetherly.domain.journey

import com.togetherly.domain.completion.MemoryMedia

/** Positions stay clear of the outer edge of a constellation's drawing area. */
internal val STAR_SAFE_MARGIN = 0.08f..0.92f

/**
 * Derives one [JourneyStar] per [JourneyEntry], deterministically — the same [entry] (by
 * [com.togetherly.domain.completion.QuestCompletion.id]) always produces the exact same
 * [JourneyStar], on any platform, on any run, since every derived value comes only from
 * [stableHash] over the completion ID, never from [kotlin.random.Random] or a platform hash.
 * [JourneyConstellationPolicy] builds on this for a whole collection; call this directly only
 * when a single star (no collision-avoidance pass) is genuinely enough.
 */
class JourneyStarPolicy {
    fun create(entry: JourneyEntry): JourneyStar {
        val completion = entry.completion
        val seed = completion.id.value

        return JourneyStar(
            completionId = completion.id,
            questId = completion.questId,
            category = entry.quest?.category,
            completedAt = completion.completedAt,
            position = StarPosition(
                x = positionFraction(seed, axis = "x"),
                y = positionFraction(seed, axis = "y"),
            ),
            visualVariant = StarVisualVariant.entries[
                (stableHash("$seed|variant") % StarVisualVariant.entries.size.toUInt()).toInt()
            ],
            hasNote = completion.note != null,
            hasPhoto = completion.media.any { it is MemoryMedia.Photo },
            hasVoice = completion.media.any { it is MemoryMedia.Voice },
        )
    }
}

internal fun positionFraction(seed: String, axis: String): Float {
    val fraction = stableHash("$seed|$axis").toUnitFraction()
    return STAR_SAFE_MARGIN.start + fraction * (STAR_SAFE_MARGIN.endInclusive - STAR_SAFE_MARGIN.start)
}

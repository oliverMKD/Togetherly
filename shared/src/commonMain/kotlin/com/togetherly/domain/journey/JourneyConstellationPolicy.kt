package com.togetherly.domain.journey

import kotlin.math.sqrt

/**
 * The constellation overview only ever shows the most recent stars — a family with a long
 * history still gets a readable sky, not one star per completion ever made. The full timeline
 * (Step 10.6) is unaffected: it reads [JourneyEntry] directly, never through this cap.
 */
internal const val MAX_CONSTELLATION_STARS = 40

/** Positions this close together are nudged apart — see [resolveCollisions]. */
internal const val MINIMUM_SEPARATION = 0.05f

private const val MAX_COLLISION_ATTEMPTS = 8

/**
 * Arranges the most recent [JourneyEntry] items into a constellation, deterministically. Star
 * identity and position never depend on the order [entries] is passed in: recency selection
 * breaks completedAt ties by completion ID, and collision resolution always processes candidates
 * in completion-ID order internally — only the *returned* list is newest-first, for callers'
 * convenience. Collision avoidance is intentionally simple (see [resolveCollisions]): a handful of
 * deterministic nudges away from anything already placed, not a physics simulation.
 */
class JourneyConstellationPolicy(
    private val starPolicy: JourneyStarPolicy = JourneyStarPolicy(),
) {
    fun arrange(entries: List<JourneyEntry>): List<JourneyStar> {
        val recent = entries
            .sortedWith(compareByDescending<JourneyEntry> { it.completedAt }.thenBy { it.completion.id.value })
            .take(MAX_CONSTELLATION_STARS)

        val canonicalOrder = recent.sortedBy { it.completion.id.value }
        val resolvedById = resolveCollisions(canonicalOrder.map(starPolicy::create)).associateBy { it.completionId }

        return recent.map { entry -> resolvedById.getValue(entry.completion.id) }
    }
}

/**
 * Nudges each star, in order, away from every star already placed before it — deterministic
 * because both the processing order (the order [stars] is given in) and each nudge (derived from
 * [stableHash] over the star's own completion ID and attempt number) are pure functions of the
 * input. [JourneyConstellationPolicy.arrange] always calls this with candidates pre-sorted by
 * completion ID, so the result never depends on caller-supplied ordering.
 */
internal fun resolveCollisions(stars: List<JourneyStar>): List<JourneyStar> {
    val placed = mutableListOf<JourneyStar>()
    for (star in stars) {
        var candidate = star
        var attempt = 0
        while (placed.any { distance(it.position, candidate.position) < MINIMUM_SEPARATION } && attempt < MAX_COLLISION_ATTEMPTS) {
            attempt += 1
            candidate = candidate.copy(position = nudgedPosition(candidate, attempt))
        }
        placed += candidate
    }
    return placed
}

/**
 * `dx`/`dy` prefix the seed rather than suffix it — see [positionFraction]'s KDoc for why hashing
 * two strings that differ only in their final character (here, "dx" vs "dy") produces hashes that
 * are always exactly `FNV_PRIME` apart, which would nudge every colliding star in a near-identical
 * diagonal direction instead of a genuinely varied one.
 */
private fun nudgedPosition(star: JourneyStar, attempt: Int): StarPosition {
    val seed = "${star.completionId.value}|nudge|$attempt"
    val dx = (stableHash("dx|$seed").toUnitFraction() - 0.5f) * 2 * MINIMUM_SEPARATION
    val dy = (stableHash("dy|$seed").toUnitFraction() - 0.5f) * 2 * MINIMUM_SEPARATION
    return StarPosition(
        x = (star.position.x + dx).coerceIn(STAR_SAFE_MARGIN),
        y = (star.position.y + dy).coerceIn(STAR_SAFE_MARGIN),
    )
}

private fun distance(a: StarPosition, b: StarPosition): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

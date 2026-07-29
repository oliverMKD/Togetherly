package com.togetherly.domain.journey

import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.validQuestCompletion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class JourneyConstellationPolicyTest {

    private val policy = JourneyConstellationPolicy()

    private fun entryAt(id: String, completedAt: Instant) =
        JourneyEntry(validQuestCompletion(id = CompletionId(id), startedAt = null, completedAt = completedAt), quest = null)

    @Test
    fun everyEntryProducesAStarWhenUnderTheCap() {
        val entries = (1..5).map { entryAt("c$it", Instant.fromEpochSeconds(it.toLong() * 86_400)) }

        val stars = policy.arrange(entries)

        assertEquals(5, stars.size)
    }

    @Test
    fun overviewIsLimitedToTheMostRecentStars() {
        val entries = (1..60).map { index ->
            entryAt("c$index", Instant.fromEpochSeconds(index.toLong() * 86_400))
        }

        val stars = policy.arrange(entries)

        assertEquals(MAX_CONSTELLATION_STARS, stars.size)
        val expectedMostRecentIds = (60 downTo 60 - MAX_CONSTELLATION_STARS + 1).map { "c$it" }.toSet()
        assertEquals(expectedMostRecentIds, stars.map { it.completionId.value }.toSet())
    }

    @Test
    fun inputOrderingDoesNotChangeStarIdentityOrPosition() {
        val entries = (1..10).map { entryAt("c$it", Instant.fromEpochSeconds(it.toLong() * 86_400)) }

        val forward = policy.arrange(entries).associateBy { it.completionId }
        val shuffled = policy.arrange(entries.reversed()).associateBy { it.completionId }

        assertEquals(forward.keys, shuffled.keys)
        forward.forEach { (id, star) -> assertEquals(star.position, shuffled.getValue(id).position) }
    }

    @Test
    fun deletedCompletionHasNoDerivedStar() {
        val entries = (1..5).map { entryAt("c$it", Instant.fromEpochSeconds(it.toLong() * 86_400)) }

        val before = policy.arrange(entries)
        val after = policy.arrange(entries.filterNot { it.completion.id == CompletionId("c3") })

        assertTrue(before.any { it.completionId == CompletionId("c3") })
        assertTrue(after.none { it.completionId == CompletionId("c3") })
    }

    @Test
    fun collisionResolutionNudgesOverlappingStarsApartDeterministically() {
        val samePosition = StarPosition(0.5f, 0.5f)
        val stars = listOf(
            starAt("c1", samePosition),
            starAt("c2", samePosition),
        )

        val resolved = resolveCollisions(stars)

        assertEquals(samePosition, resolved[0].position)
        assertTrue(resolved[1].position != samePosition)
        assertTrue(distanceBetween(resolved[0].position, resolved[1].position) >= MINIMUM_SEPARATION)

        val resolvedAgain = resolveCollisions(stars)
        assertEquals(resolved.map { it.position }, resolvedAgain.map { it.position })
    }

    private fun starAt(id: String, position: StarPosition) = JourneyStar(
        completionId = CompletionId(id),
        questId = com.togetherly.domain.quest.QuestId("quest-1"),
        category = null,
        completedAt = Instant.fromEpochSeconds(0),
        position = position,
        visualVariant = StarVisualVariant.SMALL,
        hasNote = false,
        hasPhoto = false,
        hasVoice = false,
    )

    private fun distanceBetween(a: StarPosition, b: StarPosition): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}

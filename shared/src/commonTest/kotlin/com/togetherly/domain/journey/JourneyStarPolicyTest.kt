package com.togetherly.domain.journey

import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.MemoryNote
import com.togetherly.domain.completion.validQuestCompletion
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.validFamilyQuest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class JourneyStarPolicyTest {

    private val policy = JourneyStarPolicy()

    @Test
    fun everyCompletionProducesAStar() {
        val entry = JourneyEntry(validQuestCompletion(id = CompletionId("c1")), quest = null)

        val star = policy.create(entry)

        assertEquals(CompletionId("c1"), star.completionId)
    }

    @Test
    fun completionWithoutMemoryStillProducesAStarWithNoFlagsSet() {
        val entry = JourneyEntry(validQuestCompletion(id = CompletionId("c1")), quest = null)

        val star = policy.create(entry)

        assertFalse(star.hasNote)
        assertFalse(star.hasPhoto)
        assertFalse(star.hasVoice)
    }

    @Test
    fun memoryFlagsReflectPresentContent() {
        val completion = validQuestCompletion(
            id = CompletionId("c1"),
            note = MemoryNote("A great day"),
            media = listOf(
                MemoryMedia.Photo(MemoryMediaId("media-1"), MediaReference("photo-1")),
                MemoryMedia.Voice(MemoryMediaId("media-2"), MediaReference("voice-1"), 8.seconds),
            ),
        )
        val star = policy.create(JourneyEntry(completion, quest = null))

        assertTrue(star.hasNote)
        assertTrue(star.hasPhoto)
        assertTrue(star.hasVoice)
    }

    @Test
    fun missingQuestProducesNullCategory() {
        val star = policy.create(JourneyEntry(validQuestCompletion(id = CompletionId("c1")), quest = null))

        assertNull(star.category)
    }

    @Test
    fun resolvedQuestProducesItsCategory() {
        val quest = validFamilyQuest(id = QuestId("quest-1"), category = QuestCategory.KINDNESS)
        val star = policy.create(
            JourneyEntry(validQuestCompletion(id = CompletionId("c1"), questId = quest.id), quest = quest),
        )

        assertEquals(QuestCategory.KINDNESS, star.category)
    }

    @Test
    fun sameCompletionIdProducesTheSamePosition() {
        val first = policy.create(JourneyEntry(validQuestCompletion(id = CompletionId("stable-id")), quest = null))
        val second = policy.create(JourneyEntry(validQuestCompletion(id = CompletionId("stable-id")), quest = null))

        assertEquals(first.position, second.position)
        assertEquals(first.visualVariant, second.visualVariant)
    }

    @Test
    fun positionRemainsWithinTheSafeMargin() {
        repeat(50) { index ->
            val star = policy.create(JourneyEntry(validQuestCompletion(id = CompletionId("completion-$index")), quest = null))
            assertTrue(star.position.x in STAR_SAFE_MARGIN, "x=${star.position.x} out of bounds")
            assertTrue(star.position.y in STAR_SAFE_MARGIN, "y=${star.position.y} out of bounds")
        }
    }

    /**
     * Golden fixture: [stableHash]'s exact output for these seeds, computed independently
     * (outside Kotlin, from the same documented FNV-1a algorithm) — a change to the hash
     * algorithm that would silently move every family's stars must fail this test.
     */
    @Test
    fun stableHashMatchesTheDocumentedFnv1aAlgorithm() {
        assertEquals(4122696995u, stableHash("completion-golden|x"))
        assertEquals(4105919376u, stableHash("completion-golden|y"))
        assertEquals(818103658u, stableHash("completion-golden|variant"))
    }

    @Test
    fun sameFixtureProducesTheExpectedStar() {
        val star = policy.create(JourneyEntry(validQuestCompletion(id = CompletionId("completion-golden")), quest = null))

        assertEquals(0.886f, star.position.x, absoluteTolerance = 0.001f)
        assertEquals(0.883f, star.position.y, absoluteTolerance = 0.001f)
        assertEquals(StarVisualVariant.MEDIUM, star.visualVariant)
    }
}

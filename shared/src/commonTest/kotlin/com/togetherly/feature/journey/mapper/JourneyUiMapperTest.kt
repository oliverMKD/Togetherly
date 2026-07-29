package com.togetherly.feature.journey.mapper

import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.FamilyReaction
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.MemoryNote
import com.togetherly.domain.completion.validQuestCompletion
import com.togetherly.domain.journey.JourneyEntry
import com.togetherly.domain.journey.JourneyMilestone
import com.togetherly.domain.journey.JourneyStar
import com.togetherly.domain.journey.StarPosition
import com.togetherly.domain.journey.StarVisualVariant
import com.togetherly.domain.journey.deriveJourneySummary
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.validFamilyQuest
import com.togetherly.feature.today.model.QuestCategoryUi
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class JourneyUiMapperTest {

    @Test
    fun resolvedQuestMapsTitleAndCategory() {
        val quest = validFamilyQuest(id = QuestId("quest-1"), category = QuestCategory.MOVE)
        val completion = validQuestCompletion(id = CompletionId("c1"), questId = quest.id, completedAt = Instant.parse("2026-06-15T09:05:00Z"))
        val entryUi = JourneyEntry(completion, quest).toUi(TimeZone.UTC)

        assertEquals(quest.title.value, entryUi.questTitle)
        assertEquals(QuestCategoryUi.MOVE, entryUi.category)
        assertEquals("June 15, 2026", entryUi.completedDate)
        assertEquals("9:05 AM", entryUi.completedTime)
    }

    @Test
    fun missingQuestMapsToNullTitleAndCategory() {
        val completion = validQuestCompletion(id = CompletionId("c1"))
        val entryUi = JourneyEntry(completion, quest = null).toUi(TimeZone.UTC)

        assertNull(entryUi.questTitle)
        assertNull(entryUi.category)
    }

    @Test
    fun reactionsAreMappedWithLabelAndEmoji() {
        val completion = validQuestCompletion(id = CompletionId("c1"), reactions = setOf(FamilyReaction.HAPPY))
        val entryUi = JourneyEntry(completion, quest = null).toUi(TimeZone.UTC)

        val reaction = entryUi.reactions.single()
        assertEquals("😊", reaction.emoji)
    }

    @Test
    fun photoAndVoiceMediaAreMapped() {
        val photo = MemoryMedia.Photo(MemoryMediaId("media-1"), MediaReference("completions/c1/photo-1.jpg"))
        val voice = MemoryMedia.Voice(MemoryMediaId("media-2"), MediaReference("completions/c1/voice-1.m4a"), 12.seconds)
        val completion = validQuestCompletion(id = CompletionId("c1"), media = listOf(photo, voice))
        val entryUi = JourneyEntry(completion, quest = null).toUi(TimeZone.UTC)

        assertEquals(photo.localReference, entryUi.photo?.reference)
        assertEquals(voice.id, entryUi.voice?.mediaId)
        assertEquals("0:12", entryUi.voice?.durationLabel)
    }

    @Test
    fun noteIsPassedThroughAsPlainText() {
        val completion = validQuestCompletion(id = CompletionId("c1"), note = MemoryNote("What a day"))
        val entryUi = JourneyEntry(completion, quest = null).toUi(TimeZone.UTC)

        assertEquals("What a day", entryUi.note)
    }

    @Test
    fun starMapperReusesDomainPositionAndVariant() {
        val star = JourneyStar(
            completionId = CompletionId("c1"),
            questId = QuestId("quest-1"),
            category = QuestCategory.SILLY,
            completedAt = Instant.fromEpochSeconds(0),
            position = StarPosition(0.4f, 0.6f),
            visualVariant = StarVisualVariant.LARGE,
            hasNote = true,
            hasPhoto = false,
            hasVoice = true,
        )

        val starUi = star.toUi()

        assertEquals(StarPosition(0.4f, 0.6f), starUi.position)
        assertEquals(StarVisualVariant.LARGE, starUi.visualVariant)
        assertEquals(QuestCategoryUi.SILLY, starUi.category)
        assertEquals(true, starUi.hasNote)
        assertEquals(false, starUi.hasPhoto)
        assertEquals(true, starUi.hasVoice)
    }

    @Test
    fun summaryMapperIncludesAchievedMilestones() {
        val entries = (1..3).map { JourneyEntry(validQuestCompletion(id = CompletionId("c$it")), quest = null) }
        val summaryUi = deriveJourneySummary(entries, TimeZone.UTC).toUi()

        assertEquals(3, summaryUi.totalCompletions)
        assertEquals(setOf(JourneyMilestone.FIRST_STAR, JourneyMilestone.THREE_STARS), summaryUi.achievedMilestones)
    }

    @Test
    fun latestOrNullPicksTheHighestMilestone() {
        val achieved = setOf(JourneyMilestone.FIRST_STAR, JourneyMilestone.THREE_STARS, JourneyMilestone.SEVEN_STARS)
        assertEquals(JourneyMilestone.SEVEN_STARS, achieved.latestOrNull())
    }

    @Test
    fun latestOrNullIsNullWhenNoMilestoneAchieved() {
        assertNull(emptySet<JourneyMilestone>().latestOrNull())
    }
}

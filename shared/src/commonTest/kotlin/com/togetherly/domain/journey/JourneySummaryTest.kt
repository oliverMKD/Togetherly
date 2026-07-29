package com.togetherly.domain.journey

import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.validQuestCompletion
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.validFamilyQuest
import com.togetherly.domain.validation.DomainValidationException
import com.togetherly.domain.validation.DomainValidationReason
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JourneySummaryTest {

    @Test
    fun summaryCountsValidCategories() {
        val moveQuest = validFamilyQuest(id = QuestId("quest-move"), category = QuestCategory.MOVE)
        val createQuest = validFamilyQuest(id = QuestId("quest-create"), category = QuestCategory.CREATE)
        val entries = listOf(
            JourneyEntry(validQuestCompletion(id = CompletionId("c1"), questId = moveQuest.id), moveQuest),
            JourneyEntry(validQuestCompletion(id = CompletionId("c2"), questId = moveQuest.id), moveQuest),
            JourneyEntry(validQuestCompletion(id = CompletionId("c3"), questId = createQuest.id), createQuest),
            JourneyEntry(validQuestCompletion(id = CompletionId("c4"), questId = QuestId("missing")), null),
        )

        val summary = deriveJourneySummary(entries, timeZone = TimeZone.UTC)

        assertEquals(4, summary.totalCompletions)
        assertEquals(2, summary.completionsByCategory[QuestCategory.MOVE])
        assertEquals(1, summary.completionsByCategory[QuestCategory.CREATE])
    }

    @Test
    fun summaryPreventsNegativeCounts() {
        val exception = assertFailsWith<DomainValidationException> {
            JourneySummary(
                totalCompletions = -1,
                completionsByCategory = emptyMap(),
                activeDays = emptySet(),
                memoriesWithPhoto = 0,
                memoriesWithVoice = 0,
            )
        }
        assertEquals(DomainValidationReason.NON_POSITIVE_VALUE, exception.reason)
    }

    @Test
    fun summaryCountsPhotoAndVoiceMemories() {
        val questId = QuestId("quest-1")
        val entries = listOf(
            JourneyEntry(
                validQuestCompletion(
                    id = CompletionId("c1"),
                    questId = questId,
                    media = listOf(
                        com.togetherly.domain.completion.MemoryMedia.Photo(
                            id = com.togetherly.domain.completion.MemoryMediaId("media-1"),
                            localReference = com.togetherly.domain.completion.MediaReference("photo-1"),
                        ),
                    ),
                ),
                quest = null,
            ),
            JourneyEntry(
                validQuestCompletion(
                    id = CompletionId("c2"),
                    questId = questId,
                    media = listOf(
                        com.togetherly.domain.completion.MemoryMedia.Voice(
                            id = com.togetherly.domain.completion.MemoryMediaId("media-2"),
                            localReference = com.togetherly.domain.completion.MediaReference("voice-1"),
                            duration = kotlin.time.Duration.parse("PT8S"),
                        ),
                    ),
                ),
                quest = null,
            ),
            JourneyEntry(validQuestCompletion(id = CompletionId("c3"), questId = questId), quest = null),
        )

        val summary = deriveJourneySummary(entries, timeZone = TimeZone.UTC)

        assertEquals(1, summary.memoriesWithPhoto)
        assertEquals(1, summary.memoriesWithVoice)
    }
}

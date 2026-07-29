package com.togetherly.domain.journey

import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.validQuestCompletion
import com.togetherly.domain.quest.validFamilyQuest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class JourneyEntryTest {

    @Test
    fun completionIsPreservedWhenQuestIsResolved() {
        val completion = validQuestCompletion()
        val quest = validFamilyQuest()

        val entry = JourneyEntry(completion = completion, quest = quest)

        assertEquals(completion, entry.completion)
        assertEquals(quest, entry.quest)
    }

    @Test
    fun completionRemainsVisibleWhenQuestIsNull() {
        val completion = validQuestCompletion()

        val entry = JourneyEntry(completion = completion, quest = null)

        assertEquals(completion, entry.completion)
        assertEquals(null, entry.quest)
    }

    @Test
    fun completionOrderingIsDeterministic() {
        val early = JourneyEntry(
            completion = validQuestCompletion(
                id = CompletionId("completion-1"),
                completedAt = Instant.parse("2026-06-15T09:00:00Z"),
            ),
            quest = null,
        )
        val late = JourneyEntry(
            completion = validQuestCompletion(
                id = CompletionId("completion-2"),
                completedAt = Instant.parse("2026-06-15T10:00:00Z"),
            ),
            quest = null,
        )

        val sorted = listOf(early, late).sortedByDescending { it.completedAt }

        assertEquals(listOf(late, early), sorted)
    }
}

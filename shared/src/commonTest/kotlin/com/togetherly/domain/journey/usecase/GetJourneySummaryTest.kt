package com.togetherly.domain.journey.usecase

import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.validQuestCompletion
import com.togetherly.domain.journey.JourneyEntry
import com.togetherly.domain.journey.repository.FakeJourneyRepository
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.validFamilyQuest
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class GetJourneySummaryTest {

    @Test
    fun summaryCountsCompletions() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"), category = QuestCategory.MOVE)
        val repository = FakeJourneyRepository().apply {
            setEntries(
                listOf(
                    JourneyEntry(validQuestCompletion(id = CompletionId("c1"), questId = quest.id), quest),
                    JourneyEntry(validQuestCompletion(id = CompletionId("c2"), questId = quest.id), quest),
                ),
            )
        }
        val useCase = GetJourneySummary(repository)

        val result = useCase(timeZone = TimeZone.UTC)

        val summary = (result as DataResult.Success).value
        assertEquals(2, summary.totalCompletions)
        assertEquals(2, summary.completionsByCategory[QuestCategory.MOVE])
    }

    @Test
    fun missingQuestContentDoesNotRemoveCompletionTotal() = runTest {
        val repository = FakeJourneyRepository().apply {
            setEntries(
                listOf(
                    JourneyEntry(validQuestCompletion(id = CompletionId("c1")), quest = null),
                ),
            )
        }
        val useCase = GetJourneySummary(repository)

        val result = useCase(timeZone = TimeZone.UTC)

        val summary = (result as DataResult.Success).value
        assertEquals(1, summary.totalCompletions)
        assertEquals(emptyMap(), summary.completionsByCategory)
    }

    @Test
    fun activeDatesUseTheSuppliedTimezone() = runTest {
        val completion = validQuestCompletion(
            id = CompletionId("c1"),
            completedAt = kotlin.time.Instant.parse("2026-06-15T23:30:00Z"),
        )
        val repository = FakeJourneyRepository().apply {
            setEntries(listOf(JourneyEntry(completion, quest = null)))
        }
        val useCase = GetJourneySummary(repository)

        val utcSummary = (useCase(timeZone = TimeZone.UTC) as DataResult.Success).value
        val tokyoSummary = (useCase(timeZone = TimeZone.of("UTC+9")) as DataResult.Success).value

        assertEquals(setOf(kotlinx.datetime.LocalDate(2026, 6, 15)), utcSummary.activeDays)
        assertEquals(setOf(kotlinx.datetime.LocalDate(2026, 6, 16)), tokyoSummary.activeDays)
    }
}

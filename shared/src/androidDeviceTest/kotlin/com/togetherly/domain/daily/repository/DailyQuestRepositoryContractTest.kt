package com.togetherly.domain.daily.repository

import com.togetherly.core.result.DataResult
import com.togetherly.data.testDailyQuest
import com.togetherly.data.testDismissedQuest
import com.togetherly.domain.quest.QuestId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * A shared behavioral contract every [DailyQuestRepository] implementation must satisfy, run
 * against both [FakeDailyQuestRepository] and
 * [com.togetherly.data.daily.RoomDailyQuestRepository] by their respective concrete subclasses.
 */
internal abstract class DailyQuestRepositoryContractTest {

    abstract fun repository(): DailyQuestRepository

    private val today = LocalDate(2026, 7, 24)
    private val tomorrow = LocalDate(2026, 7, 25)

    @Test
    fun missingSelectionReturnsSuccessNull() = runTest {
        assertEquals(DataResult.Success(null), repository().getToday(today))
    }

    @Test
    fun savingForTheSameDateReplacesTheSelection() = runTest {
        val repository = repository()
        repository.saveDailyQuest(testDailyQuest(localDate = today, questId = QuestId("quest-1")))

        val replacement = testDailyQuest(localDate = today, questId = QuestId("quest-2"), selectionIndex = 1)
        repository.saveDailyQuest(replacement)

        assertEquals(DataResult.Success(replacement), repository.getToday(today))
    }

    @Test
    fun differentDatesRemainIndependent() = runTest {
        val repository = repository()
        val todaySelection = testDailyQuest(localDate = today, questId = QuestId("quest-1"))
        val tomorrowSelection = testDailyQuest(localDate = tomorrow, questId = QuestId("quest-2"))

        repository.saveDailyQuest(todaySelection)
        repository.saveDailyQuest(tomorrowSelection)

        assertEquals(DataResult.Success(todaySelection), repository.getToday(today))
        assertEquals(DataResult.Success(tomorrowSelection), repository.getToday(tomorrow))
    }

    @Test
    fun observeTodayEmitsTheCurrentSelection() = runTest {
        val repository = repository()
        val selection = testDailyQuest(localDate = today)

        repository.saveDailyQuest(selection)

        assertEquals(DataResult.Success(selection), repository.observeToday(today).first())
    }

    @Test
    fun clearingOneDateLeavesOtherDatesUntouched() = runTest {
        val repository = repository()
        repository.saveDailyQuest(testDailyQuest(localDate = today))
        val tomorrowSelection = testDailyQuest(localDate = tomorrow)
        repository.saveDailyQuest(tomorrowSelection)

        repository.clearDailyQuest(today)

        assertEquals(DataResult.Success(null), repository.getToday(today))
        assertEquals(DataResult.Success(tomorrowSelection), repository.getToday(tomorrow))
    }

    @Test
    fun clearingIsIdempotent() = runTest {
        val repository = repository()

        assertEquals(DataResult.Success(Unit), repository.clearDailyQuest(today))
        assertEquals(DataResult.Success(Unit), repository.clearDailyQuest(today))
    }

    @Test
    fun dismissalInsertionPreservesHistoryNewestFirst() = runTest {
        val repository = repository()
        val first = testDismissedQuest(questId = QuestId("quest-1"), dismissedAt = Instant.fromEpochMilliseconds(1_000L))
        val second = testDismissedQuest(questId = QuestId("quest-2"), dismissedAt = Instant.fromEpochMilliseconds(2_000L))

        repository.recordDismissal(first)
        repository.recordDismissal(second)

        val result = repository.getRecentDismissals(Instant.fromEpochMilliseconds(0L))
        assertEquals(DataResult.Success(listOf(second, first)), result)
    }

    @Test
    fun recentDismissalsAreFilteredByTimestamp() = runTest {
        val repository = repository()
        repository.recordDismissal(testDismissedQuest(questId = QuestId("old"), dismissedAt = Instant.fromEpochMilliseconds(1_000L)))
        val recent = testDismissedQuest(questId = QuestId("recent"), dismissedAt = Instant.fromEpochMilliseconds(3_000L))
        repository.recordDismissal(recent)

        val result = repository.getRecentDismissals(since = Instant.fromEpochMilliseconds(2_000L))

        assertEquals(DataResult.Success(listOf(recent)), result)
    }
}

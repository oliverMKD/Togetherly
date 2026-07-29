package com.togetherly.domain.daily.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.daily.DailyQuest
import com.togetherly.domain.daily.DailyQuestSource
import com.togetherly.domain.daily.DismissedQuest
import com.togetherly.domain.daily.QuestContext
import com.togetherly.domain.quest.QuestId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private val DAY_ONE = LocalDate(2026, 6, 15)
private val DAY_TWO = LocalDate(2026, 6, 16)
private val SELECTED_AT = Instant.parse("2026-06-15T08:00:00Z")
private val EMPTY_CONTEXT = QuestContext(null, null, null, null, null)

private fun dailyQuest(
    questId: String = "quest-1",
    localDate: LocalDate = DAY_ONE,
    selectionIndex: Int = 0,
    source: DailyQuestSource = DailyQuestSource.AUTOMATIC,
) = DailyQuest(
    questId = QuestId(questId),
    localDate = localDate,
    selectionIndex = selectionIndex,
    selectedAt = SELECTED_AT,
    source = source,
    context = EMPTY_CONTEXT,
)

class FakeDailyQuestRepositoryTest {

    @Test
    fun emptyDayReturnsNull() = runTest {
        val repository = FakeDailyQuestRepository()

        assertEquals(DataResult.Success(null), repository.getToday(DAY_ONE))
        assertEquals(DataResult.Success(null), repository.observeToday(DAY_ONE).first())
    }

    @Test
    fun savingEmitsTheSelection() = runTest {
        val repository = FakeDailyQuestRepository()
        val selection = dailyQuest()

        repository.saveDailyQuest(selection)

        assertEquals(DataResult.Success(selection), repository.observeToday(DAY_ONE).first())
    }

    @Test
    fun replacingSameDateSelectionEmitsTheReplacement() = runTest {
        val repository = FakeDailyQuestRepository()
        val original = dailyQuest(questId = "quest-1", selectionIndex = 0)
        val reroll = dailyQuest(questId = "quest-2", selectionIndex = 1, source = DailyQuestSource.REROLL)

        repository.saveDailyQuest(original)
        repository.saveDailyQuest(reroll)

        assertEquals(DataResult.Success(reroll), repository.getToday(DAY_ONE))
    }

    @Test
    fun differentDatesRemainIndependent() = runTest {
        val repository = FakeDailyQuestRepository()
        val dayOneSelection = dailyQuest(localDate = DAY_ONE)
        val dayTwoSelection = dailyQuest(questId = "quest-2", localDate = DAY_TWO)

        repository.saveDailyQuest(dayOneSelection)
        repository.saveDailyQuest(dayTwoSelection)

        assertEquals(DataResult.Success(dayOneSelection), repository.getToday(DAY_ONE))
        assertEquals(DataResult.Success(dayTwoSelection), repository.getToday(DAY_TWO))
    }

    @Test
    fun clearingRemovesOnlyTheRequestedDate() = runTest {
        val repository = FakeDailyQuestRepository()
        val dayTwoSelection = dailyQuest(questId = "quest-2", localDate = DAY_TWO)
        repository.saveDailyQuest(dailyQuest(localDate = DAY_ONE))
        repository.saveDailyQuest(dayTwoSelection)

        repository.clearDailyQuest(DAY_ONE)

        assertEquals(DataResult.Success(null), repository.getToday(DAY_ONE))
        assertEquals(DataResult.Success(dayTwoSelection), repository.getToday(DAY_TWO))
    }

    @Test
    fun dismissalsAreReturnedFromTheRequestedTimestamp() = runTest {
        val repository = FakeDailyQuestRepository()
        val early = DismissedQuest(QuestId("quest-1"), SELECTED_AT, DAY_ONE)
        val late = DismissedQuest(QuestId("quest-2"), SELECTED_AT + 2.hours, DAY_ONE)
        repository.recordDismissal(early)
        repository.recordDismissal(late)

        val result = repository.getRecentDismissals(since = SELECTED_AT + 1.hours)

        assertEquals(DataResult.Success(listOf(late)), result)
    }

    @Test
    fun errorsArePreserved() = runTest {
        val repository = FakeDailyQuestRepository()
        val error = AppError.Storage(StorageError.WRITE_FAILED)
        repository.setNextError(error)

        val result = repository.saveDailyQuest(dailyQuest())

        assertEquals(DataResult.Error(error), result)
    }
}

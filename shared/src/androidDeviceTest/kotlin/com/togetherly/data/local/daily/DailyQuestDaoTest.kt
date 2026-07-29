package com.togetherly.data.local.daily

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.data.local.RoomDaoTest
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun testDailyQuestEntity(
    localDate: String = "2026-07-24",
    questId: String = "quest-1",
    selectionIndex: Int = 0,
) = DailyQuestEntity(
    localDate = localDate,
    questId = questId,
    selectionIndex = selectionIndex,
    selectedAtEpochMillis = 1_000L,
    source = "automatic",
    contextDuration = null,
    contextLocation = null,
    contextEnergy = null,
    contextPreparation = null,
    contextCategory = null,
)

@RunWith(AndroidJUnit4::class)
internal class DailyQuestDaoTest : RoomDaoTest() {

    private val dao get() = database.dailyQuestDao()

    @Test
    fun insertingASecondSelectionForTheSameDateUpsertsRatherThanDuplicating() = runTest {
        dao.insertDailyQuest(testDailyQuestEntity(questId = "quest-1", selectionIndex = 0))

        dao.insertDailyQuest(testDailyQuestEntity(questId = "quest-2", selectionIndex = 1))

        val result = requireNotNull(dao.getDailyQuest("2026-07-24"))
        assertEquals("quest-2", result.questId)
        assertEquals(1, result.selectionIndex)
    }

    @Test
    fun missingDateReturnsNull() = runTest {
        assertNull(dao.getDailyQuest("2026-07-24"))
    }

    @Test
    fun deletingByDateRemovesOnlyThatDate() = runTest {
        dao.insertDailyQuest(testDailyQuestEntity(localDate = "2026-07-24"))
        dao.insertDailyQuest(testDailyQuestEntity(localDate = "2026-07-25"))

        dao.deleteDailyQuest("2026-07-24")

        assertNull(dao.getDailyQuest("2026-07-24"))
        assertEquals("2026-07-25", dao.getDailyQuest("2026-07-25")?.localDate)
    }

    @Test
    fun dismissalsSinceFiltersAndOrdersNewestFirst() = runTest {
        dao.insertDismissal(DismissedQuestEntity("quest-old", dismissedAtEpochMillis = 1_000L, localDate = "2026-07-01"))
        dao.insertDismissal(DismissedQuestEntity("quest-mid", dismissedAtEpochMillis = 2_000L, localDate = "2026-07-10"))
        dao.insertDismissal(DismissedQuestEntity("quest-new", dismissedAtEpochMillis = 3_000L, localDate = "2026-07-20"))

        val result = dao.getDismissalsSince(sinceEpochMillis = 1_500L)

        assertEquals(listOf("quest-new", "quest-mid"), result.map { it.questId })
    }

    @Test
    fun deleteDismissalsBeforeRemovesOnlyOlderRows() = runTest {
        dao.insertDismissal(DismissedQuestEntity("quest-old", dismissedAtEpochMillis = 1_000L, localDate = "2026-07-01"))
        dao.insertDismissal(DismissedQuestEntity("quest-new", dismissedAtEpochMillis = 3_000L, localDate = "2026-07-20"))

        dao.deleteDismissalsBefore(beforeEpochMillis = 2_000L)

        val remaining = dao.getDismissalsSince(sinceEpochMillis = 0L)
        assertEquals(listOf("quest-new"), remaining.map { it.questId })
    }
}

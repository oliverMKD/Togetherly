package com.togetherly.data.local.saved

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.togetherly.data.local.RoomDaoTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
internal class SavedQuestDaoTest : RoomDaoTest() {

    private val dao get() = database.savedQuestDao()

    @Test
    fun savedQuestsAreReturnedNewestFirst() = runTest {
        dao.insertSavedQuest(SavedQuestEntity("quest-old", savedAtEpochMillis = 1_000L))
        dao.insertSavedQuest(SavedQuestEntity("quest-new", savedAtEpochMillis = 2_000L))

        assertEquals(listOf("quest-new", "quest-old"), dao.getSavedQuests().map { it.questId })
        assertEquals(listOf("quest-new", "quest-old"), dao.observeSavedQuests().first().map { it.questId })
    }

    @Test
    fun reSavingTheSameQuestIsIdempotentNotDuplicated() = runTest {
        dao.insertSavedQuest(SavedQuestEntity("quest-1", savedAtEpochMillis = 1_000L))

        dao.insertSavedQuest(SavedQuestEntity("quest-1", savedAtEpochMillis = 2_000L))

        val all = dao.getSavedQuests()
        assertEquals(1, all.size)
        assertEquals(2_000L, all.single().savedAtEpochMillis)
    }

    @Test
    fun getSavedQuestReturnsNullWhenNotSaved() = runTest {
        assertNull(dao.getSavedQuest("quest-1"))
    }

    @Test
    fun deletingBySavedQuestIdRemovesOnlyThatQuest() = runTest {
        dao.insertSavedQuest(SavedQuestEntity("quest-1", savedAtEpochMillis = 1_000L))
        dao.insertSavedQuest(SavedQuestEntity("quest-2", savedAtEpochMillis = 2_000L))

        dao.deleteSavedQuest("quest-1")

        assertNull(dao.getSavedQuest("quest-1"))
        assertEquals("quest-2", dao.getSavedQuest("quest-2")?.questId)
    }

    @Test
    fun observingSavedQuestsEmitsUpdatesAsRowsChange() = runTest {
        dao.observeSavedQuests().test {
            assertEquals(emptyList(), awaitItem())

            dao.insertSavedQuest(SavedQuestEntity("quest-1", savedAtEpochMillis = 1_000L))
            assertEquals(listOf("quest-1"), awaitItem().map { it.questId })

            dao.deleteSavedQuest("quest-1")
            assertEquals(emptyList(), awaitItem())
        }
    }
}

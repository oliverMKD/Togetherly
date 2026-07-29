package com.togetherly.data.family

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.result.DataResult
import com.togetherly.data.completion.RoomCompletionRepository
import com.togetherly.data.daily.RoomDailyQuestRepository
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.data.local.database.buildTogetherlyDatabase
import com.togetherly.data.local.mapper.ActiveQuestSessionMapper
import com.togetherly.data.local.mapper.DailyQuestMapper
import com.togetherly.data.local.mapper.DismissedQuestMapper
import com.togetherly.data.local.mapper.FamilyProfileMapper
import com.togetherly.data.local.mapper.QuestCompletionMapper
import com.togetherly.data.local.mapper.SavedQuestMapper
import com.togetherly.data.saved.RoomSavedQuestRepository
import com.togetherly.data.testActiveQuestSession
import com.togetherly.data.testDailyQuest
import com.togetherly.data.testDismissedQuest
import com.togetherly.data.testFamilyProfile
import com.togetherly.data.testQuestCompletion
import com.togetherly.data.testSavedQuest
import com.togetherly.domain.family.repository.QuestHistoryCleaner
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
internal class RoomQuestHistoryCleanerTest {

    private lateinit var database: TogetherlyDatabase
    private val dispatchers = TestAppDispatchers(UnconfinedTestDispatcher())

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = buildTogetherlyDatabase(Room.inMemoryDatabaseBuilder(context, TogetherlyDatabase::class.java))
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun cleaner(): QuestHistoryCleaner = RoomQuestHistoryCleaner(
        dailyQuestDao = database.dailyQuestDao(),
        completionDao = database.completionDao(),
        database = database,
        dispatchers = dispatchers,
    )

    private suspend fun seedEverything() {
        RoomFamilyRepository(database.familyDao(), FamilyProfileMapper(), database, dispatchers)
            .saveProfile(testFamilyProfile())
        RoomDailyQuestRepository(database.dailyQuestDao(), DailyQuestMapper(), DismissedQuestMapper(), dispatchers).apply {
            saveDailyQuest(testDailyQuest(localDate = LocalDate(2026, 7, 24)))
            recordDismissal(testDismissedQuest())
        }
        RoomSavedQuestRepository(database.savedQuestDao(), SavedQuestMapper(), dispatchers)
            .save(testSavedQuest())
        RoomCompletionRepository(database.completionDao(), ActiveQuestSessionMapper(), QuestCompletionMapper(), database, dispatchers)
            .apply {
                saveActiveSession(testActiveQuestSession())
                saveCompletion(testQuestCompletion())
            }
    }

    @Test
    fun deletesQuestHistoryTablesButPreservesProfileAndSavedQuests() = runTest {
        seedEverything()

        val result = cleaner().resetQuestHistory()

        assertEquals(DataResult.Success(Unit), result)
        assertNull(database.dailyQuestDao().getDailyQuest(LocalDate(2026, 7, 24).toString()))
        assertEquals(emptyList<Any>(), database.dailyQuestDao().getDismissalsSince(0L))
        assertNull(database.completionDao().getActiveSession(0))
        assertEquals(emptyList<Any>(), database.completionDao().getCompletions())
        assertNotNull(database.familyDao().getFamilyProfile(), "Family profile must survive a quest-history reset")
        assertEquals(1, database.savedQuestDao().getSavedQuests().size, "Saved quests must survive a quest-history reset")
    }

    @Test
    fun repeatedResetIsIdempotent() = runTest {
        seedEverything()
        val cleaner = cleaner()

        assertEquals(DataResult.Success(Unit), cleaner.resetQuestHistory())
        assertEquals(DataResult.Success(Unit), cleaner.resetQuestHistory())
    }

    @Test
    fun resettingWithNothingStoredStillSucceeds() = runTest {
        assertEquals(DataResult.Success(Unit), cleaner().resetQuestHistory())
    }
}

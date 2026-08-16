package com.togetherly.data.family

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import com.togetherly.data.completion.RoomCompletionRepository
import com.togetherly.data.daily.RoomDailyQuestRepository
import com.togetherly.data.local.database.DatabaseMetadataEntity
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
import com.togetherly.domain.family.repository.FamilyDataCleaner
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
internal class RoomFamilyDataCleanerTest {

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

    private fun cleaner(): FamilyDataCleaner = RoomFamilyDataCleaner(
        familyDao = database.familyDao(),
        dailyQuestDao = database.dailyQuestDao(),
        savedQuestDao = database.savedQuestDao(),
        completionDao = database.completionDao(),
        database = database,
        dispatchers = dispatchers,
        diagnostics = FakeOperationalDiagnostics(),
    )

    private suspend fun seedEveryFamilyOwnedTable() {
        RoomFamilyRepository(
            familyDao = database.familyDao(),
            familyMapper = FamilyProfileMapper(),
            database = database,
            dispatchers = dispatchers,
            diagnostics = FakeOperationalDiagnostics(),
        )
            .saveProfile(testFamilyProfile())
        RoomDailyQuestRepository(
            dailyQuestDao = database.dailyQuestDao(),
            dailyQuestMapper = DailyQuestMapper(),
            dismissedQuestMapper = DismissedQuestMapper(),
            dispatchers = dispatchers,
            diagnostics = FakeOperationalDiagnostics(),
        ).apply {
            saveDailyQuest(testDailyQuest(localDate = LocalDate(2026, 7, 24)))
            recordDismissal(testDismissedQuest())
        }
        RoomSavedQuestRepository(
            savedQuestDao = database.savedQuestDao(),
            mapper = SavedQuestMapper(),
            dispatchers = dispatchers,
            diagnostics = FakeOperationalDiagnostics(),
        )
            .save(testSavedQuest())
        RoomCompletionRepository(
            completionDao = database.completionDao(),
            activeQuestSessionMapper = ActiveQuestSessionMapper(),
            completionMapper = QuestCompletionMapper(),
            database = database,
            dispatchers = dispatchers,
            diagnostics = FakeOperationalDiagnostics(),
        )
            .apply {
                saveActiveSession(testActiveQuestSession())
                saveCompletion(testQuestCompletion())
            }
    }

    @Test
    fun deletesEveryFamilyOwnedTable() = runTest {
        seedEveryFamilyOwnedTable()

        val result = cleaner().deleteAllFamilyData()

        assertEquals(DataResult.Success(Unit), result)
        assertNull(database.familyDao().getFamilyProfile())
        assertNull(database.dailyQuestDao().getDailyQuest(LocalDate(2026, 7, 24).toString()))
        assertEquals(emptyList<Any>(), database.dailyQuestDao().getDismissalsSince(0L))
        assertEquals(emptyList<Any>(), database.savedQuestDao().getSavedQuests())
        assertNull(database.completionDao().getActiveSession(0))
        assertEquals(emptyList<Any>(), database.completionDao().getCompletions())
    }

    @Test
    fun neverDeletesDatabaseMetadata() = runTest {
        database.metadataDao().set(DatabaseMetadataEntity(key = "schema_note", value = "kept"))
        seedEveryFamilyOwnedTable()

        cleaner().deleteAllFamilyData()

        assertEquals("kept", database.metadataDao().getValue("schema_note"))
    }

    @Test
    fun repeatedDeletionIsIdempotent() = runTest {
        seedEveryFamilyOwnedTable()
        val cleaner = cleaner()

        assertEquals(DataResult.Success(Unit), cleaner.deleteAllFamilyData())
        assertEquals(DataResult.Success(Unit), cleaner.deleteAllFamilyData())

        assertNull(database.familyDao().getFamilyProfile())
    }

    @Test
    fun deletingWithNothingStoredStillSucceeds() = runTest {
        assertEquals(DataResult.Success(Unit), cleaner().deleteAllFamilyData())
    }
}

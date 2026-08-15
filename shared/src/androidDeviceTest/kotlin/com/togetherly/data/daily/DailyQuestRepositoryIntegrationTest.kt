package com.togetherly.data.daily

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.data.local.database.buildTogetherlyDatabase
import com.togetherly.data.local.mapper.DailyQuestMapper
import com.togetherly.data.local.mapper.DismissedQuestMapper
import com.togetherly.data.testDailyQuest
import com.togetherly.data.testDismissedQuest
import com.togetherly.domain.daily.DailyQuest
import com.togetherly.domain.daily.repository.DailyQuestRepository
import com.togetherly.domain.quest.QuestId
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
internal class DailyQuestRepositoryIntegrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "daily-integration-${java.util.UUID.randomUUID()}.db"

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    private fun openDatabase(): TogetherlyDatabase =
        buildTogetherlyDatabase(Room.databaseBuilder<TogetherlyDatabase>(context = context, name = databaseName))

    private fun repositoryFor(database: TogetherlyDatabase): DailyQuestRepository = RoomDailyQuestRepository(
        dailyQuestDao = database.dailyQuestDao(),
        dailyQuestMapper = DailyQuestMapper(),
        dismissedQuestMapper = DismissedQuestMapper(),
        dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
        diagnostics = FakeOperationalDiagnostics(),
    )

    @Test
    fun dailySelectionSurvivesProcessStyleRecreation() = runTest {
        val localDate = LocalDate(2026, 7, 24)
        val selection = testDailyQuest(localDate = localDate)

        val firstProcessDatabase = openDatabase()
        repositoryFor(firstProcessDatabase).saveDailyQuest(selection)
        firstProcessDatabase.close()

        val secondProcessDatabase = openDatabase()
        try {
            assertEquals(DataResult.Success(selection), repositoryFor(secondProcessDatabase).getToday(localDate))
        } finally {
            secondProcessDatabase.close()
        }
    }

    @Test
    fun dismissalsSurviveProcessStyleRecreation() = runTest {
        val dismissal = testDismissedQuest(dismissedAt = Instant.fromEpochMilliseconds(1_000L))

        val firstProcessDatabase = openDatabase()
        repositoryFor(firstProcessDatabase).recordDismissal(dismissal)
        firstProcessDatabase.close()

        val secondProcessDatabase = openDatabase()
        try {
            val result = repositoryFor(secondProcessDatabase).getRecentDismissals(Instant.fromEpochMilliseconds(0L))
            assertEquals(DataResult.Success(listOf(dismissal)), result)
        } finally {
            secondProcessDatabase.close()
        }
    }

    @Test
    fun observeTodayEmitsUpdatesAsTheSelectionChanges() = runTest {
        val database = openDatabase()
        try {
            val localDate = LocalDate(2026, 7, 24)
            val repository = repositoryFor(database)

            repository.observeToday(localDate).test {
                assertEquals(DataResult.Success(null), awaitItem())

                val selection = testDailyQuest(localDate = localDate, questId = QuestId("quest-1"))
                repository.saveDailyQuest(selection)
                assertEquals(DataResult.Success(selection), awaitItem())

                repository.clearDailyQuest(localDate)
                assertEquals(DataResult.Success(null), awaitItem())
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun repositoryReturnsDomainModelsOnly() = runTest {
        val database = openDatabase()
        try {
            val repository: DailyQuestRepository = repositoryFor(database)
            val localDate = LocalDate(2026, 7, 24)
            repository.saveDailyQuest(testDailyQuest(localDate = localDate))

            assertIs<DataResult.Success<DailyQuest?>>(repository.getToday(localDate))
        } finally {
            database.close()
        }
    }
}

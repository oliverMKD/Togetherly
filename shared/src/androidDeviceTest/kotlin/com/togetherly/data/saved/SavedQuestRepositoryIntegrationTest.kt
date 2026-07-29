package com.togetherly.data.saved

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.result.DataResult
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.data.local.database.buildTogetherlyDatabase
import com.togetherly.data.local.mapper.SavedQuestMapper
import com.togetherly.data.testSavedQuest
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.saved.SavedQuest
import com.togetherly.domain.saved.repository.SavedQuestRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
internal class SavedQuestRepositoryIntegrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "saved-integration-${java.util.UUID.randomUUID()}.db"

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    private fun openDatabase(): TogetherlyDatabase =
        buildTogetherlyDatabase(Room.databaseBuilder<TogetherlyDatabase>(context = context, name = databaseName))

    private fun repositoryFor(database: TogetherlyDatabase): SavedQuestRepository = RoomSavedQuestRepository(
        savedQuestDao = database.savedQuestDao(),
        mapper = SavedQuestMapper(),
        dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
    )

    @Test
    fun savedQuestsSurviveProcessStyleRecreation() = runTest {
        val saved = testSavedQuest(questId = QuestId("quest-1"), savedAt = Instant.fromEpochMilliseconds(1_000L))

        val firstProcessDatabase = openDatabase()
        repositoryFor(firstProcessDatabase).save(saved)
        firstProcessDatabase.close()

        val secondProcessDatabase = openDatabase()
        try {
            assertEquals(DataResult.Success(listOf(saved)), repositoryFor(secondProcessDatabase).getSavedQuests())
        } finally {
            secondProcessDatabase.close()
        }
    }

    @Test
    fun observeSavedQuestsEmitsUpdatesAsQuestsAreSavedAndRemoved() = runTest {
        val database = openDatabase()
        try {
            val repository = repositoryFor(database)

            repository.observeSavedQuests().test {
                assertEquals(DataResult.Success(emptyList()), awaitItem())

                val saved = testSavedQuest(questId = QuestId("quest-1"))
                repository.save(saved)
                assertEquals(DataResult.Success(listOf(saved)), awaitItem())

                repository.remove(QuestId("quest-1"))
                assertEquals(DataResult.Success(emptyList()), awaitItem())
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun repositoryReturnsDomainModelsOnly() = runTest {
        val database = openDatabase()
        try {
            val repository: SavedQuestRepository = repositoryFor(database)
            repository.save(testSavedQuest())

            assertIs<DataResult.Success<List<SavedQuest>>>(repository.getSavedQuests())
        } finally {
            database.close()
        }
    }
}

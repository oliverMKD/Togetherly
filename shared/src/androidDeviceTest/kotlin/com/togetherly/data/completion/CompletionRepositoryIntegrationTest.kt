package com.togetherly.data.completion

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.data.local.database.buildTogetherlyDatabase
import com.togetherly.data.local.mapper.ActiveQuestSessionMapper
import com.togetherly.data.local.mapper.QuestCompletionMapper
import com.togetherly.data.testActiveQuestSession
import com.togetherly.data.testPhotoMedia
import com.togetherly.data.testQuestCompletion
import com.togetherly.data.testVoiceMedia
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.FamilyReaction
import com.togetherly.domain.completion.MemoryNote
import com.togetherly.domain.completion.QuestCompletion
import com.togetherly.domain.completion.repository.CompletionRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs

@RunWith(AndroidJUnit4::class)
internal class CompletionRepositoryIntegrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "completion-integration-${java.util.UUID.randomUUID()}.db"

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    private fun openDatabase(): TogetherlyDatabase =
        buildTogetherlyDatabase(Room.databaseBuilder<TogetherlyDatabase>(context = context, name = databaseName))

    private fun repositoryFor(database: TogetherlyDatabase): CompletionRepository = RoomCompletionRepository(
        completionDao = database.completionDao(),
        activeQuestSessionMapper = ActiveQuestSessionMapper(),
        completionMapper = QuestCompletionMapper(),
        database = database,
        dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
        diagnostics = FakeOperationalDiagnostics(),
    )

    @Test
    fun activeSessionSurvivesProcessStyleRecreation() = runTest {
        val session = testActiveQuestSession()

        val firstProcessDatabase = openDatabase()
        repositoryFor(firstProcessDatabase).saveActiveSession(session)
        firstProcessDatabase.close()

        val secondProcessDatabase = openDatabase()
        try {
            assertEquals(DataResult.Success(session), repositoryFor(secondProcessDatabase).getActiveSession())
        } finally {
            secondProcessDatabase.close()
        }
    }

    @Test
    fun completionWithNoteReactionsAndMediaSurvivesProcessStyleRecreation() = runTest {
        val completion = testQuestCompletion(
            note = MemoryNote("A rainy afternoon indoors."),
            reactions = setOf(FamilyReaction.HAPPY, FamilyReaction.LOVED_IT),
            media = listOf(testPhotoMedia(), testVoiceMedia()),
        )

        val firstProcessDatabase = openDatabase()
        repositoryFor(firstProcessDatabase).saveCompletion(completion)
        firstProcessDatabase.close()

        val secondProcessDatabase = openDatabase()
        try {
            assertEquals(DataResult.Success(completion), repositoryFor(secondProcessDatabase).getCompletion(completion.id))
        } finally {
            secondProcessDatabase.close()
        }
    }

    @Test
    fun observeCompletionsEmitsUpdatesAsCompletionsAreSavedAndDeleted() = runTest {
        val database = openDatabase()
        try {
            val repository = repositoryFor(database)

            repository.observeCompletions().test {
                assertEquals(DataResult.Success(emptyList()), awaitItem())

                val completion = testQuestCompletion(id = CompletionId("completion-1"))
                repository.saveCompletion(completion)
                assertEquals(DataResult.Success(listOf(completion)), awaitItem())

                repository.deleteCompletion(CompletionId("completion-1"))
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
            val repository: CompletionRepository = repositoryFor(database)
            repository.saveCompletion(testQuestCompletion())

            assertIs<DataResult.Success<List<QuestCompletion>>>(repository.getCompletions())
        } finally {
            database.close()
        }
    }
}

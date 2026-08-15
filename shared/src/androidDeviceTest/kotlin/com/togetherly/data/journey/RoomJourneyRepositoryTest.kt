package com.togetherly.data.journey

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import com.togetherly.data.completion.RoomCompletionRepository
import com.togetherly.data.local.completion.CompletionDao
import com.togetherly.data.local.completion.QuestCompletionWithDetails
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.data.local.database.buildTogetherlyDatabase
import com.togetherly.data.local.mapper.ActiveQuestSessionMapper
import com.togetherly.data.local.mapper.QuestCompletionMapper
import com.togetherly.data.testFamilyQuest
import com.togetherly.data.testQuestCompletion
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MemoryNote
import com.togetherly.domain.completion.repository.CompletionRepository
import com.togetherly.domain.journey.JourneyEntry
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.repository.QuestRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [RoomJourneyRepository] joins [com.togetherly.data.local.completion.CompletionDao] against a
 * [QuestRepository] in memory — this exercises that join directly, sharing one in-memory database
 * between a real [RoomCompletionRepository] (to seed/mutate completions realistically, the same
 * way production wires the two repositories) and a [FakeQuestRepository] standing in for the
 * bundled catalogue, so the catalogue-unavailable degraded path can be triggered deterministically.
 */
@RunWith(AndroidJUnit4::class)
internal class RoomJourneyRepositoryTest {

    private lateinit var database: TogetherlyDatabase
    private lateinit var completionRepository: CompletionRepository
    private lateinit var questRepository: FakeQuestRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = buildTogetherlyDatabase(Room.inMemoryDatabaseBuilder(context, TogetherlyDatabase::class.java))
        completionRepository = RoomCompletionRepository(
            completionDao = database.completionDao(),
            activeQuestSessionMapper = ActiveQuestSessionMapper(),
            completionMapper = QuestCompletionMapper(),
            database = database,
            dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
            diagnostics = FakeOperationalDiagnostics(),
        )
        questRepository = FakeQuestRepository()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun journeyRepository(quests: QuestRepository = questRepository) = RoomJourneyRepository(
        completionDao = database.completionDao(),
        questRepository = quests,
        completionMapper = QuestCompletionMapper(),
        dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
        diagnostics = FakeOperationalDiagnostics(),
    )

    @Test
    fun resolvesQuestFromTheCatalogueForACompletion() = runTest {
        val quest = testFamilyQuest(id = QuestId("quest-1"))
        questRepository.setQuests(listOf(quest))
        val completion = testQuestCompletion(id = CompletionId("completion-1"), questId = quest.id)
        completionRepository.saveCompletion(completion)

        val result = journeyRepository().getJourney()

        assertEquals(DataResult.Success(listOf(JourneyEntry(completion, quest))), result)
    }

    @Test
    fun missingQuestPreservesTheCompletionWithNullQuest() = runTest {
        questRepository.setQuests(emptyList())
        val completion = testQuestCompletion(id = CompletionId("completion-1"), questId = QuestId("quest-removed"))
        completionRepository.saveCompletion(completion)

        val result = journeyRepository().getJourney()

        assertEquals(DataResult.Success(listOf(JourneyEntry(completion, null))), result)
        assertNull((result as DataResult.Success).value.single().quest)
    }

    @Test
    fun catalogueUnavailablePreservesCompletionsWithNullQuestInsteadOfAnError() = runTest {
        val completion = testQuestCompletion(id = CompletionId("completion-1"))
        completionRepository.saveCompletion(completion)
        questRepository.setNextError(AppError.Content(ContentError.CATALOGUE_UNAVAILABLE))

        val result = journeyRepository().getJourney()

        assertEquals(DataResult.Success(listOf(JourneyEntry(completion, null))), result)
    }

    /**
     * Closing [database] mid-test was tried first and rejected: Room's coroutine-based connection
     * pool then fails pending reads with [kotlinx.coroutines.JobCancellationException] — a
     * [kotlin.coroutines.cancellation.CancellationException] subtype — which
     * [com.togetherly.data.runCatchingStorage] correctly rethrows rather than swallowing into an
     * [AppError] (cancellation must always propagate, never be reinterpreted as a failure — see
     * [com.togetherly.core.result.DataResult]'s own KDoc). That makes a closed database the wrong
     * tool to simulate a genuine read failure here; a [CompletionDao] that throws a plain
     * exception does it deterministically instead.
     */
    @Test
    fun aCompletionsReadFailureIsAGenuineJourneyError() = runTest {
        completionRepository.saveCompletion(testQuestCompletion())
        val failingRepository = RoomJourneyRepository(
            completionDao = FailingCompletionsDao(database.completionDao()),
            questRepository = questRepository,
            completionMapper = QuestCompletionMapper(),
            dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
            diagnostics = FakeOperationalDiagnostics(),
        )

        val result = failingRepository.getJourney()

        assert(result is DataResult.Error) { "expected a Journey error when the completions read fails, got $result" }
    }

    private class FailingCompletionsDao(
        private val delegate: CompletionDao,
    ) : CompletionDao by delegate {
        override suspend fun getCompletions(): List<QuestCompletionWithDetails> =
            throw IllegalStateException("simulated read failure")
    }

    @Test
    fun catalogueIsLoadedOnceRegardlessOfCompletionCount() = runTest {
        val quest = testFamilyQuest(id = QuestId("quest-1"))
        questRepository.setQuests(listOf(quest))
        repeat(3) { index ->
            completionRepository.saveCompletion(
                testQuestCompletion(id = CompletionId("completion-$index"), questId = quest.id),
            )
        }
        val counting = CountingQuestRepository(questRepository)

        val result = journeyRepository(counting).getJourney()

        assertEquals(3, (result as DataResult.Success).value.size)
        assertEquals(1, counting.getAllQuestsCallCount)
    }

    @Test
    fun deletingACompletionUpdatesJourney() = runTest {
        val quest = testFamilyQuest(id = QuestId("quest-1"))
        questRepository.setQuests(listOf(quest))
        val kept = testQuestCompletion(id = CompletionId("completion-kept"), questId = quest.id)
        val removed = testQuestCompletion(id = CompletionId("completion-removed"), questId = quest.id)
        completionRepository.saveCompletion(kept)
        completionRepository.saveCompletion(removed)

        completionRepository.deleteCompletion(removed.id)

        val result = journeyRepository().getJourney()
        assertEquals(listOf(kept.id), (result as DataResult.Success).value.map { it.completion.id })
    }

    @Test
    fun replacingACompletionUpdatesJourneyWithoutDuplicating() = runTest {
        val quest = testFamilyQuest(id = QuestId("quest-1"))
        questRepository.setQuests(listOf(quest))
        val original = testQuestCompletion(id = CompletionId("completion-1"), questId = quest.id, note = MemoryNote("Before"))
        completionRepository.saveCompletion(original)

        val replacement = original.copy(note = MemoryNote("After"))
        completionRepository.saveCompletion(replacement)

        val result = journeyRepository().getJourney()
        val entries = (result as DataResult.Success).value
        assertEquals(1, entries.size)
        assertEquals(MemoryNote("After"), entries.single().completion.note)
    }

    @Test
    fun journeyOrdersEntriesNewestCompletionFirst() = runTest {
        val quest = testFamilyQuest(id = QuestId("quest-1"))
        questRepository.setQuests(listOf(quest))
        val older = testQuestCompletion(
            id = CompletionId("completion-old"),
            questId = quest.id,
            completedAt = kotlin.time.Instant.fromEpochMilliseconds(1_000L),
        )
        val newer = testQuestCompletion(
            id = CompletionId("completion-new"),
            questId = quest.id,
            completedAt = kotlin.time.Instant.fromEpochMilliseconds(2_000L),
        )
        completionRepository.saveCompletion(older)
        completionRepository.saveCompletion(newer)

        val result = journeyRepository().getJourney()

        assertEquals(listOf(newer.id, older.id), (result as DataResult.Success).value.map { it.completion.id })
    }

    @Test
    fun journeyReadsNeverWriteAdditionalPersistenceData() = runTest {
        val quest = testFamilyQuest(id = QuestId("quest-1"))
        questRepository.setQuests(listOf(quest))
        completionRepository.saveCompletion(testQuestCompletion(id = CompletionId("completion-1"), questId = quest.id))

        val repository = journeyRepository()
        repository.getJourney()
        repository.getJourney()

        val storedCompletions = (completionRepository.getCompletions() as DataResult.Success).value
        assertEquals(1, storedCompletions.size)
    }

    private class CountingQuestRepository(
        private val delegate: QuestRepository,
    ) : QuestRepository by delegate {
        var getAllQuestsCallCount = 0
            private set

        override suspend fun getAllQuests(): DataResult<List<FamilyQuest>> {
            getAllQuestsCallCount++
            return delegate.getAllQuests()
        }
    }
}

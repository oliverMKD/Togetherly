package com.togetherly.data.completion

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.result.DataResult
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.data.local.database.buildTogetherlyDatabase
import com.togetherly.data.local.mapper.ActiveQuestSessionMapper
import com.togetherly.data.local.mapper.QuestCompletionMapper
import com.togetherly.data.testPhotoMedia
import com.togetherly.data.testQuestCompletion
import com.togetherly.data.testVoiceMedia
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.FamilyReaction
import com.togetherly.domain.completion.MemoryNote
import com.togetherly.domain.completion.repository.MemoryCleaner
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
internal class RoomMemoryCleanerTest {

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

    private fun cleaner(): MemoryCleaner = RoomMemoryCleaner(
        completionDao = database.completionDao(),
        database = database,
        dispatchers = dispatchers,
    )

    private fun completionRepository() =
        RoomCompletionRepository(database.completionDao(), ActiveQuestSessionMapper(), QuestCompletionMapper(), database, dispatchers)

    @Test
    fun clearsNoteAndMediaButKeepsTheCompletionRowAndReactions() = runTest {
        completionRepository().saveCompletion(
            testQuestCompletion(
                note = MemoryNote("Best day ever"),
                reactions = setOf(FamilyReaction.LOVE),
                media = listOf(testPhotoMedia(), testVoiceMedia()),
            ),
        )

        val result = cleaner().clearAllMemoryContent()

        assertEquals(DataResult.Success(Unit), result)
        val stored = database.completionDao().getCompletion(CompletionId("completion-1").value)
        assertNotNull(stored, "The completion row itself must survive a memory-only clear")
        assertNull(stored.completion.note)
        assertTrue(stored.media.isEmpty())
        assertEquals(1, stored.reactions.size, "Reactions are not memory content and must be preserved")
    }

    @Test
    fun clearsMemoryContentAcrossEveryCompletion() = runTest {
        val repository = completionRepository()
        repository.saveCompletion(testQuestCompletion(id = CompletionId("completion-1"), note = MemoryNote("One")))
        repository.saveCompletion(testQuestCompletion(id = CompletionId("completion-2"), note = MemoryNote("Two")))

        cleaner().clearAllMemoryContent()

        assertNull(database.completionDao().getCompletion(CompletionId("completion-1").value)?.completion?.note)
        assertNull(database.completionDao().getCompletion(CompletionId("completion-2").value)?.completion?.note)
    }

    @Test
    fun repeatedClearingIsIdempotent() = runTest {
        completionRepository().saveCompletion(testQuestCompletion(note = MemoryNote("Best day ever"), media = listOf(testPhotoMedia())))
        val cleaner = cleaner()

        assertEquals(DataResult.Success(Unit), cleaner.clearAllMemoryContent())
        assertEquals(DataResult.Success(Unit), cleaner.clearAllMemoryContent())
    }

    @Test
    fun clearingWithNothingStoredStillSucceeds() = runTest {
        assertEquals(DataResult.Success(Unit), cleaner().clearAllMemoryContent())
    }
}

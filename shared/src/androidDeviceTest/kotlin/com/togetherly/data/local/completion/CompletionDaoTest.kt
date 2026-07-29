package com.togetherly.data.local.completion

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.data.local.RoomDaoTest
import com.togetherly.data.local.keys.MEMORY_MEDIA_TYPE_PHOTO
import com.togetherly.data.local.keys.MEMORY_MEDIA_TYPE_VOICE
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun testCompletionEntity(
    id: String = "completion-1",
    familyId: String = "family-1",
    questId: String = "quest-1",
    completedAtEpochMillis: Long = 2_000L,
) = QuestCompletionEntity(
    id = id,
    familyId = familyId,
    questId = questId,
    questVersion = 1,
    startedAtEpochMillis = 1_000L,
    completedAtEpochMillis = completedAtEpochMillis,
    note = null,
)

@RunWith(AndroidJUnit4::class)
internal class CompletionDaoTest : RoomDaoTest() {

    private val dao get() = database.completionDao()

    @Test
    fun onlyOneActiveSessionSlotExists() = runTest {
        dao.insertActiveSession(
            ActiveQuestSessionEntity(
                slotId = ACTIVE_QUEST_SESSION_SLOT_ID,
                completionId = "completion-a",
                familyId = "family-1",
                questId = "quest-1",
                questVersion = 1,
                startedAtEpochMillis = 1_000L,
            ),
        )

        dao.insertActiveSession(
            ActiveQuestSessionEntity(
                slotId = ACTIVE_QUEST_SESSION_SLOT_ID,
                completionId = "completion-b",
                familyId = "family-1",
                questId = "quest-2",
                questVersion = 1,
                startedAtEpochMillis = 2_000L,
            ),
        )

        val session = requireNotNull(dao.getActiveSession(ACTIVE_QUEST_SESSION_SLOT_ID))
        assertEquals("completion-b", session.completionId)
    }

    @Test
    fun deletingTheActiveSessionClearsTheSlot() = runTest {
        dao.insertActiveSession(
            ActiveQuestSessionEntity(ACTIVE_QUEST_SESSION_SLOT_ID, "completion-a", "family-1", "quest-1", 1, 1_000L),
        )

        dao.deleteActiveSession(ACTIVE_QUEST_SESSION_SLOT_ID)

        assertNull(dao.getActiveSession(ACTIVE_QUEST_SESSION_SLOT_ID))
    }

    @Test
    fun getCompletionAssemblesReactionsAndMedia() = runTest {
        dao.insertCompletion(testCompletionEntity())
        dao.insertReactions(
            listOf(
                CompletionReactionEntity("completion-1", "happy"),
                CompletionReactionEntity("completion-1", "silly"),
            ),
        )
        dao.insertMedia(
            listOf(
                MemoryMediaEntity("media-1", "completion-1", MEMORY_MEDIA_TYPE_PHOTO, "ref-1", durationMillis = null),
                MemoryMediaEntity("media-2", "completion-1", MEMORY_MEDIA_TYPE_VOICE, "ref-2", durationMillis = 4_000L),
            ),
        )

        val result = requireNotNull(dao.getCompletion("completion-1"))

        assertEquals("completion-1", result.completion.id)
        assertEquals(setOf("happy", "silly"), result.reactions.map { it.reaction }.toSet())
        assertEquals(setOf("media-1", "media-2"), result.media.map { it.id }.toSet())
    }

    @Test
    fun missingCompletionReturnsNull() = runTest {
        assertNull(dao.getCompletion("completion-1"))
    }

    @Test
    fun deletingACompletionCascadesToReactionsAndMedia() = runTest {
        dao.insertCompletion(testCompletionEntity())
        dao.insertReactions(listOf(CompletionReactionEntity("completion-1", "happy")))
        dao.insertMedia(listOf(MemoryMediaEntity("media-1", "completion-1", MEMORY_MEDIA_TYPE_PHOTO, "ref-1", null)))

        dao.deleteCompletion("completion-1")

        // Re-create a bare completion with the same ID. If the cascade hadn't fired, the old
        // reaction/media rows (which reference this completionId) would still resolve here.
        dao.insertCompletion(testCompletionEntity())

        val result = requireNotNull(dao.getCompletion("completion-1"))
        assertTrue(result.reactions.isEmpty())
        assertTrue(result.media.isEmpty())
    }

    @Test
    fun aSecondPhotoForTheSameCompletionReplacesTheFirstRatherThanDuplicating() = runTest {
        dao.insertCompletion(testCompletionEntity())
        dao.insertMedia(listOf(MemoryMediaEntity("media-1", "completion-1", MEMORY_MEDIA_TYPE_PHOTO, "ref-1", null)))

        dao.insertMedia(listOf(MemoryMediaEntity("media-2", "completion-1", MEMORY_MEDIA_TYPE_PHOTO, "ref-2", null)))

        val result = requireNotNull(dao.getCompletion("completion-1"))
        assertEquals(1, result.media.count { it.type == MEMORY_MEDIA_TYPE_PHOTO })
        assertEquals("media-2", result.media.single { it.type == MEMORY_MEDIA_TYPE_PHOTO }.id)
    }

    @Test
    fun observeCompletionsOrdersNewestFirst() = runTest {
        dao.insertCompletion(testCompletionEntity(id = "completion-old", completedAtEpochMillis = 1_000L))
        dao.insertCompletion(testCompletionEntity(id = "completion-new", completedAtEpochMillis = 2_000L))

        val ids = dao.observeCompletions().first().map { it.completion.id }

        assertEquals(listOf("completion-new", "completion-old"), ids)
    }
}

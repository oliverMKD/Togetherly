package com.togetherly.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.data.local.completion.CompletionReactionEntity
import com.togetherly.data.local.completion.MemoryMediaEntity
import com.togetherly.data.local.completion.QuestCompletionEntity
import com.togetherly.data.local.daily.DailyQuestEntity
import com.togetherly.data.local.daily.DismissedQuestEntity
import com.togetherly.data.local.keys.MEMORY_MEDIA_TYPE_PHOTO
import com.togetherly.data.local.saved.SavedQuestEntity
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/**
 * Step 6.6's foreign-key audit. Two things are checked here that no existing test covers:
 *
 * 1. [SavedQuestEntity]/[DailyQuestEntity]/[DismissedQuestEntity] can store any `questId` string,
 *    including one that matches nothing anywhere — there is no catalogue table to constrain
 *    against (the bundled catalogue is JSON-backed, see docs/content-system.md), so creating a
 *    foreign key to a nonexistent table is not just unnecessary but impossible; this proves no
 *    such constraint was accidentally added another way (e.g. a `CHECK` clause).
 * 2. A precise *count* of surviving reaction/media rows after a completion delete — not just "the
 *    deleted completion's own children are gone" (already covered by [com.togetherly.data.local.completion.CompletionDaoTest.deletingACompletionCascadesToReactionsAndMedia])
 *    but "exactly the untouched completion's children remain, no more and no less".
 *
 * The remaining audit items the task calls for are already covered elsewhere and are not
 * duplicated here: completion deletion cascading reactions/media
 * ([com.togetherly.data.local.completion.CompletionDaoTest]), and family deletion cascading
 * preference rows without touching completions or the active session
 * ([com.togetherly.data.local.family.FamilyDaoTest], [com.togetherly.data.family.RoomFamilyRepository]'s
 * own KDoc).
 */
@RunWith(AndroidJUnit4::class)
internal class ForeignKeyAuditTest : RoomDaoTest() {

    @Test
    fun savedQuestAcceptsAQuestIdThatMatchesNoCatalogueEntry() = runTest {
        database.savedQuestDao().insertSavedQuest(SavedQuestEntity(questId = "not-a-real-catalogue-id", savedAtEpochMillis = 1_000L))

        assertEquals("not-a-real-catalogue-id", database.savedQuestDao().getSavedQuest("not-a-real-catalogue-id")?.questId)
    }

    @Test
    fun dailyQuestAcceptsAQuestIdThatMatchesNoCatalogueEntry() = runTest {
        database.dailyQuestDao().insertDailyQuest(
            DailyQuestEntity(
                localDate = "2026-07-24",
                questId = "not-a-real-catalogue-id",
                selectionIndex = 0,
                selectedAtEpochMillis = 1_000L,
                source = "automatic",
                contextDuration = null,
                contextLocation = null,
                contextEnergy = null,
                contextPreparation = null,
                contextCategory = null,
            ),
        )

        assertEquals("not-a-real-catalogue-id", database.dailyQuestDao().getDailyQuest("2026-07-24")?.questId)
    }

    @Test
    fun dismissedQuestAcceptsAQuestIdThatMatchesNoCatalogueEntry() = runTest {
        database.dailyQuestDao().insertDismissal(
            DismissedQuestEntity(questId = "not-a-real-catalogue-id", dismissedAtEpochMillis = 1_000L, localDate = "2026-07-24"),
        )

        val dismissals = database.dailyQuestDao().getDismissalsSince(0L)
        assertEquals(listOf("not-a-real-catalogue-id"), dismissals.map { it.questId })
    }

    @Test
    fun deletingOneCompletionLeavesExactlyTheOtherCompletionsChildRows() = runTest {
        val dao = database.completionDao()
        dao.insertCompletion(
            QuestCompletionEntity("completion-kept", "family-1", "quest-1", 1, null, 1_000L, null),
        )
        dao.insertReactions(listOf(CompletionReactionEntity("completion-kept", "happy")))
        dao.insertMedia(listOf(MemoryMediaEntity("media-kept", "completion-kept", MEMORY_MEDIA_TYPE_PHOTO, "ref-kept", null)))

        dao.insertCompletion(
            QuestCompletionEntity("completion-removed", "family-1", "quest-1", 1, null, 2_000L, null),
        )
        dao.insertReactions(
            listOf(
                CompletionReactionEntity("completion-removed", "happy"),
                CompletionReactionEntity("completion-removed", "silly"),
            ),
        )
        dao.insertMedia(listOf(MemoryMediaEntity("media-removed", "completion-removed", MEMORY_MEDIA_TYPE_PHOTO, "ref-removed", null)))

        dao.deleteCompletion("completion-removed")

        val remaining = dao.getCompletions()
        assertEquals(listOf("completion-kept"), remaining.map { it.completion.id })
        assertEquals(1, remaining.sumOf { it.reactions.size })
        assertEquals(1, remaining.sumOf { it.media.size })
    }
}

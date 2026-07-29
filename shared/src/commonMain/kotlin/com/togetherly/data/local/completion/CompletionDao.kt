package com.togetherly.data.local.completion

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
internal interface CompletionDao {

    @Query("SELECT * FROM active_quest_session WHERE slotId = :slotId")
    fun observeActiveSession(slotId: Int): Flow<ActiveQuestSessionEntity?>

    @Query("SELECT * FROM active_quest_session WHERE slotId = :slotId")
    suspend fun getActiveSession(slotId: Int): ActiveQuestSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActiveSession(entity: ActiveQuestSessionEntity)

    @Query("DELETE FROM active_quest_session WHERE slotId = :slotId")
    suspend fun deleteActiveSession(slotId: Int)

    @Transaction
    @Query("SELECT * FROM quest_completion ORDER BY completedAtEpochMillis DESC")
    fun observeCompletions(): Flow<List<QuestCompletionWithDetails>>

    @Transaction
    @Query("SELECT * FROM quest_completion ORDER BY completedAtEpochMillis DESC")
    suspend fun getCompletions(): List<QuestCompletionWithDetails>

    @Transaction
    @Query("SELECT * FROM quest_completion WHERE id = :completionId")
    fun observeCompletion(completionId: String): Flow<QuestCompletionWithDetails?>

    @Transaction
    @Query("SELECT * FROM quest_completion WHERE id = :completionId")
    suspend fun getCompletion(completionId: String): QuestCompletionWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(entity: QuestCompletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReactions(entities: List<CompletionReactionEntity>)

    @Query("DELETE FROM completion_reaction WHERE completionId = :completionId")
    suspend fun deleteReactions(completionId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(entities: List<MemoryMediaEntity>)

    @Query("DELETE FROM memory_media WHERE completionId = :completionId")
    suspend fun deleteMedia(completionId: String)

    /**
     * Replaces a completion's full state in one transaction: insert-or-replace the primary row,
     * then unconditionally re-derive its reaction and media rows from scratch (delete-then-insert
     * for each) rather than diffing. This guarantees the stored reaction/media set always exactly
     * mirrors [reactions]/[media] as supplied — a reaction or media item removed by the caller is
     * actually removed here too, not just left stale alongside newly inserted ones.
     */
    @Transaction
    suspend fun replaceCompletion(
        completion: QuestCompletionEntity,
        reactions: List<CompletionReactionEntity>,
        media: List<MemoryMediaEntity>,
    ) {
        insertCompletion(completion)
        deleteReactions(completion.id)
        insertReactions(reactions)
        deleteMedia(completion.id)
        insertMedia(media)
    }

    /**
     * Cascade-deletes the completion's reaction and media metadata rows via their FK `ON DELETE
     * CASCADE` (Step 6.2) — never the referenced media *files*, which are a separate, not-yet-
     * implemented responsibility. Idempotent: deleting an unknown ID still succeeds.
     */
    @Query("DELETE FROM quest_completion WHERE id = :completionId")
    suspend fun deleteCompletion(completionId: String)

    /** Cascade-deletes every reaction and media metadata row along with every completion — see [deleteCompletion]'s own KDoc. */
    @Query("DELETE FROM quest_completion")
    suspend fun deleteAllCompletions()

    /**
     * Clears every completion's note without touching the completion row itself, its reactions,
     * or [deleteAllMemoryMedia] — see [com.togetherly.domain.completion.repository.MemoryCleaner]'s
     * own KDoc for why a memory's note/media is clearable independently of the completion it
     * belongs to.
     */
    @Query("UPDATE quest_completion SET note = NULL")
    suspend fun clearAllNotes()

    /** Deletes every memory media metadata row across every completion — never the referenced files; see [MemoryCleaner][com.togetherly.domain.completion.repository.MemoryCleaner]. */
    @Query("DELETE FROM memory_media")
    suspend fun deleteAllMemoryMedia()
}

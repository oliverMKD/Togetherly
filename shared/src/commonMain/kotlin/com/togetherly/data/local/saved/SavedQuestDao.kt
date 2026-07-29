package com.togetherly.data.local.saved

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** [OnConflictStrategy.REPLACE] on the [SavedQuestEntity.questId] primary key makes re-saving an already-saved quest an idempotent upsert rather than a constraint failure. */
@Dao
internal interface SavedQuestDao {

    @Query("SELECT * FROM saved_quest ORDER BY savedAtEpochMillis DESC")
    fun observeSavedQuests(): Flow<List<SavedQuestEntity>>

    @Query("SELECT * FROM saved_quest ORDER BY savedAtEpochMillis DESC")
    suspend fun getSavedQuests(): List<SavedQuestEntity>

    @Query("SELECT * FROM saved_quest WHERE questId = :questId")
    suspend fun getSavedQuest(questId: String): SavedQuestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedQuest(entity: SavedQuestEntity)

    @Query("DELETE FROM saved_quest WHERE questId = :questId")
    suspend fun deleteSavedQuest(questId: String)

    @Query("DELETE FROM saved_quest")
    suspend fun deleteAllSavedQuests()
}

package com.togetherly.data.local.daily

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface DailyQuestDao {

    @Query("SELECT * FROM daily_quest WHERE localDate = :localDate")
    fun observeDailyQuest(localDate: String): Flow<DailyQuestEntity?>

    @Query("SELECT * FROM daily_quest WHERE localDate = :localDate")
    suspend fun getDailyQuest(localDate: String): DailyQuestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyQuest(entity: DailyQuestEntity)

    @Query("DELETE FROM daily_quest WHERE localDate = :localDate")
    suspend fun deleteDailyQuest(localDate: String)

    @Query("DELETE FROM daily_quest")
    suspend fun deleteAllDailyQuests()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDismissal(entity: DismissedQuestEntity)

    @Query("SELECT * FROM dismissed_quest WHERE dismissedAtEpochMillis >= :sinceEpochMillis ORDER BY dismissedAtEpochMillis DESC")
    suspend fun getDismissalsSince(sinceEpochMillis: Long): List<DismissedQuestEntity>

    @Query("DELETE FROM dismissed_quest WHERE dismissedAtEpochMillis < :beforeEpochMillis")
    suspend fun deleteDismissalsBefore(beforeEpochMillis: Long)

    @Query("DELETE FROM dismissed_quest")
    suspend fun deleteAllDismissals()
}

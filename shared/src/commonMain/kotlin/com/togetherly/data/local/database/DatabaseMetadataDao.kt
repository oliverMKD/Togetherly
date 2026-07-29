package com.togetherly.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface DatabaseMetadataDao {

    @Query("SELECT value FROM database_metadata WHERE `key` = :key")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(entity: DatabaseMetadataEntity)

    @Query("DELETE FROM database_metadata WHERE `key` = :key")
    suspend fun delete(key: String)
}

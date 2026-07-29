package com.togetherly.data.local.saved

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_quest",
    indices = [Index("savedAtEpochMillis")],
)
internal data class SavedQuestEntity(
    @PrimaryKey
    val questId: String,
    val savedAtEpochMillis: Long,
)

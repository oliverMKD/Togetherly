package com.togetherly.data.local.completion

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quest_completion",
    indices = [
        Index("familyId"),
        Index("questId"),
        Index("completedAtEpochMillis"),
    ],
)
internal data class QuestCompletionEntity(
    @PrimaryKey
    val id: String,
    val familyId: String,
    val questId: String,
    val questVersion: Int,
    val startedAtEpochMillis: Long?,
    val completedAtEpochMillis: Long,
    val note: String?,
)

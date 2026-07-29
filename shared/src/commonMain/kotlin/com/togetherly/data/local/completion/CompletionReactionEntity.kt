package com.togetherly.data.local.completion

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "completion_reaction",
    primaryKeys = ["completionId", "reaction"],
    foreignKeys = [
        ForeignKey(
            entity = QuestCompletionEntity::class,
            parentColumns = ["id"],
            childColumns = ["completionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("completionId")],
)
internal data class CompletionReactionEntity(
    val completionId: String,
    val reaction: String,
)

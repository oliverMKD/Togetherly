package com.togetherly.data.local.completion

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * [localReference] stores an app-controlled reference/identifier only — never media bytes, an
 * Android URI, an iOS URL object, or a public URL (see
 * [com.togetherly.domain.completion.MediaReference]'s own KDoc). The unique
 * (completionId, type) index enforces at most one photo and one voice memory per completion at
 * the storage level; [durationMillis] should be null for a photo and positive for a voice memory,
 * enforced later once domain mapping exists (Step 6.2 stores raw columns only).
 */
@Entity(
    tableName = "memory_media",
    foreignKeys = [
        ForeignKey(
            entity = QuestCompletionEntity::class,
            parentColumns = ["id"],
            childColumns = ["completionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("completionId"),
        Index(value = ["completionId", "type"], unique = true),
    ],
)
internal data class MemoryMediaEntity(
    @PrimaryKey
    val id: String,
    val completionId: String,
    val type: String,
    val localReference: String,
    val durationMillis: Long?,
)

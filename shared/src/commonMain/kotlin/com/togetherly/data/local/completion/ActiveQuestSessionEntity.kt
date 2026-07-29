package com.togetherly.data.local.completion

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * MVP supports exactly one active session, stored under this one documented slot. This slot ID is
 * purely a database row identity — it's independent of and never substitutes for the domain
 * [com.togetherly.domain.completion.CompletionId].
 */
internal const val ACTIVE_QUEST_SESSION_SLOT_ID = 0

@Entity(tableName = "active_quest_session")
internal data class ActiveQuestSessionEntity(
    @PrimaryKey
    val slotId: Int,
    val completionId: String,
    val familyId: String,
    val questId: String,
    val questVersion: Int,
    val startedAtEpochMillis: Long,
)

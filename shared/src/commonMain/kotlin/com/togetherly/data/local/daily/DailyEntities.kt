package com.togetherly.data.local.daily

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * [localDate] (ISO-8601, `kotlinx.datetime.LocalDate.toString()`) is the primary key — exactly
 * one selection exists per date, so no separate surrogate ID is needed. The `context*` columns
 * mirror [com.togetherly.domain.daily.QuestContext]'s fields; each is `null` exactly when that
 * field was absent (deferring to the family's normal preference), never a sentinel string.
 */
@Entity(
    tableName = "daily_quest",
    indices = [Index(value = ["localDate"], unique = true)],
)
internal data class DailyQuestEntity(
    @PrimaryKey
    val localDate: String,
    val questId: String,
    val selectionIndex: Int,
    val selectedAtEpochMillis: Long,
    val source: String,
    val contextDuration: String?,
    val contextLocation: String?,
    val contextEnergy: String?,
    val contextPreparation: String?,
    val contextCategory: String?,
)

@Entity(
    tableName = "dismissed_quest",
    primaryKeys = ["questId", "dismissedAtEpochMillis"],
    indices = [
        Index("questId"),
        Index("dismissedAtEpochMillis"),
        Index("localDate"),
    ],
)
internal data class DismissedQuestEntity(
    val questId: String,
    val dismissedAtEpochMillis: Long,
    val localDate: String,
)

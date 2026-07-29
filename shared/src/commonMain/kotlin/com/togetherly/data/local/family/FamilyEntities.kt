package com.togetherly.data.local.family

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * MVP has exactly one row, keyed by the real [com.togetherly.domain.family.FamilyId] — never a
 * fabricated constant primary key. [reminderLocalTime] is an ISO-8601 string
 * (`kotlinx.datetime.LocalTime.toString()`/`.parse()`); it's `null` exactly when reminders are
 * disabled, matching the absence of any [FamilyReminderDayEntity] rows for this family.
 *
 * [allowPhotos]/[allowVoiceMemories]/[diagnosticsEnabled] (schema version 2, Step 13.1) and
 * [allowTextNotes]/[showMemoryPromptAfterQuests] (schema version 4, Step 13.5 — replacing the
 * never-enforced `defaultSaveNote` column, see `MIGRATION_3_4`'s own KDoc) are Family Settings
 * columns on this same row, not a second table — see [com.togetherly.domain.family.FamilySettings]'s
 * own KDoc for why. Every one of them was added with a `NOT NULL DEFAULT` so an upgrading install's
 * existing row is backfilled with a safe value automatically, never left `NULL`.
 */
@Entity(tableName = "family_profile")
internal data class FamilyProfileEntity(
    @PrimaryKey
    val id: String,
    val displayName: String?,
    val locationPreference: String,
    val preparationPreference: String,
    val reminderLocalTime: String?,
    val allowPhotos: Boolean,
    val allowVoiceMemories: Boolean,
    val allowTextNotes: Boolean,
    val diagnosticsEnabled: Boolean,
    val showMemoryPromptAfterQuests: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "family_age_band",
    primaryKeys = ["familyId", "ageBand"],
    foreignKeys = [
        ForeignKey(
            entity = FamilyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["familyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("familyId")],
)
internal data class FamilyAgeBandEntity(
    val familyId: String,
    val ageBand: String,
)

@Entity(
    tableName = "family_interest",
    primaryKeys = ["familyId", "category"],
    foreignKeys = [
        ForeignKey(
            entity = FamilyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["familyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("familyId")],
)
internal data class FamilyInterestEntity(
    val familyId: String,
    val category: String,
)

@Entity(
    tableName = "family_duration_preference",
    primaryKeys = ["familyId", "duration"],
    foreignKeys = [
        ForeignKey(
            entity = FamilyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["familyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("familyId")],
)
internal data class FamilyDurationPreferenceEntity(
    val familyId: String,
    val duration: String,
)

@Entity(
    tableName = "family_energy_preference",
    primaryKeys = ["familyId", "energyLevel"],
    foreignKeys = [
        ForeignKey(
            entity = FamilyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["familyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("familyId")],
)
internal data class FamilyEnergyPreferenceEntity(
    val familyId: String,
    val energyLevel: String,
)

@Entity(
    tableName = "family_reminder_day",
    primaryKeys = ["familyId", "dayOfWeek"],
    foreignKeys = [
        ForeignKey(
            entity = FamilyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["familyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("familyId")],
)
internal data class FamilyReminderDayEntity(
    val familyId: String,
    val dayOfWeek: String,
)

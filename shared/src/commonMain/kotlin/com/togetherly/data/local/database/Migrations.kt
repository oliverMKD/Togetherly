package com.togetherly.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * The first real migration this database has ever shipped (see [TogetherlyDatabase]'s own KDoc on
 * why version 1 stays immutable). Adds Family Settings (Step 13.1) as four new columns on the
 * existing `family_profile` row — never a new table, see [com.togetherly.domain.family.FamilySettings]'s
 * own KDoc for why. Every `ADD COLUMN` carries a `NOT NULL DEFAULT`, so SQLite backfills every
 * existing row with a safe value in the same statement — an upgrading install never ends up with a
 * `NULL` settings column, and no separate backfill pass is needed.
 *
 * Defaults match [com.togetherly.domain.family.MemoryPreferences.defaults]/
 * [com.togetherly.domain.family.PrivacyPreferences.defaults] exactly — `allowPhotos`/
 * `allowVoiceMemories` default to enabled (`1`) so an existing family's app behavior is unchanged
 * by this upgrade (photo/voice memory capture was already unconditionally available before this
 * setting existed); `defaultSaveNote`/`diagnosticsEnabled` default to off (`0`).
 */
internal val MIGRATION_1_2: Migration = object : Migration(startVersion = 1, endVersion = 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE family_profile ADD COLUMN allowPhotos INTEGER NOT NULL DEFAULT 1")
        connection.execSQL("ALTER TABLE family_profile ADD COLUMN allowVoiceMemories INTEGER NOT NULL DEFAULT 1")
        connection.execSQL("ALTER TABLE family_profile ADD COLUMN defaultSaveNote INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE family_profile ADD COLUMN diagnosticsEnabled INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * Adds [com.togetherly.data.local.family.FamilyEnergyPreferenceEntity] (Step 13.3) — a new child
 * table, not a column, same shape as `family_duration_preference`: a family's energy preference is
 * a set, and (unlike age bands/interests/durations) is allowed to be empty — "no preference set,"
 * never corrupted storage (see [com.togetherly.domain.family.FamilyProfile]'s own KDoc) — so no
 * backfill is needed for existing rows; they simply start with zero energy-preference rows, which
 * [com.togetherly.data.local.mapper.FamilyProfileMapper.toDomain] already treats as a valid empty set.
 */
internal val MIGRATION_2_3: Migration = object : Migration(startVersion = 2, endVersion = 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `family_energy_preference` (`familyId` TEXT NOT NULL, `energyLevel` TEXT NOT NULL, " +
                "PRIMARY KEY(`familyId`, `energyLevel`), FOREIGN KEY(`familyId`) REFERENCES `family_profile`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_family_energy_preference_familyId` ON `family_energy_preference` (`familyId`)",
        )
    }
}

/**
 * Replaces `defaultSaveNote` with [com.togetherly.data.local.family.FamilyProfileEntity.allowTextNotes]
 * (Step 13.5) — the old column had zero readers anywhere in the app (its "default-expanded note"
 * meaning was never wired to any UI; see [com.togetherly.domain.family.MemoryPreferences]'s own
 * KDoc), so it's dropped rather than left as dead storage alongside a new column. Also adds
 * [com.togetherly.data.local.family.FamilyProfileEntity.showMemoryPromptAfterQuests]. Both new
 * columns default to `1` (true) — every one of these capabilities (a text note, the post-quest
 * "add a memory" prompt) was already unconditionally available/shown before this setting existed,
 * so an upgrading install must never silently lose one.
 */
internal val MIGRATION_3_4: Migration = object : Migration(startVersion = 3, endVersion = 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE family_profile DROP COLUMN defaultSaveNote")
        connection.execSQL("ALTER TABLE family_profile ADD COLUMN allowTextNotes INTEGER NOT NULL DEFAULT 1")
        connection.execSQL("ALTER TABLE family_profile ADD COLUMN showMemoryPromptAfterQuests INTEGER NOT NULL DEFAULT 1")
    }
}

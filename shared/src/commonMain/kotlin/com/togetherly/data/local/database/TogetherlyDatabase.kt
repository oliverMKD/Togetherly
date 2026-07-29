package com.togetherly.data.local.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.togetherly.data.local.completion.ActiveQuestSessionEntity
import com.togetherly.data.local.completion.CompletionDao
import com.togetherly.data.local.completion.CompletionReactionEntity
import com.togetherly.data.local.completion.MemoryMediaEntity
import com.togetherly.data.local.completion.QuestCompletionEntity
import com.togetherly.data.local.daily.DailyQuestDao
import com.togetherly.data.local.daily.DailyQuestEntity
import com.togetherly.data.local.daily.DismissedQuestEntity
import com.togetherly.data.local.family.FamilyAgeBandEntity
import com.togetherly.data.local.family.FamilyDao
import com.togetherly.data.local.family.FamilyDurationPreferenceEntity
import com.togetherly.data.local.family.FamilyEnergyPreferenceEntity
import com.togetherly.data.local.family.FamilyInterestEntity
import com.togetherly.data.local.family.FamilyProfileEntity
import com.togetherly.data.local.family.FamilyReminderDayEntity
import com.togetherly.data.local.saved.SavedQuestDao
import com.togetherly.data.local.saved.SavedQuestEntity

/**
 * [DatabaseMetadataEntity] started as temporary proof that Room code generation works end to end
 * (Step 6.1); it stays registered as a genuinely useful place for catalogue/database metadata
 * (e.g. a last successful sync timestamp). Every other entity here is the version-1 persistence
 * schema (Step 6.2) — no quest catalogue table exists or ever should; that content stays
 * JSON-backed (see docs/content-system.md).
 *
 * **Version 1 (and its exported `shared/schemas/.../1.json`) is immutable** — the moment any
 * external tester or user installed a build with it, any further column/table/index change became
 * a version 2+ migration, never an edit to the existing entities in place. **Version 2** (Step
 * 13.1) adds Family Settings columns to [FamilyProfileEntity] via [MIGRATION_1_2] — see that
 * migration's own KDoc. **Version 3** (Step 13.3) adds [FamilyEnergyPreferenceEntity], a new child
 * table (not a column) since a family's energy preference is a set, same shape as
 * [FamilyDurationPreferenceEntity] — see [MIGRATION_2_3]. **Version 4** (Step 13.5) replaces the
 * never-enforced `defaultSaveNote` column with `allowTextNotes` and adds `showMemoryPromptAfterQuests`,
 * both on [FamilyProfileEntity] — see [MIGRATION_3_4]. From here on, the same rule applies to
 * `2.json`/`3.json`/`4.json`: once shipped, each is immutable. See
 * [com.togetherly.data.local.database.TogetherlyDatabaseMigrationTest] for the migration tests this
 * project keeps for exactly this, and docs/persistence.md for the full migration/corruption
 * policy. No destructive-migration fallback is configured anywhere (see [buildTogetherlyDatabase]'s
 * own KDoc) — a missing migration path fails loudly instead of silently deleting a family's data.
 */
@Database(
    entities = [
        DatabaseMetadataEntity::class,
        FamilyProfileEntity::class,
        FamilyAgeBandEntity::class,
        FamilyInterestEntity::class,
        FamilyDurationPreferenceEntity::class,
        FamilyEnergyPreferenceEntity::class,
        FamilyReminderDayEntity::class,
        DailyQuestEntity::class,
        DismissedQuestEntity::class,
        SavedQuestEntity::class,
        ActiveQuestSessionEntity::class,
        QuestCompletionEntity::class,
        CompletionReactionEntity::class,
        MemoryMediaEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@ConstructedBy(TogetherlyDatabaseConstructor::class)
internal abstract class TogetherlyDatabase : RoomDatabase() {

    abstract fun metadataDao(): DatabaseMetadataDao
    abstract fun familyDao(): FamilyDao
    abstract fun dailyQuestDao(): DailyQuestDao
    abstract fun savedQuestDao(): SavedQuestDao
    abstract fun completionDao(): CompletionDao
}

/**
 * The Room KSP compiler generates the `actual` implementation of this object for every target
 * automatically — nothing here needs a hand-written `actual object` per platform.
 */
@Suppress("KotlinNoActualForExpect")
internal expect object TogetherlyDatabaseConstructor : RoomDatabaseConstructor<TogetherlyDatabase> {
    override fun initialize(): TogetherlyDatabase
}

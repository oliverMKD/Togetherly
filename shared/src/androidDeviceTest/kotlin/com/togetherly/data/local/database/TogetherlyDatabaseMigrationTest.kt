package com.togetherly.data.local.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Migration test scaffolding (Step 6.6) — [TogetherlyDatabase] has only ever shipped as version 1
 * (see its own KDoc), so there is no real migration to test yet. This proves the *mechanism*
 * works today, against the real exported schema in `shared/schemas/`, rather than leaving it
 * unverified until the day a version 2 migration actually lands.
 *
 * [MigrationTestHelper]'s driver-based constructor (not its older `SupportSQLiteOpenHelper.Factory`
 * one) is required here: [buildTogetherlyDatabase] configures every platform with
 * [BundledSQLiteDriver] (see its own KDoc), and the legacy constructor throws
 * `IllegalStateException` at runtime when asked to open a driver-backed database — confirmed by
 * running this test against the wrong constructor before settling on this one. [databaseFactory]
 * is exactly [buildTogetherlyDatabase]'s own production construction path: [MigrationTestHelper]
 * uses it internally to validate a migrated database against Room's real generated schema.
 *
 * The Room Gradle plugin (`room { schemaDirectory(...) }` in `shared/build.gradle.kts`) copies
 * `shared/schemas/` into this source set's test assets automatically (see the
 * `copyRoomSchemasToAndroidTestAssetsAndroidDeviceTest` task) — nothing here does that by hand.
 *
 * **When a version 2 migration is introduced:** add a `Migration(1, 2) { ... }` to
 * [TogetherlyDatabase]'s builder configuration, keep the exported `1.json` file exactly as
 * committed (see [TogetherlyDatabase]'s own KDoc on why version 1 must stay immutable once real
 * users exist), and add a test here in the same shape as [schemaVersion1CanBeCreatedFromTheExportedSchema]
 * but calling `migrationTestHelper.runMigrationsAndValidate(2, listOf(theNewMigration))` instead of
 * `createDatabase(1)` — [MigrationTestHelper] then opens the exported v1 schema, runs the real
 * migration against it, and validates the result against the current schema, catching a missing
 * column/table/index the same way a real upgrading install would hit it.
 */
@RunWith(AndroidJUnit4::class)
internal class TogetherlyDatabaseMigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val databaseFile = instrumentation.targetContext.getDatabasePath("migration-test.db")

    @get:Rule
    val migrationTestHelper: MigrationTestHelper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = databaseFile,
        driver = BundledSQLiteDriver(),
        databaseClass = TogetherlyDatabase::class,
        databaseFactory = {
            buildTogetherlyDatabase(
                Room.databaseBuilder<TogetherlyDatabase>(context = instrumentation.targetContext, name = databaseFile.path),
            )
        },
        autoMigrationSpecs = emptyList(),
    )

    @Test
    fun schemaVersion1CanBeCreatedFromTheExportedSchema() {
        migrationTestHelper.createDatabase(1).close()
    }

    /**
     * The real target of this test (Step 13.1): an install that already has a family profile
     * (and its age-band/interest/duration child rows) must keep every one of those rows intact
     * after upgrading, with the four new Family Settings columns backfilled to their documented
     * defaults ([MIGRATION_1_2]'s own KDoc) — never `NULL`, never a data loss.
     */
    @Test
    fun migration1To2PreservesAnExistingProfileAndBackfillsSettingsDefaults() {
        val connection = migrationTestHelper.createDatabase(1)
        connection.execSQL(
            """
            INSERT INTO family_profile (
                id, displayName, locationPreference, preparationPreference, reminderLocalTime,
                createdAtEpochMillis, updatedAtEpochMillis
            ) VALUES (
                'family-1', 'The Smiths', 'both', 'any', NULL, 1000, 1000
            )
            """.trimIndent(),
        )
        connection.execSQL("INSERT INTO family_age_band (familyId, ageBand) VALUES ('family-1', 'age_6_8')")
        connection.execSQL("INSERT INTO family_interest (familyId, category) VALUES ('family-1', 'talk')")
        connection.execSQL("INSERT INTO family_duration_preference (familyId, duration) VALUES ('family-1', 'ten_minutes')")
        connection.close()

        migrationTestHelper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2))

        val migratedDatabase = buildTogetherlyDatabase(
            Room.databaseBuilder<TogetherlyDatabase>(context = instrumentation.targetContext, name = databaseFile.path),
        )
        try {
            runBlocking {
                val relation = requireNotNull(migratedDatabase.familyDao().getFamilyProfile())
                assertEquals("family-1", relation.profile.id)
                assertEquals("The Smiths", relation.profile.displayName)
                assertNull(relation.profile.reminderLocalTime)
                assertEquals(listOf("age_6_8"), relation.ageBands.map { it.ageBand })
                assertEquals(listOf("talk"), relation.interests.map { it.category })
                assertEquals(listOf("ten_minutes"), relation.durationPreferences.map { it.duration })
                // Backfilled defaults — never NULL, matching MemoryPreferences.defaults()/PrivacyPreferences.defaults().
                // Opening via buildTogetherlyDatabase (not MigrationTestHelper) chains every
                // registered migration, so this row has already gone through MIGRATION_2_3 and
                // MIGRATION_3_4 too — asserting against the *current* (v4) column shape.
                assertEquals(true, relation.profile.allowPhotos)
                assertEquals(true, relation.profile.allowVoiceMemories)
                assertEquals(true, relation.profile.allowTextNotes)
                assertEquals(false, relation.profile.diagnosticsEnabled)
                assertEquals(true, relation.profile.showMemoryPromptAfterQuests)
            }
        } finally {
            migratedDatabase.close()
        }
    }

    /**
     * The real target of this test (Step 13.3): an install upgrading straight from version 2
     * (no `family_energy_preference` table yet) must keep its existing profile and every other
     * preference child row intact, landing with zero energy-preference rows — a valid empty set
     * (see [com.togetherly.domain.family.FamilyProfile]'s own KDoc), never a migration failure.
     */
    @Test
    fun migration2To3PreservesAnExistingProfileAndAddsAnEmptyEnergyPreferenceTable() {
        val connection = migrationTestHelper.createDatabase(2)
        connection.execSQL(
            """
            INSERT INTO family_profile (
                id, displayName, locationPreference, preparationPreference, reminderLocalTime,
                allowPhotos, allowVoiceMemories, defaultSaveNote, diagnosticsEnabled,
                createdAtEpochMillis, updatedAtEpochMillis
            ) VALUES (
                'family-1', 'The Smiths', 'both', 'any', NULL, 1, 1, 0, 0, 1000, 1000
            )
            """.trimIndent(),
        )
        connection.execSQL("INSERT INTO family_age_band (familyId, ageBand) VALUES ('family-1', 'age_6_8')")
        connection.execSQL("INSERT INTO family_interest (familyId, category) VALUES ('family-1', 'talk')")
        connection.execSQL("INSERT INTO family_duration_preference (familyId, duration) VALUES ('family-1', 'ten_minutes')")
        connection.close()

        migrationTestHelper.runMigrationsAndValidate(3, listOf(MIGRATION_2_3))

        val migratedDatabase = buildTogetherlyDatabase(
            Room.databaseBuilder<TogetherlyDatabase>(context = instrumentation.targetContext, name = databaseFile.path),
        )
        try {
            runBlocking {
                val relation = requireNotNull(migratedDatabase.familyDao().getFamilyProfile())
                assertEquals("family-1", relation.profile.id)
                assertEquals(listOf("age_6_8"), relation.ageBands.map { it.ageBand })
                assertEquals(listOf("talk"), relation.interests.map { it.category })
                assertEquals(listOf("ten_minutes"), relation.durationPreferences.map { it.duration })
                assertTrue(relation.energyPreferences.isEmpty())
            }
        } finally {
            migratedDatabase.close()
        }
    }

    /**
     * The real target of this test (Step 13.5): an install upgrading straight from version 3 must
     * keep its existing profile and preference rows intact, with `allowTextNotes`/
     * `showMemoryPromptAfterQuests` backfilled to `true` — the same "never silently lose a
     * capability already available" guarantee [MIGRATION_1_2] established for photo/voice. The old
     * `defaultSaveNote` value (`1` here, deliberately the non-default value) is simply dropped —
     * there is nothing to preserve since nothing ever read it.
     */
    @Test
    fun migration3To4PreservesAnExistingProfileAndBackfillsMemoryPromptDefaults() {
        val connection = migrationTestHelper.createDatabase(3)
        connection.execSQL(
            """
            INSERT INTO family_profile (
                id, displayName, locationPreference, preparationPreference, reminderLocalTime,
                allowPhotos, allowVoiceMemories, defaultSaveNote, diagnosticsEnabled,
                createdAtEpochMillis, updatedAtEpochMillis
            ) VALUES (
                'family-1', 'The Smiths', 'both', 'any', NULL, 1, 1, 1, 0, 1000, 1000
            )
            """.trimIndent(),
        )
        connection.execSQL("INSERT INTO family_age_band (familyId, ageBand) VALUES ('family-1', 'age_6_8')")
        connection.execSQL("INSERT INTO family_interest (familyId, category) VALUES ('family-1', 'talk')")
        connection.execSQL("INSERT INTO family_duration_preference (familyId, duration) VALUES ('family-1', 'ten_minutes')")
        connection.execSQL("INSERT INTO family_energy_preference (familyId, energyLevel) VALUES ('family-1', 'calm')")
        connection.close()

        migrationTestHelper.runMigrationsAndValidate(4, listOf(MIGRATION_3_4))

        val migratedDatabase = buildTogetherlyDatabase(
            Room.databaseBuilder<TogetherlyDatabase>(context = instrumentation.targetContext, name = databaseFile.path),
        )
        try {
            runBlocking {
                val relation = requireNotNull(migratedDatabase.familyDao().getFamilyProfile())
                assertEquals("family-1", relation.profile.id)
                assertEquals(listOf("age_6_8"), relation.ageBands.map { it.ageBand })
                assertEquals(listOf("calm"), relation.energyPreferences.map { it.energyLevel })
                assertEquals(true, relation.profile.allowPhotos)
                assertEquals(true, relation.profile.allowVoiceMemories)
                assertEquals(true, relation.profile.allowTextNotes)
                assertEquals(true, relation.profile.showMemoryPromptAfterQuests)
            }
        } finally {
            migratedDatabase.close()
        }
    }
}

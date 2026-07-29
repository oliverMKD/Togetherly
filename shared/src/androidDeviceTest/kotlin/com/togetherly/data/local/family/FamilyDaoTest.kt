package com.togetherly.data.local.family

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.data.local.RoomDaoTest
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun testProfileEntity(id: String = "family-1") = FamilyProfileEntity(
    id = id,
    displayName = "The Smiths",
    locationPreference = "both",
    preparationPreference = "any",
    reminderLocalTime = "09:30:00",
    allowPhotos = true,
    allowVoiceMemories = true,
    allowTextNotes = true,
    diagnosticsEnabled = false,
    showMemoryPromptAfterQuests = true,
    createdAtEpochMillis = 1_000L,
    updatedAtEpochMillis = 1_000L,
)

@RunWith(AndroidJUnit4::class)
internal class FamilyDaoTest : RoomDaoTest() {

    private val dao get() = database.familyDao()

    @Test
    fun replaceFamilyProfileAssemblesTheCompleteRelation() = runTest {
        val familyId = "family-1"

        dao.replaceFamilyProfile(
            profile = testProfileEntity(familyId),
            ageBands = listOf(FamilyAgeBandEntity(familyId, "age_6_8"), FamilyAgeBandEntity(familyId, "age_9_11")),
            interests = listOf(FamilyInterestEntity(familyId, "talk"), FamilyInterestEntity(familyId, "move")),
            durationPreferences = listOf(FamilyDurationPreferenceEntity(familyId, "ten_minutes")),
            energyPreferences = listOf(FamilyEnergyPreferenceEntity(familyId, "calm"), FamilyEnergyPreferenceEntity(familyId, "active")),
            reminderDays = listOf(FamilyReminderDayEntity(familyId, "monday"), FamilyReminderDayEntity(familyId, "friday")),
        )

        val relation = requireNotNull(dao.getFamilyProfile())
        assertEquals(familyId, relation.profile.id)
        assertEquals("The Smiths", relation.profile.displayName)
        assertEquals(setOf("age_6_8", "age_9_11"), relation.ageBands.map { it.ageBand }.toSet())
        assertEquals(setOf("talk", "move"), relation.interests.map { it.category }.toSet())
        assertEquals(listOf("ten_minutes"), relation.durationPreferences.map { it.duration })
        assertEquals(setOf("calm", "active"), relation.energyPreferences.map { it.energyLevel }.toSet())
        assertEquals(setOf("monday", "friday"), relation.reminderDays.map { it.dayOfWeek }.toSet())
    }

    @Test
    fun replaceFamilyProfileReplacesPreviousChildRowsRatherThanAccumulating() = runTest {
        val familyId = "family-1"
        dao.replaceFamilyProfile(
            profile = testProfileEntity(familyId),
            ageBands = listOf(FamilyAgeBandEntity(familyId, "age_6_8")),
            interests = listOf(FamilyInterestEntity(familyId, "talk")),
            durationPreferences = listOf(FamilyDurationPreferenceEntity(familyId, "ten_minutes")),
            energyPreferences = listOf(FamilyEnergyPreferenceEntity(familyId, "calm")),
            reminderDays = listOf(FamilyReminderDayEntity(familyId, "monday")),
        )

        dao.replaceFamilyProfile(
            profile = testProfileEntity(familyId),
            ageBands = listOf(FamilyAgeBandEntity(familyId, "age_12_13")),
            interests = listOf(FamilyInterestEntity(familyId, "discover")),
            durationPreferences = listOf(FamilyDurationPreferenceEntity(familyId, "twenty_minutes")),
            energyPreferences = emptyList(),
            reminderDays = emptyList(),
        )

        val relation = requireNotNull(dao.getFamilyProfile())
        assertEquals(listOf("age_12_13"), relation.ageBands.map { it.ageBand })
        assertEquals(listOf("discover"), relation.interests.map { it.category })
        assertEquals(listOf("twenty_minutes"), relation.durationPreferences.map { it.duration })
        assertTrue(relation.energyPreferences.isEmpty())
        assertTrue(relation.reminderDays.isEmpty())
    }

    @Test
    fun deletingProfileCascadesToPreferenceChildRows() = runTest {
        val familyId = "family-1"
        // Raw inserts, not replaceFamilyProfile, so this test proves the foreign-key CASCADE
        // itself — not replaceFamilyProfile's own explicit delete-then-insert logic.
        dao.insertProfile(testProfileEntity(familyId))
        dao.insertAgeBands(listOf(FamilyAgeBandEntity(familyId, "age_6_8")))
        dao.insertInterests(listOf(FamilyInterestEntity(familyId, "talk")))
        dao.insertDurationPreferences(listOf(FamilyDurationPreferenceEntity(familyId, "ten_minutes")))
        dao.insertEnergyPreferences(listOf(FamilyEnergyPreferenceEntity(familyId, "calm")))
        dao.insertReminderDays(listOf(FamilyReminderDayEntity(familyId, "monday")))

        dao.deleteProfile(familyId)

        // Re-create a bare profile with the same ID and no children. If the cascade hadn't fired,
        // the old child rows (which reference this familyId) would still resolve here.
        dao.insertProfile(testProfileEntity(familyId))

        val relation = requireNotNull(dao.getFamilyProfile())
        assertTrue(relation.ageBands.isEmpty())
        assertTrue(relation.interests.isEmpty())
        assertTrue(relation.durationPreferences.isEmpty())
        assertTrue(relation.energyPreferences.isEmpty())
        assertTrue(relation.reminderDays.isEmpty())
    }

    @Test
    fun noProfileReturnsNull() = runTest {
        assertNull(dao.getFamilyProfile())
    }

    @Test
    fun deleteAllProfilesRemovesTheProfile() = runTest {
        dao.insertProfile(testProfileEntity())

        dao.deleteAllProfiles()

        assertNull(dao.getFamilyProfile())
    }

    @Test
    fun updateQuestPreferencesTouchesOnlyQuestPreferenceColumnsAndChildRows() = runTest {
        val familyId = "family-1"
        dao.replaceFamilyProfile(
            profile = testProfileEntity(familyId),
            ageBands = listOf(FamilyAgeBandEntity(familyId, "age_6_8")),
            interests = listOf(FamilyInterestEntity(familyId, "talk")),
            durationPreferences = listOf(FamilyDurationPreferenceEntity(familyId, "ten_minutes")),
            energyPreferences = listOf(FamilyEnergyPreferenceEntity(familyId, "calm")),
            reminderDays = listOf(FamilyReminderDayEntity(familyId, "monday")),
        )

        dao.updateQuestPreferences(
            familyId = familyId,
            locationPreference = "outdoor",
            preparationPreference = "none",
            interests = listOf(FamilyInterestEntity(familyId, "discover")),
            durationPreferences = listOf(FamilyDurationPreferenceEntity(familyId, "twenty_minutes")),
            energyPreferences = listOf(FamilyEnergyPreferenceEntity(familyId, "active")),
            updatedAtEpochMillis = 2_000L,
        )

        val relation = requireNotNull(dao.getFamilyProfile())
        assertEquals("outdoor", relation.profile.locationPreference)
        assertEquals("none", relation.profile.preparationPreference)
        assertEquals(listOf("discover"), relation.interests.map { it.category })
        assertEquals(listOf("twenty_minutes"), relation.durationPreferences.map { it.duration })
        assertEquals(listOf("active"), relation.energyPreferences.map { it.energyLevel })
        assertEquals(2_000L, relation.profile.updatedAtEpochMillis)
        // Untouched: age bands and reminder days survive a quest-preferences-only update.
        assertEquals(listOf("age_6_8"), relation.ageBands.map { it.ageBand })
        assertEquals(listOf("monday"), relation.reminderDays.map { it.dayOfWeek })
    }

    @Test
    fun updateReminderPreferenceTouchesOnlyTheReminderColumnAndChildRows() = runTest {
        val familyId = "family-1"
        dao.replaceFamilyProfile(
            profile = testProfileEntity(familyId),
            ageBands = listOf(FamilyAgeBandEntity(familyId, "age_6_8")),
            interests = listOf(FamilyInterestEntity(familyId, "talk")),
            durationPreferences = listOf(FamilyDurationPreferenceEntity(familyId, "ten_minutes")),
            energyPreferences = listOf(FamilyEnergyPreferenceEntity(familyId, "calm")),
            reminderDays = listOf(FamilyReminderDayEntity(familyId, "monday")),
        )

        dao.updateReminderPreference(
            familyId = familyId,
            reminderLocalTime = null,
            reminderDays = emptyList(),
            updatedAtEpochMillis = 3_000L,
        )

        val relation = requireNotNull(dao.getFamilyProfile())
        assertNull(relation.profile.reminderLocalTime)
        assertTrue(relation.reminderDays.isEmpty())
        assertEquals(3_000L, relation.profile.updatedAtEpochMillis)
        // Untouched: quest preferences survive a reminder-only update.
        assertEquals(listOf("talk"), relation.interests.map { it.category })
    }

    @Test
    fun updateMemoryPreferencesTouchesOnlyItsOwnColumns() = runTest {
        val familyId = "family-1"
        dao.insertProfile(testProfileEntity(familyId))

        dao.updateMemoryPreferences(
            familyId = familyId,
            allowPhotos = false,
            allowVoiceMemories = false,
            allowTextNotes = false,
            showMemoryPromptAfterQuests = false,
            updatedAtEpochMillis = 4_000L,
        )

        val relation = requireNotNull(dao.getFamilyProfile())
        assertEquals(false, relation.profile.allowPhotos)
        assertEquals(false, relation.profile.allowVoiceMemories)
        assertEquals(false, relation.profile.allowTextNotes)
        assertEquals(false, relation.profile.showMemoryPromptAfterQuests)
        assertEquals(4_000L, relation.profile.updatedAtEpochMillis)
        // Untouched: privacy preference survives a memory-preferences-only update.
        assertEquals(false, relation.profile.diagnosticsEnabled)
    }

    @Test
    fun updatePrivacyPreferencesTouchesOnlyItsOwnColumn() = runTest {
        val familyId = "family-1"
        dao.insertProfile(testProfileEntity(familyId))

        dao.updatePrivacyPreferences(familyId = familyId, diagnosticsEnabled = true, updatedAtEpochMillis = 5_000L)

        val relation = requireNotNull(dao.getFamilyProfile())
        assertEquals(true, relation.profile.diagnosticsEnabled)
        assertEquals(5_000L, relation.profile.updatedAtEpochMillis)
        // Untouched: memory preferences survive a privacy-preferences-only update.
        assertEquals(true, relation.profile.allowPhotos)
    }
}

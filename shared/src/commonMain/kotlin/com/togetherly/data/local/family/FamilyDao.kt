package com.togetherly.data.local.family

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * MVP has exactly one profile, so every read is unqualified ("the" profile, `LIMIT 1`) rather
 * than keyed by ID — the repository layer (later) is what enforces the one-profile rule, this DAO
 * just reflects it. [replaceFamilyProfile] is the one multi-table write: a single `@Transaction`
 * function with a body, Room's supported pattern for wrapping several DAO calls atomically.
 */
@Dao
internal interface FamilyDao {

    @Transaction
    @Query("SELECT * FROM family_profile LIMIT 1")
    fun observeFamilyProfile(): Flow<FamilyProfileWithDetails?>

    @Transaction
    @Query("SELECT * FROM family_profile LIMIT 1")
    suspend fun getFamilyProfile(): FamilyProfileWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(entity: FamilyProfileEntity)

    @Query("DELETE FROM family_age_band WHERE familyId = :familyId")
    suspend fun deleteAgeBands(familyId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgeBands(entities: List<FamilyAgeBandEntity>)

    @Query("DELETE FROM family_interest WHERE familyId = :familyId")
    suspend fun deleteInterests(familyId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterests(entities: List<FamilyInterestEntity>)

    @Query("DELETE FROM family_duration_preference WHERE familyId = :familyId")
    suspend fun deleteDurationPreferences(familyId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDurationPreferences(entities: List<FamilyDurationPreferenceEntity>)

    @Query("DELETE FROM family_energy_preference WHERE familyId = :familyId")
    suspend fun deleteEnergyPreferences(familyId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnergyPreferences(entities: List<FamilyEnergyPreferenceEntity>)

    @Query("DELETE FROM family_reminder_day WHERE familyId = :familyId")
    suspend fun deleteReminderDays(familyId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderDays(entities: List<FamilyReminderDayEntity>)

    @Transaction
    suspend fun replaceFamilyProfile(
        profile: FamilyProfileEntity,
        ageBands: List<FamilyAgeBandEntity>,
        interests: List<FamilyInterestEntity>,
        durationPreferences: List<FamilyDurationPreferenceEntity>,
        energyPreferences: List<FamilyEnergyPreferenceEntity>,
        reminderDays: List<FamilyReminderDayEntity>,
    ) {
        insertProfile(profile)
        deleteAgeBands(profile.id)
        insertAgeBands(ageBands)
        deleteInterests(profile.id)
        insertInterests(interests)
        deleteDurationPreferences(profile.id)
        insertDurationPreferences(durationPreferences)
        deleteEnergyPreferences(profile.id)
        insertEnergyPreferences(energyPreferences)
        deleteReminderDays(profile.id)
        insertReminderDays(reminderDays)
    }

    @Query("DELETE FROM family_profile WHERE id = :familyId")
    suspend fun deleteProfile(familyId: String)

    @Query("DELETE FROM family_profile")
    suspend fun deleteAllProfiles()

    /**
     * Genuine partial updates (Step 13.1) — unlike [replaceFamilyProfile], each of these touches
     * only its own columns/child rows. [updateQuestPreferences] and [updateReminderPreference]
     * still need their own `@Transaction`, since each spans the profile row plus one child table;
     * [updateMemoryPreferences]/[updatePrivacyPreferences] are single-row updates and need none.
     */
    @Query(
        "UPDATE family_profile SET locationPreference = :locationPreference, " +
            "preparationPreference = :preparationPreference, updatedAtEpochMillis = :updatedAtEpochMillis " +
            "WHERE id = :familyId",
    )
    suspend fun updateLocationAndPreparationPreference(
        familyId: String,
        locationPreference: String,
        preparationPreference: String,
        updatedAtEpochMillis: Long,
    )

    @Transaction
    suspend fun updateQuestPreferences(
        familyId: String,
        locationPreference: String,
        preparationPreference: String,
        interests: List<FamilyInterestEntity>,
        durationPreferences: List<FamilyDurationPreferenceEntity>,
        energyPreferences: List<FamilyEnergyPreferenceEntity>,
        updatedAtEpochMillis: Long,
    ) {
        updateLocationAndPreparationPreference(familyId, locationPreference, preparationPreference, updatedAtEpochMillis)
        deleteInterests(familyId)
        insertInterests(interests)
        deleteDurationPreferences(familyId)
        insertDurationPreferences(durationPreferences)
        deleteEnergyPreferences(familyId)
        insertEnergyPreferences(energyPreferences)
    }

    @Query("UPDATE family_profile SET reminderLocalTime = :reminderLocalTime, updatedAtEpochMillis = :updatedAtEpochMillis WHERE id = :familyId")
    suspend fun updateReminderLocalTime(familyId: String, reminderLocalTime: String?, updatedAtEpochMillis: Long)

    @Transaction
    suspend fun updateReminderPreference(
        familyId: String,
        reminderLocalTime: String?,
        reminderDays: List<FamilyReminderDayEntity>,
        updatedAtEpochMillis: Long,
    ) {
        updateReminderLocalTime(familyId, reminderLocalTime, updatedAtEpochMillis)
        deleteReminderDays(familyId)
        insertReminderDays(reminderDays)
    }

    @Query(
        "UPDATE family_profile SET allowPhotos = :allowPhotos, allowVoiceMemories = :allowVoiceMemories, " +
            "allowTextNotes = :allowTextNotes, showMemoryPromptAfterQuests = :showMemoryPromptAfterQuests, " +
            "updatedAtEpochMillis = :updatedAtEpochMillis WHERE id = :familyId",
    )
    suspend fun updateMemoryPreferences(
        familyId: String,
        allowPhotos: Boolean,
        allowVoiceMemories: Boolean,
        allowTextNotes: Boolean,
        showMemoryPromptAfterQuests: Boolean,
        updatedAtEpochMillis: Long,
    )

    @Query("UPDATE family_profile SET diagnosticsEnabled = :diagnosticsEnabled, updatedAtEpochMillis = :updatedAtEpochMillis WHERE id = :familyId")
    suspend fun updatePrivacyPreferences(familyId: String, diagnosticsEnabled: Boolean, updatedAtEpochMillis: Long)
}

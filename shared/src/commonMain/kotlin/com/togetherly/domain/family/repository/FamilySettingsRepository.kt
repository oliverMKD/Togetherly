package com.togetherly.domain.family.repository

import com.togetherly.core.result.DataResult
import com.togetherly.domain.family.FamilySettings
import com.togetherly.domain.family.MemoryPreferences
import com.togetherly.domain.family.PrivacyPreferences
import com.togetherly.domain.family.QuestPreferences
import com.togetherly.domain.family.ReminderPreference
import kotlinx.coroutines.flow.Flow

/**
 * One repository, not four — [MemoryPreferences]/[PrivacyPreferences]/the quest-matching fields
 * and the reminder preference all live on the *same* `family_profile` row [FamilyRepository]
 * already owns (see [com.togetherly.data.family.RoomFamilySettingsRepository]'s own KDoc); a
 * separate `ReminderPreferencesRepository`/`MemoryPreferencesRepository`/`PrivacyPreferencesRepository`
 * would only be reading and writing the same table through a different name. Local data management
 * (wipe) already has its own focused contract — [FamilyDataCleaner] — reused as-is, not duplicated
 * here.
 *
 * Unlike [FamilyRepository.saveProfile] (whole-profile replace only), every `update*` method here
 * is a genuine partial update: it changes only its own columns/child rows, leaving everything else
 * (including [FamilyProfile.childAgeBands] and [FamilyProfile.displayName]) untouched. Every method
 * requires a profile to already exist ([com.togetherly.core.error.ValidationError.MISSING_FAMILY_PROFILE]
 * otherwise) — settings have nothing to attach to before onboarding creates one.
 */
interface FamilySettingsRepository {

    fun observeSettings(): Flow<DataResult<FamilySettings?>>

    suspend fun updateQuestPreferences(preferences: QuestPreferences): DataResult<Unit>

    suspend fun updateReminderPreference(preference: ReminderPreference?): DataResult<Unit>

    suspend fun updateMemoryPreferences(preferences: MemoryPreferences): DataResult<Unit>

    suspend fun updatePrivacyPreferences(preferences: PrivacyPreferences): DataResult<Unit>
}

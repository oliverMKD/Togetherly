package com.togetherly.data.family

import com.togetherly.core.datetime.AppClock
import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.data.catchStorageReadErrors
import com.togetherly.data.local.family.FamilyDao
import com.togetherly.data.local.family.FamilyDurationPreferenceEntity
import com.togetherly.data.local.family.FamilyEnergyPreferenceEntity
import com.togetherly.data.local.family.FamilyInterestEntity
import com.togetherly.data.local.family.FamilyReminderDayEntity
import com.togetherly.data.local.keys.toStorageKey
import com.togetherly.data.local.mapper.FamilyProfileMapper
import com.togetherly.data.runCatchingStorage
import com.togetherly.domain.family.FamilySettings
import com.togetherly.domain.family.MemoryPreferences
import com.togetherly.domain.family.PrivacyPreferences
import com.togetherly.domain.family.QuestPreferences
import com.togetherly.domain.family.ReminderPreference
import com.togetherly.domain.family.repository.FamilySettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Reads and writes the *same* `family_profile` row and its child tables
 * [com.togetherly.domain.family.repository.FamilyRepository] already owns — this repository
 * exists purely to offer a richer [FamilySettings] read and genuinely partial write methods
 * ([FamilyDao.updateQuestPreferences]/[FamilyDao.updateReminderPreference]/
 * [FamilyDao.updateMemoryPreferences]/[FamilyDao.updatePrivacyPreferences]), never a second table
 * or a cached copy of anything [FamilyRepository] reports. Every update requires a profile to
 * already exist, mirroring [com.togetherly.domain.family.usecase.UpdateFamilyProfile]'s own
 * [ValidationError.MISSING_FAMILY_PROFILE] behavior — there is nothing for a setting to attach to
 * before onboarding creates the one family row.
 */
internal class RoomFamilySettingsRepository(
    private val familyDao: FamilyDao,
    private val familyMapper: FamilyProfileMapper,
    private val clock: AppClock,
    private val dispatchers: AppDispatchers,
    private val diagnostics: OperationalDiagnostics,
) : FamilySettingsRepository {

    override fun observeSettings(): Flow<DataResult<FamilySettings?>> =
        familyDao.observeFamilyProfile()
            .map { relation -> relation?.let { familyMapper.toSettingsDomain(it) } ?: DataResult.Success(null) }
            .catchStorageReadErrors(diagnostics)

    override suspend fun updateQuestPreferences(preferences: QuestPreferences): DataResult<Unit> =
        withExistingFamilyId { familyId ->
            familyDao.updateQuestPreferences(
                familyId = familyId,
                locationPreference = preferences.locationPreference.toStorageKey(),
                preparationPreference = preferences.preparationPreference.toStorageKey(),
                interests = preferences.interests.map { FamilyInterestEntity(familyId, it.toStorageKey()) },
                durationPreferences = preferences.preferredDurations.map {
                    FamilyDurationPreferenceEntity(familyId, it.toStorageKey())
                },
                energyPreferences = preferences.preferredEnergyLevels.map {
                    FamilyEnergyPreferenceEntity(familyId, it.toStorageKey())
                },
                updatedAtEpochMillis = clock.now().toEpochMilliseconds(),
            )
        }

    override suspend fun updateReminderPreference(preference: ReminderPreference?): DataResult<Unit> =
        withExistingFamilyId { familyId ->
            familyDao.updateReminderPreference(
                familyId = familyId,
                reminderLocalTime = preference?.localTime?.toString(),
                reminderDays = preference?.enabledDays.orEmpty().map { FamilyReminderDayEntity(familyId, it.toStorageKey()) },
                updatedAtEpochMillis = clock.now().toEpochMilliseconds(),
            )
        }

    override suspend fun updateMemoryPreferences(preferences: MemoryPreferences): DataResult<Unit> =
        withExistingFamilyId { familyId ->
            familyDao.updateMemoryPreferences(
                familyId = familyId,
                allowPhotos = preferences.allowPhotos,
                allowVoiceMemories = preferences.allowVoiceMemories,
                allowTextNotes = preferences.allowTextNotes,
                showMemoryPromptAfterQuests = preferences.showMemoryPromptAfterQuests,
                updatedAtEpochMillis = clock.now().toEpochMilliseconds(),
            )
        }

    override suspend fun updatePrivacyPreferences(preferences: PrivacyPreferences): DataResult<Unit> =
        withExistingFamilyId { familyId ->
            familyDao.updatePrivacyPreferences(
                familyId = familyId,
                diagnosticsEnabled = preferences.diagnosticsEnabled,
                updatedAtEpochMillis = clock.now().toEpochMilliseconds(),
            )
        }

    private suspend fun withExistingFamilyId(write: suspend (familyId: String) -> Unit): DataResult<Unit> =
        dispatchers.runCatchingStorage(StorageError.WRITE_FAILED, diagnostics) {
            val familyId = familyDao.getFamilyProfile()?.profile?.id
                ?: return@runCatchingStorage DataResult.Error(AppError.Validation(ValidationError.MISSING_FAMILY_PROFILE))
            write(familyId)
            DataResult.Success(Unit)
        }
}

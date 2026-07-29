package com.togetherly.data.family

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.data.catchStorageReadErrors
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.data.local.family.FamilyDao
import com.togetherly.data.local.mapper.FamilyProfileMapper
import com.togetherly.data.runCatchingStorage
import com.togetherly.domain.family.FamilyProfile
import com.togetherly.domain.family.MemoryPreferences
import com.togetherly.domain.family.PrivacyPreferences
import com.togetherly.domain.family.repository.FamilyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Family deletion only removes `family_profile` — and, via the foreign-key `CASCADE` already
 * declared on its four preference child tables (Step 6.2), those preference rows along with it.
 * It does **not** touch `quest_completion` or `active_quest_session`: neither entity declares a
 * foreign key back to `family_profile`, a deliberate Step 6.2 decision. Deleting a profile can
 * therefore never silently delete a family's completions or its active session — extending the
 * cascade to those tables would require an explicit, separate foreign-key/schema change, not an
 * implicit side effect of this repository.
 */
internal class RoomFamilyRepository(
    private val familyDao: FamilyDao,
    private val familyMapper: FamilyProfileMapper,
    private val database: TogetherlyDatabase,
    private val dispatchers: AppDispatchers,
    private val diagnostics: OperationalDiagnostics,
) : FamilyRepository {

    override fun observeProfile(): Flow<DataResult<FamilyProfile?>> =
        familyDao.observeFamilyProfile()
            .map { relation -> relation?.let { familyMapper.toDomain(it) } ?: DataResult.Success(null) }
            .catchStorageReadErrors(diagnostics)

    override suspend fun getProfile(): DataResult<FamilyProfile?> =
        dispatchers.runCatchingStorage(StorageError.READ_FAILED, diagnostics) {
            val relation = familyDao.getFamilyProfile() ?: return@runCatchingStorage DataResult.Success(null)
            familyMapper.toDomain(relation)
        }

    /**
     * `RoomDatabase.withTransaction` (the classic `room-ktx` extension) is Android/JVM-only —
     * unresolved on iOS/common. [useWriterConnection]/[immediateTransaction] is the KMP-unified
     * replacement. This wraps the DAO's own already-`@Transaction` [FamilyDao.replaceFamilyProfile]
     * in that transaction too — Room supports this nesting safely — so the repository owns the
     * transaction boundary explicitly rather than relying solely on the DAO method's annotation.
     * A new profile deliberately replaces whatever single-profile state already exists, matching
     * MVP's one-profile design.
     *
     * The existing row's Family Settings columns (Step 13.1 — [FamilyProfileEntity.allowPhotos]
     * and friends) are read first and threaded back into [FamilyProfileMapper.toComponents]:
     * [FamilyProfile] itself never carries them, so a full-row replace that didn't preserve them
     * would silently reset a family's memory/privacy preferences every time
     * [com.togetherly.domain.family.usecase.UpdateFamilyProfile] runs. This keeps `saveProfile`
     * a true "replace [FamilyProfile]'s own fields only," not an accidental settings reset.
     */
    override suspend fun saveProfile(profile: FamilyProfile): DataResult<Unit> =
        dispatchers.runCatchingStorage(StorageError.WRITE_FAILED, diagnostics) {
            val existingEntity = familyDao.getFamilyProfile()?.profile
            val components = familyMapper.toComponents(
                domain = profile,
                memoryPreferences = existingEntity?.let {
                    MemoryPreferences(it.allowPhotos, it.allowVoiceMemories, it.allowTextNotes, it.showMemoryPromptAfterQuests)
                } ?: MemoryPreferences.defaults(),
                privacyPreferences = existingEntity?.let {
                    PrivacyPreferences(it.diagnosticsEnabled)
                } ?: PrivacyPreferences.defaults(),
            )
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    familyDao.replaceFamilyProfile(
                        profile = components.profile,
                        ageBands = components.ageBands,
                        interests = components.interests,
                        durationPreferences = components.durationPreferences,
                        energyPreferences = components.energyPreferences,
                        reminderDays = components.reminderDays,
                    )
                }
            }
            DataResult.Success(Unit)
        }

    /** Idempotent: [FamilyDao.deleteAllProfiles] succeeds whether or not a profile currently exists. */
    override suspend fun deleteProfile(): DataResult<Unit> =
        dispatchers.runCatchingStorage(StorageError.DELETE_FAILED, diagnostics) {
            familyDao.deleteAllProfiles()
            DataResult.Success(Unit)
        }
}

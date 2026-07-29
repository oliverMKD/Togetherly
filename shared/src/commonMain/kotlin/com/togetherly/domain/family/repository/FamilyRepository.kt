package com.togetherly.domain.family.repository

import com.togetherly.core.result.DataResult
import com.togetherly.domain.family.FamilyProfile
import kotlinx.coroutines.flow.Flow

/**
 * Togetherly's MVP supports exactly one family profile on one device — there is no notion of
 * multiple households or profile selection here.
 *
 * [observeProfile] supports UI and other long-lived state; [getProfile] supports one-shot
 * orchestration and use cases that don't need to stay subscribed.
 *
 * A `null` profile means onboarding has not created one yet, never a distinct "not found" error.
 * This repository does not generate IDs or timestamps, and does not validate anything beyond
 * what [FamilyProfile]'s own invariants already guarantee — callers must supply an already-valid
 * profile.
 */
interface FamilyRepository {

    fun observeProfile(): Flow<DataResult<FamilyProfile?>>

    suspend fun getProfile(): DataResult<FamilyProfile?>

    /**
     * Creates the profile if none exists, or replaces it entirely otherwise — there is no
     * partial update.
     */
    suspend fun saveProfile(
        profile: FamilyProfile,
    ): DataResult<Unit>

    /**
     * Idempotent: deleting when no profile exists still succeeds. This clears only the profile
     * itself — it never clears completions or memories as a hidden side effect. A full data wipe
     * across every table a family owns is [FamilyDataCleaner.deleteAllFamilyData] instead — a
     * deliberately separate, explicit contract, never hidden inside this method.
     */
    suspend fun deleteProfile(): DataResult<Unit>
}

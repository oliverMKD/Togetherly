package com.togetherly.domain.family.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.family.FamilyProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeFamilyRepository : FamilyRepository {

    private val profileFlow = MutableStateFlow<DataResult<FamilyProfile?>>(DataResult.Success(null))

    private var nextError: AppError? = null

    val savedProfiles: MutableList<FamilyProfile> = mutableListOf()
    var deleteCallCount: Int = 0
        private set

    fun setNextError(error: AppError) {
        nextError = error
    }

    /**
     * Pushes a [DataResult.Error] straight through [observeProfile]'s `Flow` — unlike
     * [setNextError] (consumed by the one-shot methods only), this mirrors what
     * `catchStorageReadErrors` does in the real `RoomFamilyRepository`: the observing flow itself
     * emits an error value rather than one of the suspend methods returning one.
     */
    fun emitObserveError(error: AppError) {
        profileFlow.value = DataResult.Error(error)
    }

    private fun consumeError(): AppError? {
        val error = nextError
        nextError = null
        return error
    }

    override fun observeProfile(): Flow<DataResult<FamilyProfile?>> = profileFlow

    override suspend fun getProfile(): DataResult<FamilyProfile?> {
        consumeError()?.let { return DataResult.Error(it) }
        return profileFlow.value
    }

    override suspend fun saveProfile(profile: FamilyProfile): DataResult<Unit> {
        consumeError()?.let { return DataResult.Error(it) }
        savedProfiles += profile
        profileFlow.value = DataResult.Success(profile)
        return DataResult.Success(Unit)
    }

    override suspend fun deleteProfile(): DataResult<Unit> {
        consumeError()?.let { return DataResult.Error(it) }
        deleteCallCount++
        profileFlow.value = DataResult.Success(null)
        return DataResult.Success(Unit)
    }
}

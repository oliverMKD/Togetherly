package com.togetherly.domain.family.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.family.FamilySettings
import com.togetherly.domain.family.MemoryPreferences
import com.togetherly.domain.family.PrivacyPreferences
import com.togetherly.domain.family.QuestPreferences
import com.togetherly.domain.family.ReminderPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeFamilySettingsRepository : FamilySettingsRepository {

    private val settingsFlow = MutableStateFlow<DataResult<FamilySettings?>>(DataResult.Success(null))

    private var nextError: AppError? = null

    fun setNextError(error: AppError) {
        nextError = error
    }

    fun setSettings(settings: FamilySettings?) {
        settingsFlow.value = DataResult.Success(settings)
    }

    fun emitObserveError(error: AppError) {
        settingsFlow.value = DataResult.Error(error)
    }

    private fun consumeError(): AppError? {
        val error = nextError
        nextError = null
        return error
    }

    private fun currentSettings(): FamilySettings? = (settingsFlow.value as? DataResult.Success)?.value

    override fun observeSettings(): Flow<DataResult<FamilySettings?>> = settingsFlow

    override suspend fun updateQuestPreferences(preferences: QuestPreferences): DataResult<Unit> {
        consumeError()?.let { return DataResult.Error(it) }
        val current = currentSettings() ?: return DataResult.Error(AppError.Validation(ValidationError.MISSING_FAMILY_PROFILE))
        settingsFlow.value = DataResult.Success(current.copy(questPreferences = preferences))
        return DataResult.Success(Unit)
    }

    override suspend fun updateReminderPreference(preference: ReminderPreference?): DataResult<Unit> {
        consumeError()?.let { return DataResult.Error(it) }
        val current = currentSettings() ?: return DataResult.Error(AppError.Validation(ValidationError.MISSING_FAMILY_PROFILE))
        settingsFlow.value = DataResult.Success(current.copy(reminderPreference = preference))
        return DataResult.Success(Unit)
    }

    override suspend fun updateMemoryPreferences(preferences: MemoryPreferences): DataResult<Unit> {
        consumeError()?.let { return DataResult.Error(it) }
        val current = currentSettings() ?: return DataResult.Error(AppError.Validation(ValidationError.MISSING_FAMILY_PROFILE))
        settingsFlow.value = DataResult.Success(current.copy(memoryPreferences = preferences))
        return DataResult.Success(Unit)
    }

    override suspend fun updatePrivacyPreferences(preferences: PrivacyPreferences): DataResult<Unit> {
        consumeError()?.let { return DataResult.Error(it) }
        val current = currentSettings() ?: return DataResult.Error(AppError.Validation(ValidationError.MISSING_FAMILY_PROFILE))
        settingsFlow.value = DataResult.Success(current.copy(privacyPreferences = preferences))
        return DataResult.Success(Unit)
    }
}

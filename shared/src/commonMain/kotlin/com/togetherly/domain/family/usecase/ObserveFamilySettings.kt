package com.togetherly.domain.family.usecase

import com.togetherly.core.result.DataResult
import com.togetherly.domain.family.FamilySettings
import com.togetherly.domain.family.repository.FamilySettingsRepository
import kotlinx.coroutines.flow.Flow

/** The one stable call site presentation code depends on for [FamilySettings] — never a direct [FamilySettingsRepository] call from presentation code. */
class ObserveFamilySettings(
    private val familySettingsRepository: FamilySettingsRepository,
) {
    operator fun invoke(): Flow<DataResult<FamilySettings?>> = familySettingsRepository.observeSettings()
}

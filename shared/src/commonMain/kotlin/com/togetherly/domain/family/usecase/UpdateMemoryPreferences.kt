package com.togetherly.domain.family.usecase

import com.togetherly.core.result.DataResult
import com.togetherly.domain.family.MemoryPreferences
import com.togetherly.domain.family.repository.FamilySettingsRepository

class UpdateMemoryPreferences(
    private val familySettingsRepository: FamilySettingsRepository,
) {
    suspend operator fun invoke(preferences: MemoryPreferences): DataResult<Unit> =
        familySettingsRepository.updateMemoryPreferences(preferences)
}

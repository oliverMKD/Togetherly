package com.togetherly.domain.family.usecase

import com.togetherly.core.result.DataResult
import com.togetherly.domain.family.PrivacyPreferences
import com.togetherly.domain.family.repository.FamilySettingsRepository

class UpdatePrivacyPreferences(
    private val familySettingsRepository: FamilySettingsRepository,
) {
    suspend operator fun invoke(preferences: PrivacyPreferences): DataResult<Unit> =
        familySettingsRepository.updatePrivacyPreferences(preferences)
}

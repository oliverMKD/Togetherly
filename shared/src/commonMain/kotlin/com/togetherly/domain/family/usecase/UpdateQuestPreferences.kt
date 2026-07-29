package com.togetherly.domain.family.usecase

import com.togetherly.core.result.DataResult
import com.togetherly.domain.family.QuestPreferences
import com.togetherly.domain.family.repository.FamilySettingsRepository

class UpdateQuestPreferences(
    private val familySettingsRepository: FamilySettingsRepository,
) {
    suspend operator fun invoke(preferences: QuestPreferences): DataResult<Unit> =
        familySettingsRepository.updateQuestPreferences(preferences)
}

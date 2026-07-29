package com.togetherly.domain.family.usecase

import com.togetherly.core.result.DataResult
import com.togetherly.domain.family.ReminderPreference
import com.togetherly.domain.family.repository.FamilySettingsRepository

/** `null` turns reminders off entirely — matches [ReminderPreference]'s own nullable-is-disabled shape, never a separate `enabled` flag. */
class UpdateReminderPreference(
    private val familySettingsRepository: FamilySettingsRepository,
) {
    suspend operator fun invoke(preference: ReminderPreference?): DataResult<Unit> =
        familySettingsRepository.updateReminderPreference(preference)
}

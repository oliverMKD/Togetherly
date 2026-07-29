package com.togetherly.feature.reminder.presentation

import com.togetherly.core.ui.UiText
import com.togetherly.feature.reminder.model.ReminderField
import com.togetherly.feature.reminder.model.ReminderUiState
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.onboarding_error_reminder_days_required
import togetherly.shared.generated.resources.onboarding_error_reminder_time_required

/** A disabled reminder ignores whatever stale day/time selection is still sitting in state — same rule [com.togetherly.feature.onboarding.validation.OnboardingValidator] already uses. Reuses onboarding's own error copy rather than duplicating it. */
object ReminderValidator {

    fun validate(state: ReminderUiState): PersistentMap<ReminderField, UiText> {
        if (!state.enabled) return persistentMapOf()
        return errorsOf(
            ReminderField.DAYS to daysError(state),
            ReminderField.TIME to timeError(state),
        )
    }

    private fun errorsOf(vararg entries: Pair<ReminderField, UiText?>): PersistentMap<ReminderField, UiText> =
        entries.mapNotNull { (field, error) -> error?.let { field to it } }.toMap().toPersistentMap()

    private fun daysError(state: ReminderUiState): UiText? =
        if (state.days.isEmpty()) UiText.Resource(Res.string.onboarding_error_reminder_days_required) else null

    private fun timeError(state: ReminderUiState): UiText? =
        if (state.time == null) UiText.Resource(Res.string.onboarding_error_reminder_time_required) else null
}

package com.togetherly.feature.family.presentation

import com.togetherly.core.ui.UiText
import com.togetherly.feature.family.model.QuestPreferencesField
import com.togetherly.feature.family.model.QuestPreferencesUiState
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.onboarding_error_durations_required
import togetherly.shared.generated.resources.quest_preferences_error_energy_required

/**
 * Same shape as [FamilyProfileValidator] — durations/energy must stay non-empty so a "prefer
 * nothing" state can never make every recommendation impossible (see
 * [com.togetherly.domain.recommendation.DeterministicQuestRecommendationPolicy]'s own KDoc: these
 * are ranking bonuses, never hard filters, but an empty set still means the bonus can never apply
 * to anything, which isn't a useful preference to save). Location/preparation are single-choice —
 * always valid, nothing to validate.
 */
object QuestPreferencesValidator {

    fun validate(state: QuestPreferencesUiState): PersistentMap<QuestPreferencesField, UiText> = errorsOf(
        QuestPreferencesField.DURATIONS to durationsError(state),
        QuestPreferencesField.ENERGY to energyError(state),
    )

    private fun errorsOf(vararg entries: Pair<QuestPreferencesField, UiText?>): PersistentMap<QuestPreferencesField, UiText> =
        entries.mapNotNull { (field, error) -> error?.let { field to it } }.toMap().toPersistentMap()

    private fun durationsError(state: QuestPreferencesUiState): UiText? =
        if (state.selectedDurations.isEmpty()) UiText.Resource(Res.string.onboarding_error_durations_required) else null

    private fun energyError(state: QuestPreferencesUiState): UiText? =
        if (state.selectedEnergyLevels.isEmpty()) UiText.Resource(Res.string.quest_preferences_error_energy_required) else null
}

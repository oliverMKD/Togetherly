package com.togetherly.feature.today.model

import androidx.compose.runtime.Immutable
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.PreparationLevel
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestLocation

/**
 * Draft-only: mutating this never touches the persisted daily quest by itself. Only
 * [com.togetherly.feature.today.presentation.TodayAction.ApplyFiltersClicked] turns this into a
 * [com.togetherly.domain.daily.QuestContext] and calls
 * [com.togetherly.domain.daily.usecase.SelectDailyQuestForContext] — see
 * [com.togetherly.feature.today.presentation.TodayUiState]'s own KDoc for why every other filter
 * action only ever replaces this field.
 */
@Immutable
data class TodayFilterState(
    val duration: DurationBand? = null,
    val location: QuestLocation? = null,
    val energy: EnergyLevel? = null,
    val preparation: PreparationLevel? = null,
    val category: QuestCategory? = null,
) {
    val activeCount: Int
        get() = listOfNotNull(duration, location, energy, preparation, category).size
}

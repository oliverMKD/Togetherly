package com.togetherly.domain.daily

import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.PreparationLevel
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestLocation

/**
 * Every field is nullable — an absent field means "use the family's normal preference," not an
 * unset error state. An entirely empty context is valid and simply defers to those preferences.
 */
data class QuestContext(
    val duration: DurationBand?,
    val location: QuestLocation?,
    val energy: EnergyLevel?,
    val preparation: PreparationLevel?,
    val preferredCategory: QuestCategory?,
)

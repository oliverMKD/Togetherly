package com.togetherly.domain.explore

import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestLocation

/**
 * Every field here reuses an existing domain value object — [QuestCategory], [DurationBand]
 * (bucketed against [FamilyQuest.durationMinutes][com.togetherly.domain.quest.FamilyQuest.durationMinutes]
 * via [com.togetherly.domain.recommendation.durationBandFor], the same bucketing
 * [com.togetherly.domain.recommendation.DeterministicQuestRecommendationPolicy] already uses),
 * [EnergyLevel], [QuestLocation], and [AgeBand] — none of them are duplicated here. [QuestAccessFilter]
 * is the one genuinely new type, since nothing in the domain models an "either" access state (see
 * its own KDoc). `null` on any reused field means "don't filter on this dimension" — there is no
 * separate `ExploreCategory.All`/wrapper enum shadowing [QuestCategory]'s own values.
 */
data class ExploreFilters(
    val category: QuestCategory? = null,
    val duration: DurationBand? = null,
    val energy: EnergyLevel? = null,
    val location: QuestLocation? = null,
    val ageBand: AgeBand? = null,
    val access: QuestAccessFilter = QuestAccessFilter.ALL,
) {
    val isEmpty: Boolean
        get() = category == null && duration == null && energy == null &&
            location == null && ageBand == null && access == QuestAccessFilter.ALL

    /** How many of the six dimensions above are actively narrowing results — what a "Filters (N)" badge counts. */
    val activeCount: Int
        get() = listOfNotNull(category, duration, energy, location, ageBand).size +
            if (access != QuestAccessFilter.ALL) 1 else 0
}

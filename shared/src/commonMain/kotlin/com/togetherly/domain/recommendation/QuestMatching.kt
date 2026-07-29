package com.togetherly.domain.recommendation

import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.PreparationLevel
import com.togetherly.domain.quest.QuestLocation

/**
 * The band a quest's raw [FamilyQuest.durationMinutes] falls into. This project defines no other
 * mapping anywhere else, so this is it — [DeterministicQuestRecommendationPolicy] is the only
 * caller today, but any future duration-band display logic should reuse this rather than
 * redefining the ranges:
 *
 * - [DurationBand.FIVE_MINUTES] — 1 to 5 minutes
 * - [DurationBand.TEN_MINUTES] — 6 to 10 minutes
 * - [DurationBand.TWENTY_MINUTES] — 11 to 20 minutes
 * - [DurationBand.THIRTY_PLUS_MINUTES] — 21 minutes or more
 */
internal fun durationBandFor(minutes: Int): DurationBand = when {
    minutes <= 5 -> DurationBand.FIVE_MINUTES
    minutes <= 10 -> DurationBand.TEN_MINUTES
    minutes <= 20 -> DurationBand.TWENTY_MINUTES
    else -> DurationBand.THIRTY_PLUS_MINUTES
}

/**
 * Scoring-only (never a hard filter) — see [DeterministicQuestRecommendationPolicy]'s own KDoc for
 * why location, unlike preparation, never excludes a candidate from the family's *normal*
 * preference alone. [LocationPreference.BOTH] matches everything; otherwise this mirrors
 * [com.togetherly.domain.quest.matches] (quest-side [QuestLocation.EITHER] always matches).
 */
internal fun FamilyQuest.matchesLocationPreference(preference: LocationPreference): Boolean = when (preference) {
    LocationPreference.BOTH -> true
    LocationPreference.INDOOR -> location == QuestLocation.INDOOR || location == QuestLocation.EITHER
    LocationPreference.OUTDOOR -> location == QuestLocation.OUTDOOR || location == QuestLocation.EITHER
}

/**
 * Maps the family's normal preparation preference onto the same [PreparationLevel] scale a quest
 * is rated on, so both the explicit-context ceiling and the normal-preference ceiling can be
 * compared with one ordinal check — see [DeterministicQuestRecommendationPolicy]'s own KDoc for
 * why preparation (unlike location or duration) is *always* a hard maximum, explicit or not.
 * [PreparationPreference.ANY] maps to [PreparationLevel.ADVANCED] — the highest quest-side tier —
 * meaning "no practical ceiling," not literally "anything without limit."
 */
internal fun PreparationPreference.asPreparationCeiling(): PreparationLevel = when (this) {
    PreparationPreference.NONE -> PreparationLevel.NONE
    PreparationPreference.SIMPLE_MATERIALS -> PreparationLevel.SIMPLE_MATERIALS
    PreparationPreference.ANY -> PreparationLevel.ADVANCED
}

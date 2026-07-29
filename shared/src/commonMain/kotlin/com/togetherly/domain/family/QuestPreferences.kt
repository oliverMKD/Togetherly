package com.togetherly.domain.family

import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.QuestCategory

/**
 * A cohesive projection of the quest-matching fields already on [FamilyProfile] — [interests],
 * [preferredDurations], [locationPreference], [preparationPreference], and (Step 13.3)
 * [preferredEnergyLevels] are not duplicated storage, they're the exact same values [FamilyProfile]
 * already carries, grouped here purely so a settings screen can read/update them as one unit
 * without touching [FamilyProfile.childAgeBands] or [FamilyProfile.displayName] at the same time.
 *
 * [preferredEnergyLevels] may be empty ("no energy preference set") — see [FamilyProfile]'s own
 * KDoc. [defaults] is the maximally-permissive starting point (every duration/energy level, the
 * least restrictive location/preparation) — used both for a brand-new profile and for "reset to
 * defaults" in the quest preferences screen, so resetting can never make every recommendation
 * impossible.
 */
data class QuestPreferences(
    val interests: Set<QuestCategory>,
    val preferredDurations: Set<DurationBand>,
    val locationPreference: LocationPreference,
    val preparationPreference: PreparationPreference,
    val preferredEnergyLevels: Set<EnergyLevel> = emptySet(),
) {
    companion object {
        fun defaults(interests: Set<QuestCategory>): QuestPreferences = QuestPreferences(
            interests = interests,
            preferredDurations = DurationBand.entries.toSet(),
            locationPreference = LocationPreference.BOTH,
            preparationPreference = PreparationPreference.ANY,
            preferredEnergyLevels = EnergyLevel.entries.toSet(),
        )
    }
}

fun FamilyProfile.toQuestPreferences(): QuestPreferences = QuestPreferences(
    interests = interests,
    preferredDurations = preferredDurations,
    locationPreference = locationPreference,
    preparationPreference = preparationPreference,
    preferredEnergyLevels = preferredEnergyLevels,
)

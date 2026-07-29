package com.togetherly.domain.family

import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.validation.DomainValidationException
import com.togetherly.domain.validation.DomainValidationReason
import com.togetherly.domain.validation.requireNotEmpty
import kotlin.time.Instant

/**
 * [preferredEnergyLevels] (Step 13.3) is a *ranking* signal only — unlike [childAgeBands]/
 * [interests]/[preferredDurations], it is allowed to be empty (meaning "no energy preference set
 * yet," not "impossible to match"), so there is no `requireNotEmpty` for it here. See
 * [com.togetherly.domain.recommendation.DeterministicQuestRecommendationPolicy] for how it's used —
 * it never hard-filters a quest out, only adds a scoring bonus, matching this project's rule that
 * preferences influence future suggestions without permanently hiding content from Explore.
 */
data class FamilyProfile(
    val id: FamilyId,
    val displayName: FamilyDisplayName?,
    val childAgeBands: Set<AgeBand>,
    val interests: Set<QuestCategory>,
    val preferredDurations: Set<DurationBand>,
    val locationPreference: LocationPreference,
    val preparationPreference: PreparationPreference,
    val preferredEnergyLevels: Set<EnergyLevel> = emptySet(),
    val reminderPreference: ReminderPreference?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        requireNotEmpty(childAgeBands)
        requireNotEmpty(interests)
        requireNotEmpty(preferredDurations)
        if (updatedAt < createdAt) {
            throw DomainValidationException(DomainValidationReason.INVALID_ORDER)
        }
    }
}

fun FamilyProfile.updatePreferences(
    interests: Set<QuestCategory>,
    preferredDurations: Set<DurationBand>,
    locationPreference: LocationPreference,
    preparationPreference: PreparationPreference,
    updatedAt: Instant,
): FamilyProfile = copy(
    interests = interests,
    preferredDurations = preferredDurations,
    locationPreference = locationPreference,
    preparationPreference = preparationPreference,
    updatedAt = updatedAt,
)

package com.togetherly.domain.family

import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.QuestCategory

internal fun testQuestPreferences(
    interests: Set<QuestCategory> = setOf(QuestCategory.TALK, QuestCategory.MOVE),
    preferredDurations: Set<DurationBand> = setOf(DurationBand.TEN_MINUTES),
    locationPreference: LocationPreference = LocationPreference.BOTH,
    preparationPreference: PreparationPreference = PreparationPreference.ANY,
    preferredEnergyLevels: Set<EnergyLevel> = setOf(EnergyLevel.MODERATE),
) = QuestPreferences(
    interests = interests,
    preferredDurations = preferredDurations,
    locationPreference = locationPreference,
    preparationPreference = preparationPreference,
    preferredEnergyLevels = preferredEnergyLevels,
)

internal fun testFamilySettings(
    profile: FamilyProfile,
    questPreferences: QuestPreferences = profile.toQuestPreferences(),
    reminderPreference: ReminderPreference? = profile.reminderPreference,
    memoryPreferences: MemoryPreferences = MemoryPreferences.defaults(),
    privacyPreferences: PrivacyPreferences = PrivacyPreferences.defaults(),
) = FamilySettings(
    profile = profile,
    questPreferences = questPreferences,
    reminderPreference = reminderPreference,
    memoryPreferences = memoryPreferences,
    privacyPreferences = privacyPreferences,
)

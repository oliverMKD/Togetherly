package com.togetherly.integration

import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.family.FamilyProfile
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.QuestCategory
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.time.Instant

/**
 * These use-case integration tests (Step 5.6) run the real content pipeline against fake local
 * repositories for everything else — family profile, daily quest, saved quests, completions —
 * since none of those have a production implementation yet.
 */
internal val FIXED_NOW: Instant = Instant.parse("2026-07-24T09:00:00Z")

internal fun testFamilyProfile(
    id: FamilyId = FamilyId("family-1"),
    childAgeBands: Set<AgeBand> = setOf(AgeBand.AGE_6_TO_8, AgeBand.AGE_9_TO_11, AgeBand.AGE_12_TO_13),
    interests: Set<QuestCategory> = QuestCategory.entries.toSet(),
    preferredDurations: Set<DurationBand> = DurationBand.entries.toSet(),
    locationPreference: LocationPreference = LocationPreference.BOTH,
    preparationPreference: PreparationPreference = PreparationPreference.ANY,
    preferredEnergyLevels: Set<EnergyLevel> = EnergyLevel.entries.toSet(),
) = FamilyProfile(
    id = id,
    displayName = null,
    childAgeBands = childAgeBands,
    interests = interests,
    preferredDurations = preferredDurations,
    locationPreference = locationPreference,
    preparationPreference = preparationPreference,
    preferredEnergyLevels = preferredEnergyLevels,
    reminderPreference = null,
    createdAt = FIXED_NOW,
    updatedAt = FIXED_NOW,
)

/** Unconfined so the repository's background first-load completes synchronously in tests. */
internal fun testDispatchers(): AppDispatchers = TestAppDispatchers(UnconfinedTestDispatcher())

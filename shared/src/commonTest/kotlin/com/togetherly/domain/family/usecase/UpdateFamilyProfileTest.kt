package com.togetherly.domain.family.usecase

import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.family.FamilyProfile
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.family.repository.FakeFamilyRepository
import com.togetherly.domain.quest.QuestCategory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val CREATED_AT = Instant.parse("2026-06-01T08:00:00Z")
private val UPDATED_AT = Instant.parse("2026-06-15T08:00:00Z")

private fun existingProfile() = FamilyProfile(
    id = FamilyId("family-1"),
    displayName = null,
    childAgeBands = setOf(AgeBand.AGE_6_TO_8),
    interests = setOf(QuestCategory.CREATE),
    preferredDurations = setOf(DurationBand.TEN_MINUTES),
    locationPreference = LocationPreference.BOTH,
    preparationPreference = PreparationPreference.SIMPLE_MATERIALS,
    reminderPreference = null,
    createdAt = CREATED_AT,
    updatedAt = CREATED_AT,
)

class UpdateFamilyProfileTest {

    @Test
    fun updatePreservesIdentityAndCreationTime() = runTest {
        val repository = FakeFamilyRepository()
        val original = existingProfile()
        repository.saveProfile(original)
        val useCase = UpdateFamilyProfile(repository, TestAppClock(UPDATED_AT))

        val result = useCase(
            UpdateFamilyProfileCommand(
                displayName = null,
                childAgeBands = original.childAgeBands,
                interests = setOf(QuestCategory.MOVE),
                preferredDurations = original.preferredDurations,
                locationPreference = LocationPreference.OUTDOOR,
                preparationPreference = original.preparationPreference,
                preferredEnergyLevels = original.preferredEnergyLevels,
                reminderPreference = null,
            ),
        )

        val updated = (result as DataResult.Success).value
        assertEquals(original.id, updated.id)
        assertEquals(original.createdAt, updated.createdAt)
        assertEquals(UPDATED_AT, updated.updatedAt)
        assertEquals(setOf(QuestCategory.MOVE), updated.interests)
    }

    @Test
    fun missingProfileReturnsTypedError() = runTest {
        val repository = FakeFamilyRepository()
        val useCase = UpdateFamilyProfile(repository, TestAppClock(UPDATED_AT))

        val result = useCase(
            UpdateFamilyProfileCommand(
                displayName = null,
                childAgeBands = setOf(AgeBand.AGE_6_TO_8),
                interests = setOf(QuestCategory.CREATE),
                preferredDurations = setOf(DurationBand.TEN_MINUTES),
                locationPreference = LocationPreference.BOTH,
                preparationPreference = PreparationPreference.SIMPLE_MATERIALS,
                preferredEnergyLevels = emptySet(),
                reminderPreference = null,
            ),
        )

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.MISSING_FAMILY_PROFILE)), result)
    }
}

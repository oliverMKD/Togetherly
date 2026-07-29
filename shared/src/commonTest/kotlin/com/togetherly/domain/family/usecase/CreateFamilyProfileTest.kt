package com.togetherly.domain.family.usecase

import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.id.SequentialIdGenerator
import com.togetherly.core.result.DataResult
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.family.repository.FakeFamilyRepository
import com.togetherly.domain.quest.QuestCategory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")

private fun command() = CreateFamilyProfileCommand(
    displayName = null,
    childAgeBands = setOf(AgeBand.AGE_6_TO_8),
    interests = setOf(QuestCategory.CREATE),
    preferredDurations = setOf(DurationBand.TEN_MINUTES),
    locationPreference = LocationPreference.BOTH,
    preparationPreference = PreparationPreference.SIMPLE_MATERIALS,
    reminderPreference = null,
)

class CreateFamilyProfileTest {

    @Test
    fun createsFamilyWithDeterministicIdAndTimestamps() = runTest {
        val repository = FakeFamilyRepository()
        val useCase = CreateFamilyProfile(
            familyRepository = repository,
            clock = TestAppClock(NOW),
            idGenerator = SequentialIdGenerator(prefix = "family"),
        )

        val result = useCase(command())

        val profile = (result as DataResult.Success).value
        assertEquals(FamilyId("family-0"), profile.id)
        assertEquals(NOW, profile.createdAt)
        assertEquals(NOW, profile.updatedAt)
        assertEquals(listOf(profile), repository.savedProfiles)
    }

    @Test
    fun repositoryErrorIsPropagated() = runTest {
        val repository = FakeFamilyRepository()
        val error = AppError.Storage(StorageError.WRITE_FAILED)
        repository.setNextError(error)
        val useCase = CreateFamilyProfile(
            familyRepository = repository,
            clock = TestAppClock(NOW),
            idGenerator = SequentialIdGenerator(),
        )

        val result = useCase(command())

        assertEquals(DataResult.Error(error), result)
    }
}

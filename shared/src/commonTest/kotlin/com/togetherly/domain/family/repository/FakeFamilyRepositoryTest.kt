package com.togetherly.domain.family.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.family.FamilyProfile
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.quest.QuestCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val CREATED_AT = Instant.parse("2026-01-01T00:00:00Z")

private fun testProfile(id: String = "family-1") = FamilyProfile(
    id = FamilyId(id),
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

class FakeFamilyRepositoryTest {

    @Test
    fun initialProfileIsNull() = runTest {
        val repository = FakeFamilyRepository()

        assertEquals(DataResult.Success(null), repository.getProfile())
        assertEquals(DataResult.Success(null), repository.observeProfile().first())
    }

    @Test
    fun savingEmitsTheNewProfile() = runTest {
        val repository = FakeFamilyRepository()
        val profile = testProfile()

        repository.saveProfile(profile)

        assertEquals(DataResult.Success(profile), repository.observeProfile().first())
        assertEquals(listOf(profile), repository.savedProfiles)
    }

    @Test
    fun replacingEmitsTheUpdatedProfile() = runTest {
        val repository = FakeFamilyRepository()
        val original = testProfile()
        val updated = original.copy(locationPreference = LocationPreference.OUTDOOR)

        repository.saveProfile(original)
        repository.saveProfile(updated)

        assertEquals(DataResult.Success(updated), repository.observeProfile().first())
    }

    @Test
    fun deletingEmitsNull() = runTest {
        val repository = FakeFamilyRepository()
        repository.saveProfile(testProfile())

        repository.deleteProfile()

        assertEquals(DataResult.Success(null), repository.observeProfile().first())
    }

    @Test
    fun deleteIsIdempotent() = runTest {
        val repository = FakeFamilyRepository()

        val first = repository.deleteProfile()
        val second = repository.deleteProfile()

        assertEquals(DataResult.Success(Unit), first)
        assertEquals(DataResult.Success(Unit), second)
        assertEquals(2, repository.deleteCallCount)
    }

    @Test
    fun configuredErrorIsReturned() = runTest {
        val repository = FakeFamilyRepository()
        val error = AppError.Storage(StorageError.WRITE_FAILED)
        repository.setNextError(error)

        val result = repository.saveProfile(testProfile())

        assertEquals(DataResult.Error(error), result)
    }
}

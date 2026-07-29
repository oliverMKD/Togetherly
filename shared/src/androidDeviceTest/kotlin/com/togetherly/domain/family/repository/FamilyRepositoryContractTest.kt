package com.togetherly.domain.family.repository

import com.togetherly.core.result.DataResult
import com.togetherly.data.testFamilyProfile
import com.togetherly.domain.family.FamilyDisplayName
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * A shared behavioral contract every [FamilyRepository] implementation must satisfy, run against
 * both [FakeFamilyRepository] and [com.togetherly.data.family.RoomFamilyRepository] by their
 * respective concrete subclasses.
 */
internal abstract class FamilyRepositoryContractTest {

    abstract fun repository(): FamilyRepository

    @Test
    fun missingProfileReturnsSuccessNull() = runTest {
        assertEquals(DataResult.Success(null), repository().getProfile())
    }

    @Test
    fun savedProfileIsReturnedByGetProfile() = runTest {
        val repository = repository()
        val profile = testFamilyProfile()

        repository.saveProfile(profile)

        assertEquals(DataResult.Success(profile), repository.getProfile())
    }

    @Test
    fun savingAgainReplacesThePreviousProfile() = runTest {
        val repository = repository()
        repository.saveProfile(testFamilyProfile(displayName = FamilyDisplayName("Old Name")))

        val replacement = testFamilyProfile(displayName = FamilyDisplayName("New Name"))
        repository.saveProfile(replacement)

        assertEquals(DataResult.Success(replacement), repository.getProfile())
    }

    @Test
    fun observeProfileEmitsTheCurrentState() = runTest {
        val repository = repository()
        val profile = testFamilyProfile()

        repository.saveProfile(profile)

        assertEquals(DataResult.Success(profile), repository.observeProfile().first())
    }

    @Test
    fun deleteProfileIsIdempotent() = runTest {
        val repository = repository()

        assertEquals(DataResult.Success(Unit), repository.deleteProfile())
        assertEquals(DataResult.Success(Unit), repository.deleteProfile())
        assertEquals(DataResult.Success(null), repository.getProfile())
    }

    @Test
    fun deleteProfileRemovesASavedProfile() = runTest {
        val repository = repository()
        repository.saveProfile(testFamilyProfile())

        repository.deleteProfile()

        assertEquals(DataResult.Success(null), repository.getProfile())
    }
}

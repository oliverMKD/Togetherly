package com.togetherly.data.local.keys

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals

class FamilyStorageKeysTest {

    @Test
    fun everyAgeBandRoundTripsThroughItsStorageKey() {
        AgeBand.entries.forEach { band ->
            assertEquals(DataResult.Success(band), band.toStorageKey().toAgeBand())
        }
    }

    @Test
    fun unknownAgeBandKeyIsCorruptedStorage() {
        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), "not-a-band".toAgeBand())
    }

    @Test
    fun everyDurationBandRoundTripsThroughItsStorageKey() {
        DurationBand.entries.forEach { duration ->
            assertEquals(DataResult.Success(duration), duration.toStorageKey().toDurationBand())
        }
    }

    @Test
    fun unknownDurationBandKeyIsCorruptedStorage() {
        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), "not-a-duration".toDurationBand())
    }

    @Test
    fun everyLocationPreferenceRoundTripsThroughItsStorageKey() {
        LocationPreference.entries.forEach { preference ->
            assertEquals(DataResult.Success(preference), preference.toStorageKey().toLocationPreference())
        }
    }

    @Test
    fun unknownLocationPreferenceKeyIsCorruptedStorage() {
        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), "not-a-location".toLocationPreference())
    }

    @Test
    fun everyPreparationPreferenceRoundTripsThroughItsStorageKey() {
        PreparationPreference.entries.forEach { preference ->
            assertEquals(DataResult.Success(preference), preference.toStorageKey().toPreparationPreference())
        }
    }

    @Test
    fun unknownPreparationPreferenceKeyIsCorruptedStorage() {
        assertEquals(
            DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)),
            "not-a-preparation".toPreparationPreference(),
        )
    }

    @Test
    fun everyDayOfWeekRoundTripsThroughItsStorageKey() {
        DayOfWeek.entries.forEach { day ->
            assertEquals(DataResult.Success(day), day.toStorageKey().toDayOfWeek())
        }
    }

    @Test
    fun unknownDayOfWeekKeyIsCorruptedStorage() {
        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), "not-a-day".toDayOfWeek())
    }
}

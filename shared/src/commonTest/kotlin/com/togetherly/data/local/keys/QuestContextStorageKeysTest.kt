package com.togetherly.data.local.keys

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.PreparationLevel
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestLocation
import kotlin.test.Test
import kotlin.test.assertEquals

class QuestContextStorageKeysTest {

    @Test
    fun everyQuestCategoryRoundTripsThroughItsStorageKey() {
        QuestCategory.entries.forEach { category ->
            assertEquals(DataResult.Success(category), category.toStorageKey().toQuestCategory())
        }
    }

    @Test
    fun unknownQuestCategoryKeyIsCorruptedStorage() {
        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), "not-a-category".toQuestCategory())
    }

    @Test
    fun everyQuestLocationRoundTripsThroughItsStorageKey() {
        QuestLocation.entries.forEach { location ->
            assertEquals(DataResult.Success(location), location.toStorageKey().toQuestLocation())
        }
    }

    @Test
    fun unknownQuestLocationKeyIsCorruptedStorage() {
        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), "not-a-location".toQuestLocation())
    }

    @Test
    fun everyEnergyLevelRoundTripsThroughItsStorageKey() {
        EnergyLevel.entries.forEach { energy ->
            assertEquals(DataResult.Success(energy), energy.toStorageKey().toEnergyLevel())
        }
    }

    @Test
    fun unknownEnergyLevelKeyIsCorruptedStorage() {
        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), "not-an-energy".toEnergyLevel())
    }

    @Test
    fun everyPreparationLevelRoundTripsThroughItsStorageKey() {
        PreparationLevel.entries.forEach { preparation ->
            assertEquals(DataResult.Success(preparation), preparation.toStorageKey().toPreparationLevel())
        }
    }

    @Test
    fun unknownPreparationLevelKeyIsCorruptedStorage() {
        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), "not-a-preparation".toPreparationLevel())
    }
}

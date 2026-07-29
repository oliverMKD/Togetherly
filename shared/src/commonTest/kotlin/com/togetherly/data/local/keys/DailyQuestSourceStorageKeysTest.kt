package com.togetherly.data.local.keys

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.daily.DailyQuestSource
import kotlin.test.Test
import kotlin.test.assertEquals

class DailyQuestSourceStorageKeysTest {

    @Test
    fun everySourceRoundTripsThroughItsStorageKey() {
        DailyQuestSource.entries.forEach { source ->
            assertEquals(DataResult.Success(source), source.toStorageKey().toDailyQuestSource())
        }
    }

    @Test
    fun unknownSourceKeyIsCorruptedStorage() {
        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), "not-a-source".toDailyQuestSource())
    }
}

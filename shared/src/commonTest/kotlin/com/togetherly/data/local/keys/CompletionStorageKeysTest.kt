package com.togetherly.data.local.keys

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.FamilyReaction
import kotlin.test.Test
import kotlin.test.assertEquals

class CompletionStorageKeysTest {

    @Test
    fun everyReactionRoundTripsThroughItsStorageKey() {
        FamilyReaction.entries.forEach { reaction ->
            assertEquals(DataResult.Success(reaction), reaction.toStorageKey().toFamilyReaction())
        }
    }

    @Test
    fun unknownReactionKeyIsCorruptedStorage() {
        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), "not-a-reaction".toFamilyReaction())
    }
}

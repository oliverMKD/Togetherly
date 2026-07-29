package com.togetherly.data.completion

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.data.local.completion.CompletionDao
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.data.runCatchingStorage
import com.togetherly.domain.completion.repository.MemoryCleaner

/**
 * See [MemoryCleaner]'s own KDoc for exactly what is and is not cleared. Both writes run inside
 * one [TogetherlyDatabase.useWriterConnection]/`immediateTransaction` — the same KMP-unified
 * transaction API [RoomFamilyDataCleaner][com.togetherly.data.family.RoomFamilyDataCleaner] uses —
 * so a failure partway through never leaves notes cleared but media rows still present, or vice
 * versa.
 */
internal class RoomMemoryCleaner(
    private val completionDao: CompletionDao,
    private val database: TogetherlyDatabase,
    private val dispatchers: AppDispatchers,
    private val diagnostics: OperationalDiagnostics,
) : MemoryCleaner {

    override suspend fun clearAllMemoryContent(): DataResult<Unit> =
        dispatchers.runCatchingStorage(StorageError.DELETE_FAILED, diagnostics) {
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    completionDao.clearAllNotes()
                    completionDao.deleteAllMemoryMedia()
                }
            }
            DataResult.Success(Unit)
        }
}

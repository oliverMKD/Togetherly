package com.togetherly.data.family

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.data.local.completion.ACTIVE_QUEST_SESSION_SLOT_ID
import com.togetherly.data.local.completion.CompletionDao
import com.togetherly.data.local.daily.DailyQuestDao
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.data.local.family.FamilyDao
import com.togetherly.data.local.saved.SavedQuestDao
import com.togetherly.data.runCatchingStorage
import com.togetherly.domain.family.repository.FamilyDataCleaner

/**
 * See [FamilyDataCleaner]'s own KDoc for exactly what is and is not deleted. Every delete runs
 * inside one [TogetherlyDatabase.useWriterConnection]/[immediateTransaction] — the same KMP-unified
 * transaction API [RoomFamilyRepository] uses (`RoomDatabase.withTransaction` is Android/JVM-only)
 * — so a failure partway through never leaves some tables wiped and others untouched.
 *
 * [DatabaseMetadataEntity][com.togetherly.data.local.database.DatabaseMetadataEntity] is
 * deliberately never touched here — it is schema/migration bookkeeping, not family data (see
 * [TogetherlyDatabase]'s own KDoc). There is no catalogue table to touch at all: the bundled quest
 * catalogue is JSON-backed, never a database row (see docs/content-system.md).
 */
internal class RoomFamilyDataCleaner(
    private val familyDao: FamilyDao,
    private val dailyQuestDao: DailyQuestDao,
    private val savedQuestDao: SavedQuestDao,
    private val completionDao: CompletionDao,
    private val database: TogetherlyDatabase,
    private val dispatchers: AppDispatchers,
    private val diagnostics: OperationalDiagnostics,
) : FamilyDataCleaner {

    override suspend fun deleteAllFamilyData(): DataResult<Unit> =
        dispatchers.runCatchingStorage(StorageError.DELETE_FAILED, diagnostics) {
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    familyDao.deleteAllProfiles()
                    dailyQuestDao.deleteAllDailyQuests()
                    dailyQuestDao.deleteAllDismissals()
                    savedQuestDao.deleteAllSavedQuests()
                    completionDao.deleteActiveSession(ACTIVE_QUEST_SESSION_SLOT_ID)
                    completionDao.deleteAllCompletions()
                }
            }
            DataResult.Success(Unit)
        }
}

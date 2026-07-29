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
import com.togetherly.data.runCatchingStorage
import com.togetherly.domain.family.repository.QuestHistoryCleaner

/**
 * See [QuestHistoryCleaner]'s own KDoc for exactly what is and is not deleted — the same table set
 * [RoomFamilyDataCleaner] wipes, minus `family_profile` (and its cascading preference rows) and
 * `saved_quest`. Runs inside one [TogetherlyDatabase.useWriterConnection]/`immediateTransaction`,
 * same as [RoomFamilyDataCleaner].
 */
internal class RoomQuestHistoryCleaner(
    private val dailyQuestDao: DailyQuestDao,
    private val completionDao: CompletionDao,
    private val database: TogetherlyDatabase,
    private val dispatchers: AppDispatchers,
    private val diagnostics: OperationalDiagnostics,
) : QuestHistoryCleaner {

    override suspend fun resetQuestHistory(): DataResult<Unit> =
        dispatchers.runCatchingStorage(StorageError.DELETE_FAILED, diagnostics) {
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    dailyQuestDao.deleteAllDailyQuests()
                    dailyQuestDao.deleteAllDismissals()
                    completionDao.deleteActiveSession(ACTIVE_QUEST_SESSION_SLOT_ID)
                    completionDao.deleteAllCompletions()
                }
            }
            DataResult.Success(Unit)
        }
}

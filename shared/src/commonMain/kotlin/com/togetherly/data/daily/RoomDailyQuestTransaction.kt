package com.togetherly.data.daily

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.core.result.getOrElse
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.data.local.daily.DailyQuestDao
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.data.local.mapper.DailyQuestMapper
import com.togetherly.data.local.mapper.DismissedQuestMapper
import com.togetherly.data.runCatchingStorage
import com.togetherly.domain.daily.DailyQuest
import com.togetherly.domain.daily.DismissedQuest
import com.togetherly.domain.daily.repository.DailyQuestTransaction

/**
 * Both writes run inside [TogetherlyDatabase.useWriterConnection]/[immediateTransaction] — the
 * same KMP-unified transaction API [com.togetherly.data.completion.RoomQuestSessionTransaction]
 * uses (`RoomDatabase.withTransaction` is Android/JVM-only, unavailable on iOS/common).
 */
internal class RoomDailyQuestTransaction(
    private val dailyQuestDao: DailyQuestDao,
    private val dailyQuestMapper: DailyQuestMapper,
    private val dismissedQuestMapper: DismissedQuestMapper,
    private val database: TogetherlyDatabase,
    private val dispatchers: AppDispatchers,
    private val diagnostics: OperationalDiagnostics,
) : DailyQuestTransaction {

    override suspend fun replaceWithReroll(
        previous: DismissedQuest,
        replacement: DailyQuest,
    ): DataResult<Unit> =
        dispatchers.runCatchingStorage(StorageError.WRITE_FAILED, diagnostics) {
            val dismissalEntity = dismissedQuestMapper.toEntity(previous).getOrElse { return@runCatchingStorage DataResult.Error(it) }
            val dailyQuestEntity = dailyQuestMapper.toEntity(replacement).getOrElse { return@runCatchingStorage DataResult.Error(it) }
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    dailyQuestDao.insertDismissal(dismissalEntity)
                    dailyQuestDao.insertDailyQuest(dailyQuestEntity)
                }
            }
            DataResult.Success(Unit)
        }
}

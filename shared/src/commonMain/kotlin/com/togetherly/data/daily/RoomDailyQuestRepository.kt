package com.togetherly.data.daily

import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.core.result.getOrElse
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.data.catchStorageReadErrors
import com.togetherly.data.local.daily.DailyQuestDao
import com.togetherly.data.local.mapper.DailyQuestMapper
import com.togetherly.data.local.mapper.DismissedQuestMapper
import com.togetherly.data.runCatchingStorage
import com.togetherly.domain.daily.DailyQuest
import com.togetherly.domain.daily.DismissedQuest
import com.togetherly.domain.daily.repository.DailyQuestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Never selects or recommends a quest itself — that stays a use case's job (see this interface's
 * own KDoc). [localDate]/[dismissedAt] are always caller-supplied, this repository never touches
 * the system clock.
 */
internal class RoomDailyQuestRepository(
    private val dailyQuestDao: DailyQuestDao,
    private val dailyQuestMapper: DailyQuestMapper,
    private val dismissedQuestMapper: DismissedQuestMapper,
    private val dispatchers: AppDispatchers,
    private val diagnostics: OperationalDiagnostics,
) : DailyQuestRepository {

    override fun observeToday(localDate: LocalDate): Flow<DataResult<DailyQuest?>> =
        dailyQuestDao.observeDailyQuest(localDate.toString())
            .map { entity -> entity?.let { dailyQuestMapper.toDomain(it) } ?: DataResult.Success(null) }
            .catchStorageReadErrors(diagnostics)

    override suspend fun getToday(localDate: LocalDate): DataResult<DailyQuest?> =
        dispatchers.runCatchingStorage(StorageError.READ_FAILED, diagnostics) {
            val entity = dailyQuestDao.getDailyQuest(localDate.toString()) ?: return@runCatchingStorage DataResult.Success(null)
            dailyQuestMapper.toDomain(entity)
        }

    /** [DailyQuestEntity][com.togetherly.data.local.daily.DailyQuestEntity] is keyed by `localDate`, so this replaces whatever selection already exists for that date; other dates are untouched. */
    override suspend fun saveDailyQuest(dailyQuest: DailyQuest): DataResult<Unit> =
        dispatchers.runCatchingStorage(StorageError.WRITE_FAILED, diagnostics) {
            val entity = dailyQuestMapper.toEntity(dailyQuest).getOrElse { return@runCatchingStorage DataResult.Error(it) }
            dailyQuestDao.insertDailyQuest(entity)
            DataResult.Success(Unit)
        }

    override suspend fun recordDismissal(dismissedQuest: DismissedQuest): DataResult<Unit> =
        dispatchers.runCatchingStorage(StorageError.WRITE_FAILED, diagnostics) {
            val entity = dismissedQuestMapper.toEntity(dismissedQuest).getOrElse { return@runCatchingStorage DataResult.Error(it) }
            dailyQuestDao.insertDismissal(entity)
            DataResult.Success(Unit)
        }

    override suspend fun getRecentDismissals(since: Instant): DataResult<List<DismissedQuest>> =
        dispatchers.runCatchingStorage(StorageError.READ_FAILED, diagnostics) {
            val entities = dailyQuestDao.getDismissalsSince(since.toEpochMilliseconds())
            val dismissals = mutableListOf<DismissedQuest>()
            for (entity in entities) {
                dismissals += dismissedQuestMapper.toDomain(entity).getOrElse { return@runCatchingStorage DataResult.Error(it) }
            }
            DataResult.Success(dismissals)
        }

    /** Idempotent: deleting a date with no selection still succeeds; other dates are untouched. */
    override suspend fun clearDailyQuest(localDate: LocalDate): DataResult<Unit> =
        dispatchers.runCatchingStorage(StorageError.DELETE_FAILED, diagnostics) {
            dailyQuestDao.deleteDailyQuest(localDate.toString())
            DataResult.Success(Unit)
        }
}

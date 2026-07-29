package com.togetherly.data.completion

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.core.result.getOrElse
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.data.local.completion.ACTIVE_QUEST_SESSION_SLOT_ID
import com.togetherly.data.local.completion.CompletionDao
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.data.local.mapper.ActiveQuestSessionMapper
import com.togetherly.data.local.mapper.QuestCompletionMapper
import com.togetherly.data.runCatchingStorage
import com.togetherly.domain.completion.ActiveQuestSession
import com.togetherly.domain.completion.QuestCompletion
import com.togetherly.domain.completion.repository.QuestSessionTransaction

/**
 * Both operations run inside [TogetherlyDatabase.useWriterConnection]/[immediateTransaction] —
 * the same KMP-unified transaction API established by [RoomFamilyRepository][com.togetherly.data.family.RoomFamilyRepository]
 * (`RoomDatabase.withTransaction` is Android/JVM-only, unavailable on iOS/common). SQLite allows
 * exactly one writer at a time, and Room's writer connection serializes every
 * `immediateTransaction` block onto it — a second concurrent call to either method genuinely
 * waits for the first to fully commit before its own check runs, rather than racing it. That
 * serialization, not application-level locking, is what makes the check-and-write in each method
 * atomic.
 */
internal class RoomQuestSessionTransaction(
    private val completionDao: CompletionDao,
    private val activeQuestSessionMapper: ActiveQuestSessionMapper,
    private val completionMapper: QuestCompletionMapper,
    private val database: TogetherlyDatabase,
    private val dispatchers: AppDispatchers,
    private val diagnostics: OperationalDiagnostics,
) : QuestSessionTransaction {

    override suspend fun startActiveSession(session: ActiveQuestSession): DataResult<Unit> =
        dispatchers.runCatchingStorage(StorageError.WRITE_FAILED, diagnostics) {
            val entity = activeQuestSessionMapper.toEntity(session).getOrElse { return@runCatchingStorage DataResult.Error(it) }
            val started = database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    when (val existing = completionDao.getActiveSession(ACTIVE_QUEST_SESSION_SLOT_ID)) {
                        null -> {
                            completionDao.insertActiveSession(entity)
                            true
                        }
                        else -> existing.completionId == entity.completionId
                    }
                }
            }
            if (started) {
                DataResult.Success(Unit)
            } else {
                DataResult.Error(AppError.Validation(ValidationError.ACTIVE_SESSION_CONFLICT))
            }
        }

    override suspend fun completeActiveSession(completion: QuestCompletion): DataResult<Unit> =
        dispatchers.runCatchingStorage(StorageError.WRITE_FAILED, diagnostics) {
            val components = completionMapper.toComponents(completion)
            val completed = database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    val existing = completionDao.getActiveSession(ACTIVE_QUEST_SESSION_SLOT_ID)
                    if (existing == null || existing.completionId != completion.id.value) {
                        false
                    } else {
                        completionDao.replaceCompletion(
                            completion = components.completion,
                            reactions = components.reactions,
                            media = components.media,
                        )
                        completionDao.deleteActiveSession(ACTIVE_QUEST_SESSION_SLOT_ID)
                        true
                    }
                }
            }
            if (completed) {
                DataResult.Success(Unit)
            } else {
                DataResult.Error(AppError.Validation(ValidationError.ACTIVE_SESSION_MISMATCH))
            }
        }

    override suspend fun replaceActiveSession(session: ActiveQuestSession): DataResult<Unit> =
        dispatchers.runCatchingStorage(StorageError.WRITE_FAILED, diagnostics) {
            val entity = activeQuestSessionMapper.toEntity(session).getOrElse { return@runCatchingStorage DataResult.Error(it) }
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    completionDao.insertActiveSession(entity)
                }
            }
            DataResult.Success(Unit)
        }
}

package com.togetherly.data.completion

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.core.result.getOrElse
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.data.catchStorageReadErrors
import com.togetherly.data.local.completion.ACTIVE_QUEST_SESSION_SLOT_ID
import com.togetherly.data.local.completion.CompletionDao
import com.togetherly.data.local.completion.QuestCompletionWithDetails
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.data.local.mapper.ActiveQuestSessionMapper
import com.togetherly.data.local.mapper.QuestCompletionMapper
import com.togetherly.data.runCatchingStorage
import com.togetherly.domain.completion.ActiveQuestSession
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.QuestCompletion
import com.togetherly.domain.completion.repository.CompletionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A second, focused deviation from the task's literal constructor (alongside
 * [RoomDailyQuestRepository][com.togetherly.data.daily.RoomDailyQuestRepository]'s extra
 * `dismissedQuestMapper`): [ActiveQuestSession] and [QuestCompletion] are different domain types
 * backed by different entities, so this needs both [activeQuestSessionMapper] and
 * [completionMapper] rather than a single `mapper`.
 *
 * Never logs [QuestCompletion.note] or any [com.togetherly.domain.completion.MemoryMedia]
 * reference — see [QuestCompletionMapper]'s own KDoc.
 */
internal class RoomCompletionRepository(
    private val completionDao: CompletionDao,
    private val activeQuestSessionMapper: ActiveQuestSessionMapper,
    private val completionMapper: QuestCompletionMapper,
    private val database: TogetherlyDatabase,
    private val dispatchers: AppDispatchers,
    private val diagnostics: OperationalDiagnostics,
) : CompletionRepository {

    override fun observeActiveSession(): Flow<DataResult<ActiveQuestSession?>> =
        completionDao.observeActiveSession(ACTIVE_QUEST_SESSION_SLOT_ID)
            .map { entity -> entity?.let { activeQuestSessionMapper.toDomain(it) } ?: DataResult.Success(null) }
            .catchStorageReadErrors(diagnostics)

    override suspend fun getActiveSession(): DataResult<ActiveQuestSession?> =
        dispatchers.runCatchingStorage(StorageError.READ_FAILED, diagnostics) {
            val entity = completionDao.getActiveSession(ACTIVE_QUEST_SESSION_SLOT_ID)
                ?: return@runCatchingStorage DataResult.Success(null)
            activeQuestSessionMapper.toDomain(entity)
        }

    /** MVP supports exactly one active session: this always writes to the singleton slot, replacing whatever session already exists. */
    override suspend fun saveActiveSession(session: ActiveQuestSession): DataResult<Unit> =
        dispatchers.runCatchingStorage(StorageError.WRITE_FAILED, diagnostics) {
            val entity = activeQuestSessionMapper.toEntity(session).getOrElse { return@runCatchingStorage DataResult.Error(it) }
            completionDao.insertActiveSession(entity)
            DataResult.Success(Unit)
        }

    /** Idempotent: clearing when no session exists still succeeds. */
    override suspend fun clearActiveSession(): DataResult<Unit> =
        dispatchers.runCatchingStorage(StorageError.DELETE_FAILED, diagnostics) {
            completionDao.deleteActiveSession(ACTIVE_QUEST_SESSION_SLOT_ID)
            DataResult.Success(Unit)
        }

    override fun observeCompletions(): Flow<DataResult<List<QuestCompletion>>> =
        completionDao.observeCompletions()
            .map { relations -> mapAll(relations) }
            .catchStorageReadErrors(diagnostics)

    override fun observeCompletion(completionId: CompletionId): Flow<DataResult<QuestCompletion?>> =
        completionDao.observeCompletion(completionId.value)
            .map { relation -> relation?.let { completionMapper.toDomain(it) } ?: DataResult.Success(null) }
            .catchStorageReadErrors(diagnostics)

    override suspend fun getCompletions(): DataResult<List<QuestCompletion>> =
        dispatchers.runCatchingStorage(StorageError.READ_FAILED, diagnostics) { mapAll(completionDao.getCompletions()) }

    override suspend fun getCompletion(completionId: CompletionId): DataResult<QuestCompletion?> =
        dispatchers.runCatchingStorage(StorageError.READ_FAILED, diagnostics) {
            val relation = completionDao.getCompletion(completionId.value) ?: return@runCatchingStorage DataResult.Success(null)
            completionMapper.toDomain(relation)
        }

    /**
     * Transactionally replaces all three tables backing [completion] via
     * [CompletionDao.replaceCompletion] — see that method's own KDoc for why this is a full
     * delete-then-reinsert of reactions/media rather than a diff. Uses Room's KMP-unified
     * transaction API (see [RoomFamilyRepository][com.togetherly.data.family.RoomFamilyRepository]'s
     * own KDoc for why not `withTransaction`), so no partially written completion is ever visible:
     * the primary row and its reaction/media rows commit or fail together. Domain validation
     * (unique media ids, at most one photo and one voice) already ran when [completion] was
     * constructed — this only persists an already-valid model.
     */
    override suspend fun saveCompletion(completion: QuestCompletion): DataResult<Unit> =
        dispatchers.runCatchingStorage(StorageError.WRITE_FAILED, diagnostics) {
            val components = completionMapper.toComponents(completion)
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    completionDao.replaceCompletion(
                        completion = components.completion,
                        reactions = components.reactions,
                        media = components.media,
                    )
                }
            }
            DataResult.Success(Unit)
        }

    /**
     * Deletes the completion row; see [CompletionDao.deleteCompletion]'s own KDoc for why this
     * alone is enough to remove reaction/media *metadata* too. Deleting the actual photo/voice
     * files a completion referenced is a separate, not-yet-implemented responsibility — this
     * repository only ever touches metadata rows.
     */
    override suspend fun deleteCompletion(completionId: CompletionId): DataResult<Unit> =
        dispatchers.runCatchingStorage(StorageError.DELETE_FAILED, diagnostics) {
            completionDao.deleteCompletion(completionId.value)
            DataResult.Success(Unit)
        }

    private fun mapAll(relations: List<QuestCompletionWithDetails>): DataResult<List<QuestCompletion>> {
        val results = mutableListOf<QuestCompletion>()
        for (relation in relations) {
            results += completionMapper.toDomain(relation).getOrElse { return DataResult.Error(it) }
        }
        return DataResult.Success(results)
    }
}

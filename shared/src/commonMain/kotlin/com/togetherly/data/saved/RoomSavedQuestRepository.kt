package com.togetherly.data.saved

import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.core.result.getOrElse
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.data.catchStorageReadErrors
import com.togetherly.data.local.mapper.SavedQuestMapper
import com.togetherly.data.local.saved.SavedQuestDao
import com.togetherly.data.local.saved.SavedQuestEntity
import com.togetherly.data.runCatchingStorage
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.saved.SavedQuest
import com.togetherly.domain.saved.repository.SavedQuestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

/**
 * Never checks the bundled catalogue — reconciling saved quests against catalogue changes is a
 * use case's concern (see [SavedQuestRepository]'s own KDoc), not this repository's.
 */
internal class RoomSavedQuestRepository(
    private val savedQuestDao: SavedQuestDao,
    private val mapper: SavedQuestMapper,
    private val dispatchers: AppDispatchers,
    private val diagnostics: OperationalDiagnostics,
) : SavedQuestRepository {

    override fun observeSavedQuests(): Flow<DataResult<List<SavedQuest>>> =
        savedQuestDao.observeSavedQuests()
            .map { entities -> mapAll(entities) }
            .catchStorageReadErrors(diagnostics)

    override suspend fun getSavedQuests(): DataResult<List<SavedQuest>> =
        dispatchers.runCatchingStorage(StorageError.READ_FAILED, diagnostics) { mapAll(savedQuestDao.getSavedQuests()) }

    override suspend fun isSaved(questId: QuestId): DataResult<Boolean> =
        dispatchers.runCatchingStorage(StorageError.READ_FAILED, diagnostics) {
            DataResult.Success(savedQuestDao.getSavedQuest(questId.value) != null)
        }

    /**
     * Preferred behavior (see [SavedQuestRepository.save]'s own KDoc): re-saving an
     * already-saved quest preserves the original saved timestamp rather than bumping it to
     * [savedQuest]'s — this keeps repeated save taps from silently reordering the saved list.
     */
    override suspend fun save(savedQuest: SavedQuest): DataResult<Unit> =
        dispatchers.runCatchingStorage(StorageError.WRITE_FAILED, diagnostics) {
            val existing = savedQuestDao.getSavedQuest(savedQuest.questId.value)
            val toPersist = if (existing != null) {
                savedQuest.copy(savedAt = Instant.fromEpochMilliseconds(existing.savedAtEpochMillis))
            } else {
                savedQuest
            }
            val entity = mapper.toEntity(toPersist).getOrElse { return@runCatchingStorage DataResult.Error(it) }
            savedQuestDao.insertSavedQuest(entity)
            DataResult.Success(Unit)
        }

    /** Idempotent: removing a quest that was never saved still succeeds. */
    override suspend fun remove(questId: QuestId): DataResult<Unit> =
        dispatchers.runCatchingStorage(StorageError.DELETE_FAILED, diagnostics) {
            savedQuestDao.deleteSavedQuest(questId.value)
            DataResult.Success(Unit)
        }

    private fun mapAll(entities: List<SavedQuestEntity>): DataResult<List<SavedQuest>> {
        val results = mutableListOf<SavedQuest>()
        for (entity in entities) {
            results += mapper.toDomain(entity).getOrElse { return DataResult.Error(it) }
        }
        return DataResult.Success(results)
    }
}

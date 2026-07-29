package com.togetherly.data.journey

import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.core.result.getOrElse
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.data.catchStorageReadErrors
import com.togetherly.data.local.completion.CompletionDao
import com.togetherly.data.local.completion.QuestCompletionWithDetails
import com.togetherly.data.local.mapper.QuestCompletionMapper
import com.togetherly.data.runCatchingStorage
import com.togetherly.domain.completion.QuestCompletion
import com.togetherly.domain.journey.JourneyEntry
import com.togetherly.domain.journey.repository.JourneyRepository
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.repository.QuestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Journey has no storage of its own (see [JourneyRepository]'s own KDoc) — every read here derives
 * from [CompletionDao] joined, in memory, against [questRepository]'s catalogue. The catalogue is
 * loaded/observed exactly once per read (a single [QuestRepository.getAllQuests]/
 * [QuestRepository.observeAllQuests] call, indexed into a map by [com.togetherly.domain.quest.QuestId])
 * and joined against however many completions exist, never one catalogue call per completion —
 * see [joinJourney]. This class never writes: no `save`/`update`/`delete` method exists on
 * [JourneyRepository] to implement.
 */
internal class RoomJourneyRepository(
    private val completionDao: CompletionDao,
    private val questRepository: QuestRepository,
    private val completionMapper: QuestCompletionMapper,
    private val dispatchers: AppDispatchers,
    private val diagnostics: OperationalDiagnostics,
) : JourneyRepository {

    override fun observeJourney(): Flow<DataResult<List<JourneyEntry>>> =
        combine(observeCompletionsResult(), questRepository.observeAllQuests()) { completionsResult, questsResult ->
            joinJourney(completionsResult, questsResult)
        }

    override suspend fun getJourney(): DataResult<List<JourneyEntry>> {
        val completionsResult = dispatchers.runCatchingStorage(StorageError.READ_FAILED, diagnostics) {
            mapAllCompletions(completionDao.getCompletions())
        }
        val questsResult = questRepository.getAllQuests()
        return joinJourney(completionsResult, questsResult)
    }

    private fun observeCompletionsResult(): Flow<DataResult<List<QuestCompletion>>> =
        completionDao.observeCompletions()
            .map { relations -> mapAllCompletions(relations) }
            .catchStorageReadErrors(diagnostics)

    private fun mapAllCompletions(relations: List<QuestCompletionWithDetails>): DataResult<List<QuestCompletion>> {
        val results = mutableListOf<QuestCompletion>()
        for (relation in relations) {
            results += completionMapper.toDomain(relation).getOrElse { return DataResult.Error(it) }
        }
        return DataResult.Success(results)
    }

    /**
     * Preferred degraded-mode policy (documented on [JourneyRepository] and [JourneyEntry]): a
     * completions-read failure is a genuine Journey [AppError.Storage][com.togetherly.core.error.AppError.Storage]
     * error — family memories themselves couldn't be read, so there is nothing honest to return.
     * A catalogue failure is *not* propagated as an error: every completion is still returned,
     * each with [JourneyEntry.quest] left `null` rather than losing access to a family's memories
     * just because bundled content couldn't load. A [QuestId][com.togetherly.domain.quest.QuestId]
     * absent from a successfully loaded catalogue (removed/historical content) gets the same
     * `null` treatment — this never fabricates a quest title either way.
     */
    private fun joinJourney(
        completionsResult: DataResult<List<QuestCompletion>>,
        questsResult: DataResult<List<FamilyQuest>>,
    ): DataResult<List<JourneyEntry>> {
        val completions = when (completionsResult) {
            is DataResult.Error -> return completionsResult
            is DataResult.Success -> completionsResult.value
        }
        val questsById = (questsResult as? DataResult.Success)?.value?.associateBy { it.id } ?: emptyMap()
        return DataResult.Success(completions.map { completion -> JourneyEntry(completion, questsById[completion.questId]) })
    }
}

package com.togetherly.domain.quest.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.core.result.map
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPack
import com.togetherly.domain.quest.QuestPackId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeQuestRepository : QuestRepository {

    private val questsFlow = MutableStateFlow<DataResult<List<FamilyQuest>>>(DataResult.Success(emptyList()))
    private val packsFlow = MutableStateFlow<DataResult<List<QuestPack>>>(DataResult.Success(emptyList()))

    private var nextError: AppError? = null

    fun setQuests(quests: List<FamilyQuest>) {
        questsFlow.value = DataResult.Success(quests)
    }

    fun setPacks(packs: List<QuestPack>) {
        packsFlow.value = DataResult.Success(packs.sortedBy { it.sortOrder })
    }

    /** Unlike [setNextError] (which only affects the suspend `get*` methods), this pushes the error through [observeAllQuests] itself — needed for callers observing the Flow rather than polling it. */
    fun setQuestsError(error: AppError) {
        questsFlow.value = DataResult.Error(error)
    }

    fun setNextError(error: AppError) {
        nextError = error
    }

    private fun consumeError(): AppError? {
        val error = nextError
        nextError = null
        return error
    }

    override fun observeAllQuests(): Flow<DataResult<List<FamilyQuest>>> = questsFlow

    override fun observeQuest(questId: QuestId): Flow<DataResult<FamilyQuest?>> =
        questsFlow.map { result -> result.map { quests -> quests.find { it.id == questId } } }

    override suspend fun getAllQuests(): DataResult<List<FamilyQuest>> {
        consumeError()?.let { return DataResult.Error(it) }
        return questsFlow.value
    }

    override suspend fun getQuest(questId: QuestId): DataResult<FamilyQuest?> {
        consumeError()?.let { return DataResult.Error(it) }
        return questsFlow.value.map { quests -> quests.find { it.id == questId } }
    }

    override fun observePacks(): Flow<DataResult<List<QuestPack>>> = packsFlow

    override suspend fun getPacks(): DataResult<List<QuestPack>> {
        consumeError()?.let { return DataResult.Error(it) }
        return packsFlow.value
    }

    override suspend fun getPack(packId: QuestPackId): DataResult<QuestPack?> {
        consumeError()?.let { return DataResult.Error(it) }
        return packsFlow.value.map { packs -> packs.find { it.id == packId } }
    }
}

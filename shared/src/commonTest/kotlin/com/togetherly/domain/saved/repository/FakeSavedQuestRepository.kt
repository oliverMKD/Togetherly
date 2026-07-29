package com.togetherly.domain.saved.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.saved.SavedQuest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSavedQuestRepository : SavedQuestRepository {

    private val savedFlow = MutableStateFlow<DataResult<List<SavedQuest>>>(DataResult.Success(emptyList()))

    private var nextError: AppError? = null

    fun setNextError(error: AppError) {
        nextError = error
    }

    private fun consumeError(): AppError? {
        val error = nextError
        nextError = null
        return error
    }

    private fun currentList(): List<SavedQuest> =
        (savedFlow.value as? DataResult.Success)?.value ?: emptyList()

    private fun emitSortedNewestFirst(list: List<SavedQuest>) {
        savedFlow.value = DataResult.Success(list.sortedByDescending { it.savedAt })
    }

    override fun observeSavedQuests(): Flow<DataResult<List<SavedQuest>>> = savedFlow

    override suspend fun getSavedQuests(): DataResult<List<SavedQuest>> {
        consumeError()?.let { return DataResult.Error(it) }
        return savedFlow.value
    }

    override suspend fun isSaved(questId: QuestId): DataResult<Boolean> {
        consumeError()?.let { return DataResult.Error(it) }
        return DataResult.Success(currentList().any { it.questId == questId })
    }

    /**
     * Re-saving an already-saved quest preserves the original [SavedQuest.savedAt] rather than
     * bumping it to [savedQuest]'s — matching [com.togetherly.domain.saved.repository.SavedQuestRepository]'s
     * preferred behavior (see its own KDoc) so repeated save taps never silently reorder the list.
     */
    override suspend fun save(savedQuest: SavedQuest): DataResult<Unit> {
        consumeError()?.let { return DataResult.Error(it) }
        val existing = currentList().find { it.questId == savedQuest.questId }
        val toPersist = existing?.let { savedQuest.copy(savedAt = it.savedAt) } ?: savedQuest
        val withoutExisting = currentList().filterNot { it.questId == savedQuest.questId }
        emitSortedNewestFirst(withoutExisting + toPersist)
        return DataResult.Success(Unit)
    }

    override suspend fun remove(questId: QuestId): DataResult<Unit> {
        consumeError()?.let { return DataResult.Error(it) }
        emitSortedNewestFirst(currentList().filterNot { it.questId == questId })
        return DataResult.Success(Unit)
    }
}

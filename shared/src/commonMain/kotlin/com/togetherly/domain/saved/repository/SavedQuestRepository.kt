package com.togetherly.domain.saved.repository

import com.togetherly.core.result.DataResult
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.saved.SavedQuest
import kotlinx.coroutines.flow.Flow

/**
 * Exactly one saved record exists per quest — [save] on an already-saved quest and [remove] on
 * a quest that isn't saved are both idempotent no-ops. Default ordering is newest saved first
 * (descending by [SavedQuest.savedAt]). This repository does not verify the quest still exists
 * in the catalogue; reconciling saved quests against catalogue changes is handled by
 * orchestration or content migration later.
 */
interface SavedQuestRepository {

    fun observeSavedQuests(): Flow<DataResult<List<SavedQuest>>>

    suspend fun getSavedQuests(): DataResult<List<SavedQuest>>

    suspend fun isSaved(
        questId: QuestId,
    ): DataResult<Boolean>

    suspend fun save(
        savedQuest: SavedQuest,
    ): DataResult<Unit>

    suspend fun remove(
        questId: QuestId,
    ): DataResult<Unit>
}

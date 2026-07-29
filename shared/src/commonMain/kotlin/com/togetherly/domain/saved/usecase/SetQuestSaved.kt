package com.togetherly.domain.saved.usecase

import com.togetherly.core.datetime.AppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.QuestRepository
import com.togetherly.domain.saved.SavedQuest
import com.togetherly.domain.saved.repository.SavedQuestRepository

/**
 * An explicit desired state ([saved]) rather than a toggle — a toggle is ambiguous whenever the
 * caller's view of current state could be stale.
 */
class SetQuestSaved(
    private val savedQuestRepository: SavedQuestRepository,
    private val questRepository: QuestRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(
        questId: QuestId,
        saved: Boolean,
    ): DataResult<Boolean> {
        if (!saved) {
            val removeResult = savedQuestRepository.remove(questId)
            if (removeResult is DataResult.Error) return removeResult
            return DataResult.Success(false)
        }

        val questResult = questRepository.getQuest(questId)
        if (questResult is DataResult.Error) return questResult
        val quest = (questResult as DataResult.Success).value
            ?: return DataResult.Error(AppError.Content(ContentError.QUEST_NOT_FOUND))

        val saveResult = savedQuestRepository.save(SavedQuest(questId = quest.id, savedAt = clock.now()))
        if (saveResult is DataResult.Error) return saveResult

        return DataResult.Success(true)
    }
}

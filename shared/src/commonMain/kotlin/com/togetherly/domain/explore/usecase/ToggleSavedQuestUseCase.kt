package com.togetherly.domain.explore.usecase

import com.togetherly.core.result.DataResult
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.saved.repository.SavedQuestRepository
import com.togetherly.domain.saved.usecase.SetQuestSaved

/**
 * Reuses [SetQuestSaved] for the actual write — [SetQuestSaved] itself deliberately takes an
 * explicit desired state rather than toggling (see its own KDoc: "a toggle is ambiguous whenever
 * the caller's view of current state could be stale"), so this use case is what actually resolves
 * *current* state immediately before flipping it, keeping that ambiguity out of [SetQuestSaved]
 * itself while still giving Explore's card-tap interaction the toggle semantics it needs.
 */
class ToggleSavedQuestUseCase(
    private val savedQuestRepository: SavedQuestRepository,
    private val setQuestSaved: SetQuestSaved,
) {
    suspend operator fun invoke(questId: QuestId): DataResult<Boolean> {
        val isSavedResult = savedQuestRepository.isSaved(questId)
        if (isSavedResult is DataResult.Error) return isSavedResult
        val isSaved = (isSavedResult as DataResult.Success).value

        return setQuestSaved(questId, !isSaved)
    }
}

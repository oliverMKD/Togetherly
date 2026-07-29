package com.togetherly.domain.explore.usecase

import com.togetherly.core.result.DataResult
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.repository.QuestRepository
import com.togetherly.domain.saved.repository.SavedQuestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Resolves [SavedQuestRepository.observeSavedQuests]' saved-quest IDs into full [FamilyQuest]
 * objects — unlike [ObserveSavedQuestIdsUseCase] (bare IDs, for a catalogue grid's O(1) `in`
 * checks), the Saved screen needs complete quest content to render cards. A saved quest whose id no
 * longer resolves in the catalogue (a removed/renamed quest) is silently skipped, the same
 * reasoning [GetQuestPackUseCase] applies to a pack's own stale quest references — never surfaced
 * as an error.
 *
 * Ordering follows [SavedQuestRepository.observeSavedQuests]'s own newest-saved-first convention.
 */
class ObserveSavedQuestsUseCase(
    private val savedQuestRepository: SavedQuestRepository,
    private val questRepository: QuestRepository,
) {
    operator fun invoke(): Flow<DataResult<List<FamilyQuest>>> = combine(
        savedQuestRepository.observeSavedQuests(),
        questRepository.observeAllQuests(),
    ) { savedResult, questsResult ->
        if (savedResult is DataResult.Error) return@combine savedResult
        if (questsResult is DataResult.Error) return@combine questsResult
        val saved = (savedResult as DataResult.Success).value
        val questsById = (questsResult as DataResult.Success).value.associateBy { it.id }
        DataResult.Success(saved.mapNotNull { questsById[it.questId] })
    }
}

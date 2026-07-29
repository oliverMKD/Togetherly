package com.togetherly.domain.explore.usecase

import com.togetherly.core.result.DataResult
import com.togetherly.core.result.map
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.saved.repository.SavedQuestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Maps [SavedQuestRepository.observeSavedQuests]' `List<SavedQuest>` down to the bare
 * `Set<QuestId>` a catalogue grid actually needs — an O(1) `in` check per card, rather than every
 * card independently scanning the full saved list.
 */
class ObserveSavedQuestIdsUseCase(
    private val savedQuestRepository: SavedQuestRepository,
) {
    operator fun invoke(): Flow<DataResult<Set<QuestId>>> =
        savedQuestRepository.observeSavedQuests().map { result -> result.map { saved -> saved.map { it.questId }.toSet() } }
}

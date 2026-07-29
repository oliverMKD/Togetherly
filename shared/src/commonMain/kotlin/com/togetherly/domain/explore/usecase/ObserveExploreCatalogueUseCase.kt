package com.togetherly.domain.explore.usecase

import com.togetherly.core.result.DataResult
import com.togetherly.domain.explore.ExploreCatalogue
import com.togetherly.domain.quest.repository.QuestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Pairs [QuestRepository.observeAllQuests]/[QuestRepository.observePacks] into one stream —
 * Explore's own base data source. Deliberately does not apply search, filters, or access here:
 * those are separate, focused use cases ([SearchQuestsUseCase]/[FilterQuestsUseCase]/
 * [EvaluateQuestAccessUseCase]/[EvaluatePackAccessUseCase]) composed on top by whatever ViewModel
 * observes this, so a filter/search change never has to re-touch the catalogue repository itself.
 * A failure on either side surfaces as a single [DataResult.Error] — Explore has nothing useful to
 * show with only half a catalogue loaded.
 */
class ObserveExploreCatalogueUseCase(
    private val questRepository: QuestRepository,
) {
    operator fun invoke(): Flow<DataResult<ExploreCatalogue>> = combine(
        questRepository.observeAllQuests(),
        questRepository.observePacks(),
    ) { questsResult, packsResult ->
        if (questsResult is DataResult.Error) return@combine questsResult
        if (packsResult is DataResult.Error) return@combine packsResult
        val quests = (questsResult as DataResult.Success).value
        val packs = (packsResult as DataResult.Success).value
        DataResult.Success(ExploreCatalogue(quests = quests, packs = packs))
    }
}

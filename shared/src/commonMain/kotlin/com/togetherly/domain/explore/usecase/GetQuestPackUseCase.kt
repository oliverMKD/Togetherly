package com.togetherly.domain.explore.usecase

import com.togetherly.core.result.DataResult
import com.togetherly.core.result.map
import com.togetherly.domain.explore.QuestPackDetail
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.domain.quest.repository.QuestRepository

/**
 * More than a pass-through forward of [QuestRepository.getPack] (which is why this exists as its
 * own use case rather than a ViewModel calling the repository directly, unlike e.g. Today's own
 * direct [com.togetherly.domain.family.repository.FamilyRepository] injection) — it also resolves
 * [com.togetherly.domain.quest.QuestPack.questIds] into the actual member quests a pack-detail
 * screen needs, which requires a second repository read this use case is the one place that
 * combines.
 */
class GetQuestPackUseCase(
    private val questRepository: QuestRepository,
) {
    suspend operator fun invoke(packId: QuestPackId): DataResult<QuestPackDetail?> {
        val packResult = questRepository.getPack(packId)
        if (packResult is DataResult.Error) return packResult
        val pack = (packResult as DataResult.Success).value ?: return DataResult.Success(null)

        val questsResult = questRepository.getAllQuests()
        return questsResult.map { allQuests ->
            val questsById = allQuests.associateBy { it.id }
            QuestPackDetail(pack = pack, quests = pack.questIds.mapNotNull { questsById[it] })
        }
    }
}

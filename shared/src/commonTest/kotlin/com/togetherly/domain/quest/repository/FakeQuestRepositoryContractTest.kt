package com.togetherly.domain.quest.repository

import com.togetherly.core.error.AppError
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestPack

internal class FakeQuestRepositoryContractTest : QuestRepositoryContractTest() {

    override fun repository(quests: List<FamilyQuest>, packs: List<QuestPack>): QuestRepository =
        FakeQuestRepository().apply {
            setQuests(quests)
            setPacks(packs)
        }

    override fun repositoryWithError(error: AppError): QuestRepository =
        FakeQuestRepository().apply { setNextError(error) }
}

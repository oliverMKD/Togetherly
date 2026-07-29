package com.togetherly.content.repository

import com.togetherly.content.loader.FakeQuestCatalogueLoader
import com.togetherly.content.mapper.QuestCatalogue
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestPack
import com.togetherly.domain.quest.repository.QuestRepository
import com.togetherly.domain.quest.repository.QuestRepositoryContractTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher

internal class BundledQuestRepositoryContractTest : QuestRepositoryContractTest() {

    override fun repository(quests: List<FamilyQuest>, packs: List<QuestPack>): QuestRepository {
        val catalogue = QuestCatalogue(
            schemaVersion = 1,
            catalogueVersion = 1,
            locale = "en",
            packs = packs,
            quests = quests,
        )
        return BundledQuestRepository(
            catalogueLoader = FakeQuestCatalogueLoader(DataResult.Success(catalogue)),
            dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
        )
    }

    override fun repositoryWithError(error: AppError): QuestRepository = BundledQuestRepository(
        catalogueLoader = FakeQuestCatalogueLoader(DataResult.Error(error)),
        dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
    )
}

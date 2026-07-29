package com.togetherly.content.loader

import com.togetherly.content.mapper.QuestCatalogue
import com.togetherly.core.result.DataResult

internal class FakeQuestCatalogueLoader(
    private var result: DataResult<QuestCatalogue>,
) : QuestCatalogueLoader {

    var loadCount: Int = 0
        private set

    fun setResult(result: DataResult<QuestCatalogue>) {
        this.result = result
    }

    override suspend fun load(): DataResult<QuestCatalogue> {
        loadCount++
        return result
    }
}

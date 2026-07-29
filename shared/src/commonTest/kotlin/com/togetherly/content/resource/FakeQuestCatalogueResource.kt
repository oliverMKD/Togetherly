package com.togetherly.content.resource

import com.togetherly.core.result.DataResult

internal class FakeQuestCatalogueResource(
    private var result: DataResult<String>,
) : QuestCatalogueResource {

    var readCount: Int = 0
        private set

    fun setResult(result: DataResult<String>) {
        this.result = result
    }

    override suspend fun readCatalogue(): DataResult<String> {
        readCount++
        return result
    }
}

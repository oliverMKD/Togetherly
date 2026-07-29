package com.togetherly.content.loader

import com.togetherly.content.mapper.QuestCatalogue
import com.togetherly.core.result.DataResult

/**
 * Runs the full content pipeline — read, parse, validate, map — and returns a ready-to-use
 * domain-shaped [QuestCatalogue]. A validation or mapping failure is never partially applied:
 * [load] either returns the complete catalogue or a single typed error, never a partial result.
 */
internal interface QuestCatalogueLoader {
    suspend fun load(): DataResult<QuestCatalogue>
}

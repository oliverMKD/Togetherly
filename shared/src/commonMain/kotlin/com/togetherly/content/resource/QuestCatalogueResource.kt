package com.togetherly.content.resource

import com.togetherly.core.result.DataResult

/**
 * The single place the bundled catalogue's resource filename is declared — both the production
 * implementation and any future replacement must read this constant rather than hardcode a path.
 *
 * The file at this path is the initial curated English catalogue (Step 5.5): one free starter
 * pack, 21 free quests, three per category.
 */
internal const val QUEST_CATALOGUE_RESOURCE_PATH = "files/content/quest-catalogue-en-v1.json"

/**
 * Isolates the content/loading pipeline from the generated Compose Resources API — nothing
 * outside this file depends on `Res` directly, so the pipeline stays testable with fake content
 * and swappable if the resource-loading mechanism ever changes.
 */
internal interface QuestCatalogueResource {
    suspend fun readCatalogue(): DataResult<String>
}

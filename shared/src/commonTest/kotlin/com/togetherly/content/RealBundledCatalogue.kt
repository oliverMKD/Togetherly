package com.togetherly.content

import com.togetherly.content.loader.FakeQuestCatalogueLoader
import com.togetherly.content.mapper.ContentMappingResult
import com.togetherly.content.mapper.DefaultFamilyQuestMapper
import com.togetherly.content.mapper.DefaultQuestCatalogueMapper
import com.togetherly.content.mapper.DefaultQuestPackMapper
import com.togetherly.content.mapper.QuestCatalogue
import com.togetherly.content.repository.BundledQuestRepository
import com.togetherly.content.schema.KotlinxQuestCatalogueParser
import com.togetherly.content.schema.questCatalogueJson
import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.result.DataResult
import com.togetherly.domain.quest.repository.QuestRepository

/**
 * Runs the real content pipeline (parser, mapper — validation is exercised separately in
 * [BundledCatalogueContentTest]) over the real bundled catalogue text ([BUNDLED_CATALOGUE_JSON])
 * to produce the actual [QuestCatalogue]. Use-case integration tests use this rather than
 * hand-built fixture quests specifically so they exercise real content, real IDs and real
 * categories — the whole point of testing against "the real bundled repository."
 */
internal fun loadRealBundledCatalogue(): QuestCatalogue {
    val dto = (KotlinxQuestCatalogueParser(questCatalogueJson).parse(BUNDLED_CATALOGUE_JSON) as DataResult.Success).value
    val mapped = DefaultQuestCatalogueMapper(DefaultFamilyQuestMapper(), DefaultQuestPackMapper()).map(dto)
    return (mapped as ContentMappingResult.Success).value
}

/**
 * A [QuestRepository] backed by the real, mapped bundled catalogue — everything downstream of
 * resource reading is real; only the resource read itself is replaced with the fixture copy, for
 * the same `testAndroidHostTest` reason documented on [BUNDLED_CATALOGUE_JSON].
 */
internal fun realBundledQuestRepository(dispatchers: AppDispatchers): QuestRepository =
    BundledQuestRepository(
        catalogueLoader = FakeQuestCatalogueLoader(DataResult.Success(loadRealBundledCatalogue())),
        dispatchers = dispatchers,
    )

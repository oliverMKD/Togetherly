package com.togetherly.content.mapper

import com.togetherly.content.schema.KotlinxQuestCatalogueParser
import com.togetherly.content.schema.VALID_MINIMAL_CATALOGUE_JSON
import com.togetherly.content.schema.questCatalogueJson
import com.togetherly.core.result.DataResult
import kotlin.test.Test
import kotlin.test.assertEquals

class QuestCatalogueMapperTest {

    @Test
    fun mapsAParsedMinimalCatalogueEndToEnd() {
        val parser = KotlinxQuestCatalogueParser(questCatalogueJson)
        val dto = (parser.parse(VALID_MINIMAL_CATALOGUE_JSON) as DataResult.Success).value
        val mapper = DefaultQuestCatalogueMapper(DefaultFamilyQuestMapper(), DefaultQuestPackMapper())

        val result = mapper.map(dto)

        val catalogue = (result as ContentMappingResult.Success).value
        assertEquals(1, catalogue.packs.size)
        assertEquals(1, catalogue.quests.size)
    }
}

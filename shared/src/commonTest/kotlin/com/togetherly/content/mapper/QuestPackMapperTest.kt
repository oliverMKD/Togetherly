package com.togetherly.content.mapper

import com.togetherly.content.model.QuestAccessDto
import com.togetherly.content.model.QuestPackDto
import com.togetherly.domain.quest.QuestId
import kotlin.test.Test
import kotlin.test.assertEquals

private fun validPackDto(
    category: String? = null,
    questIds: List<String> = listOf("quest-1"),
) = QuestPackDto(
    id = "pack-1",
    version = 1,
    title = "Backyard Adventures",
    description = "A pack of outdoor quests.",
    category = category,
    access = QuestAccessDto(type = "free"),
    questIds = questIds,
    artworkKey = "packs/backyard-adventures",
    sortOrder = 0,
)

class QuestPackMapperTest {

    private val mapper: QuestPackMapper = DefaultQuestPackMapper()

    @Test
    fun packMappingPreservesQuestOrder() {
        val dto = validPackDto(questIds = listOf("quest-3", "quest-1", "quest-2"))

        val result = mapper.map(dto, "packs[0]")

        val pack = (result as ContentMappingResult.Success).value
        assertEquals(listOf(QuestId("quest-3"), QuestId("quest-1"), QuestId("quest-2")), pack.questIds)
    }

    @Test
    fun nullCategoryMapsToMixedCategoryPack() {
        val result = mapper.map(validPackDto(category = null), "packs[0]")

        assertEquals(null, (result as ContentMappingResult.Success).value.category)
    }
}

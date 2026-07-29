package com.togetherly.content.mapper

import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.PreparationLevel
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CatalogueEnumMappersTest {

    @Test
    fun everyRawCategoryValueMapsCorrectly() {
        val expected = mapOf(
            "talk" to QuestCategory.TALK,
            "create" to QuestCategory.CREATE,
            "move" to QuestCategory.MOVE,
            "kindness" to QuestCategory.KINDNESS,
            "discover" to QuestCategory.DISCOVER,
            "silly" to QuestCategory.SILLY,
            "memories" to QuestCategory.MEMORIES,
        )
        for ((raw, category) in expected) {
            assertEquals(ContentMappingResult.Success(category), mapQuestCategory(raw, "path"))
        }
    }

    @Test
    fun unknownCategoryFails() {
        assertUnknownEnumIssue(mapQuestCategory("TALK", "quests[0].category"), "quests[0].category", "TALK")
    }

    @Test
    fun everyRawAgeBandValueMapsCorrectly() {
        val expected = mapOf(
            "6-8" to AgeBand.AGE_6_TO_8,
            "9-11" to AgeBand.AGE_9_TO_11,
            "12-13" to AgeBand.AGE_12_TO_13,
        )
        for ((raw, ageBand) in expected) {
            assertEquals(ContentMappingResult.Success(ageBand), mapAgeBand(raw, "path"))
        }
    }

    @Test
    fun unknownAgeBandFails() {
        assertUnknownEnumIssue(mapAgeBand("13-17", "quests[2].ageBands[1]"), "quests[2].ageBands[1]", "13-17")
    }

    @Test
    fun everyRawLocationValueMapsCorrectly() {
        val expected = mapOf(
            "indoor" to QuestLocation.INDOOR,
            "outdoor" to QuestLocation.OUTDOOR,
            "either" to QuestLocation.EITHER,
        )
        for ((raw, location) in expected) {
            assertEquals(ContentMappingResult.Success(location), mapQuestLocation(raw, "path"))
        }
    }

    @Test
    fun unknownLocationFails() {
        assertUnknownEnumIssue(mapQuestLocation("outside", "path"), "path", "outside")
    }

    @Test
    fun everyRawPreparationValueMapsCorrectly() {
        val expected = mapOf(
            "none" to PreparationLevel.NONE,
            "simple-materials" to PreparationLevel.SIMPLE_MATERIALS,
            "advanced" to PreparationLevel.ADVANCED,
        )
        for ((raw, preparation) in expected) {
            assertEquals(ContentMappingResult.Success(preparation), mapPreparationLevel(raw, "path"))
        }
    }

    @Test
    fun unknownPreparationFails() {
        assertUnknownEnumIssue(mapPreparationLevel("simple", "path"), "path", "simple")
    }

    @Test
    fun everyRawEnergyValueMapsCorrectly() {
        val expected = mapOf(
            "calm" to EnergyLevel.CALM,
            "moderate" to EnergyLevel.MODERATE,
            "active" to EnergyLevel.ACTIVE,
        )
        for ((raw, energy) in expected) {
            assertEquals(ContentMappingResult.Success(energy), mapEnergyLevel(raw, "path"))
        }
    }

    @Test
    fun unknownEnergyFails() {
        assertUnknownEnumIssue(mapEnergyLevel("high", "path"), "path", "high")
    }

    private fun assertUnknownEnumIssue(result: ContentMappingResult<*>, path: String, value: String) {
        assertIs<ContentMappingResult.Failure>(result)
        assertEquals(ContentMappingIssueCode.UNKNOWN_ENUM_VALUE, result.issue.code)
        assertEquals(path, result.issue.path)
        assertEquals(value, result.issue.value)
    }
}

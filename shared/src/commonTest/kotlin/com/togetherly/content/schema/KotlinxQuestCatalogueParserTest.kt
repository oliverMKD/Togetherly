package com.togetherly.content.schema

import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.result.DataResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class KotlinxQuestCatalogueParserTest {

    private val parser = KotlinxQuestCatalogueParser(questCatalogueJson)

    @Test
    fun validMinimalJsonParses() {
        val result = parser.parse(VALID_MINIMAL_CATALOGUE_JSON)

        val catalogue = (result as DataResult.Success).value
        assertEquals(1, catalogue.schemaVersion)
        assertEquals(1, catalogue.catalogueVersion)
        assertEquals("en", catalogue.locale)
        assertEquals(1, catalogue.packs.size)
        assertEquals(1, catalogue.quests.size)
    }

    @Test
    fun missingRequiredRootFieldFails() {
        val json = """{"schemaVersion":1,"catalogueVersion":1,"packs":[],"quests":[]}"""

        assertContentError(parser.parse(json))
    }

    @Test
    fun unknownRootFieldFails() {
        val json = """
            {"schemaVersion":1,"catalogueVersion":1,"locale":"en","packs":[],"quests":[],"extra":true}
        """.trimIndent()

        assertContentError(parser.parse(json))
    }

    @Test
    fun unknownQuestFieldFails() {
        val json = VALID_MINIMAL_CATALOGUE_JSON.replace(
            "\"completionPrompt\": \"Share what you found!\",",
            "\"completionPrompt\": \"Share what you found!\", \"extraField\": \"oops\",",
        )

        assertContentError(parser.parse(json))
    }

    @Test
    fun malformedJsonReturnsTypedContentError() {
        assertContentError(parser.parse("{ not valid json ]"))
    }

    @Test
    fun defaultEmptyMaterialsWork() {
        val result = parser.parse(VALID_MINIMAL_CATALOGUE_JSON)

        val quest = (result as DataResult.Success).value.quests.single()
        assertEquals(emptyList(), quest.materials)
    }

    @Test
    fun defaultEmptyHintsWork() {
        val result = parser.parse(VALID_MINIMAL_CATALOGUE_JSON)

        val quest = (result as DataResult.Success).value.quests.single()
        assertEquals(emptyList(), quest.hints)
    }

    @Test
    fun defaultCooldownWorks() {
        val result = parser.parse(VALID_MINIMAL_CATALOGUE_JSON)

        val quest = (result as DataResult.Success).value.quests.single()
        assertEquals(30, quest.cooldownDays)
    }

    @Test
    fun explicitNullOptionalValuesBehaveCorrectly() {
        val json = VALID_MINIMAL_CATALOGUE_JSON.replace(
            "\"completionPrompt\": \"Share what you found!\",",
            "\"completionPrompt\": \"Share what you found!\", \"safetyNote\": null,",
        )

        val result = parser.parse(json)

        val quest = (result as DataResult.Success).value.quests.single()
        assertNull(quest.safetyNote)
    }

    @Test
    fun parserDoesNotExposeRawSerializationExceptions() {
        val result = parser.parse("not json at all")

        assertContentError(result)
    }

    private fun assertContentError(result: DataResult<*>) {
        assertIs<DataResult.Error>(result)
        val error = result.error
        assertIs<AppError.Content>(error)
        assertEquals(ContentError.INVALID_CATALOGUE, error.reason)
    }
}

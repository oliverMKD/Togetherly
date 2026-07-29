package com.togetherly.content

import com.togetherly.content.mapper.ContentMappingResult
import com.togetherly.content.mapper.DefaultFamilyQuestMapper
import com.togetherly.content.mapper.DefaultQuestCatalogueMapper
import com.togetherly.content.mapper.DefaultQuestPackMapper
import com.togetherly.content.mapper.QuestCatalogue
import com.togetherly.content.model.QuestCatalogueDto
import com.togetherly.content.schema.KotlinxQuestCatalogueParser
import com.togetherly.content.schema.questCatalogueJson
import com.togetherly.content.validation.CatalogueValidationResult
import com.togetherly.content.validation.QuestCatalogueValidator
import com.togetherly.core.result.DataResult
import com.togetherly.domain.quest.PreparationLevel
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.QuestCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Loads the *actual* bundled catalogue content end to end through the real parser, validator and
 * mapper — nothing faked — unlike the pipeline-mechanics tests in `content.loader`, which
 * deliberately use fake content. This is the one place that verifies the shipped content itself.
 *
 * Sourced from [BUNDLED_CATALOGUE_JSON] (an exact copy of the resource file) rather than through
 * [com.togetherly.content.resource.ComposeQuestCatalogueResource] — see that fixture's KDoc for
 * why the real resource reader cannot run under `testAndroidHostTest`.
 */
class BundledCatalogueContentTest {

    private val parser = KotlinxQuestCatalogueParser(questCatalogueJson)
    private val validator = QuestCatalogueValidator()
    private val mapper = DefaultQuestCatalogueMapper(DefaultFamilyQuestMapper(), DefaultQuestPackMapper())

    private fun loadDto(): QuestCatalogueDto {
        val parseResult = parser.parse(BUNDLED_CATALOGUE_JSON)
        return (parseResult as DataResult.Success).value
    }

    private fun mappedCatalogue(): QuestCatalogue {
        val dto = loadDto()
        val mapped = mapper.map(dto)
        return (mapped as ContentMappingResult.Success).value
    }

    @Test
    fun parsingSucceeds() {
        val parseResult = parser.parse(BUNDLED_CATALOGUE_JSON)
        assertIs<DataResult.Success<QuestCatalogueDto>>(parseResult)
    }

    @Test
    fun validationReturnsNoErrors() {
        val dto = loadDto()

        val result = validator.validate(dto)

        // Valid may still carry non-blocking warnings — only ERROR-severity issues are disallowed.
        assertIs<CatalogueValidationResult.Valid>(result)
    }

    @Test
    fun mappingSucceeds() {
        val dto = loadDto()

        val result = mapper.map(dto)

        assertIs<ContentMappingResult.Success<QuestCatalogue>>(result)
    }

    @Test
    fun catalogueHasExactly45Quests() {
        assertEquals(45, mappedCatalogue().quests.size)
    }

    @Test
    fun everyCategoryIsRepresented() {
        val byCategory = mappedCatalogue().quests.groupingBy { it.category }.eachCount()

        assertEquals(QuestCategory.entries.toSet(), byCategory.keys)
        byCategory.forEach { (category, count) -> assertTrue(count > 0, "expected at least 1 quest for $category") }
    }

    @Test
    fun catalogueHasSixPacks() {
        assertEquals(6, mappedCatalogue().packs.size)
    }

    @Test
    fun everyPackReferencesOnlyQuestsThatBelongToIt() {
        val catalogue = mappedCatalogue()

        catalogue.packs.forEach { pack ->
            val memberQuests = catalogue.quests.filter { it.id in pack.questIds }
            assertEquals(pack.questIds.toSet(), memberQuests.map { it.id }.toSet())
            assertTrue(memberQuests.all { it.packId == pack.id }, "every quest in ${pack.id} should point back to it")
        }
    }

    @Test
    fun everyQuestBelongsToExactlyOneKnownPack() {
        val catalogue = mappedCatalogue()
        val packIds = catalogue.packs.map { it.id }.toSet()

        assertTrue(catalogue.quests.all { it.packId in packIds })
    }

    @Test
    fun catalogueHasTwentyOneFreeAndTwentyFourPremiumQuests() {
        val quests = mappedCatalogue().quests

        assertEquals(21, quests.count { it.access == QuestAccess.Free })
        assertEquals(24, quests.count { it.access is QuestAccess.Premium })
    }

    @Test
    fun freePacksContainOnlyFreeQuests() {
        val catalogue = mappedCatalogue()

        catalogue.packs.filter { it.access == QuestAccess.Free }.forEach { pack ->
            val memberQuests = catalogue.quests.filter { it.id in pack.questIds }
            assertTrue(memberQuests.all { it.access == QuestAccess.Free }, "free pack ${pack.id} must not contain a premium quest")
        }
    }

    @Test
    fun premiumPacksRemainVisibleAndEachHaveAtLeastFourQuests() {
        val catalogue = mappedCatalogue()
        val premiumPacks = catalogue.packs.filter { it.access is QuestAccess.Premium }

        assertEquals(4, premiumPacks.size)
        premiumPacks.forEach { pack ->
            assertTrue(pack.questIds.size in 4..6, "expected 4-6 quests in premium pack ${pack.id}, was ${pack.questIds.size}")
        }
    }

    @Test
    fun freePacksStillHaveEnoughContentToBeUsefulWithoutASubscription() {
        val catalogue = mappedCatalogue()
        val freePacks = catalogue.packs.filter { it.access == QuestAccess.Free }

        assertEquals(2, freePacks.size)
        assertTrue(catalogue.quests.count { it.access == QuestAccess.Free } >= 20)
    }

    @Test
    fun allQuestIdsAreUnique() {
        val ids = mappedCatalogue().quests.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun everyQuestHasAtLeastTwoInstructions() {
        assertTrue(mappedCatalogue().quests.all { it.instructions.size >= 2 })
    }

    @Test
    fun everyQuestSupportsAtLeastOneAgeBand() {
        assertTrue(mappedCatalogue().quests.all { it.ageBands.isNotEmpty() })
    }

    @Test
    fun noQuestUsesAdvancedPreparation() {
        assertTrue(mappedCatalogue().quests.none { it.preparation == PreparationLevel.ADVANCED })
    }

    @Test
    fun noQuestHasAnUnreasonableDuration() {
        assertTrue(mappedCatalogue().quests.all { it.durationMinutes in 5..30 })
    }

    @Test
    fun noContentContainsPlaceholderText() {
        val placeholderMarkers = listOf("lorem ipsum", "todo", "tbd", "placeholder", "xxx", "fixme")

        val allText = buildList {
            mappedCatalogue().quests.forEach { quest ->
                add(quest.title.value)
                add(quest.summary.value)
                add(quest.completionPrompt.value)
                quest.instructions.forEach { add(it.text.value) }
                quest.hints.forEach { add(it.value) }
                quest.materials.forEach { add(it.value) }
                quest.safetyNote?.let { add(it.value) }
            }
        }

        allText.forEach { text ->
            val lower = text.lowercase()
            placeholderMarkers.forEach { marker ->
                assertTrue(marker !in lower, "found placeholder marker '$marker' in: $text")
            }
        }
    }

    @Test
    fun noProductionQuestIdStartsWithTestOrSample() {
        mappedCatalogue().quests.forEach { quest ->
            assertTrue(!quest.id.value.startsWith("test"), "quest id starts with 'test': ${quest.id.value}")
            assertTrue(!quest.id.value.startsWith("sample"), "quest id starts with 'sample': ${quest.id.value}")
        }
    }
}

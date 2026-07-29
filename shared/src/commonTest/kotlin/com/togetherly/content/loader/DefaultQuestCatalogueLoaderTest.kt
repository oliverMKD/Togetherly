package com.togetherly.content.loader

import com.togetherly.content.mapper.ContentMappingException
import com.togetherly.content.mapper.DefaultFamilyQuestMapper
import com.togetherly.content.mapper.DefaultQuestCatalogueMapper
import com.togetherly.content.mapper.DefaultQuestPackMapper
import com.togetherly.content.resource.FakeQuestCatalogueResource
import com.togetherly.content.schema.KotlinxQuestCatalogueParser
import com.togetherly.content.schema.VALID_MINIMAL_CATALOGUE_JSON
import com.togetherly.content.schema.questCatalogueJson
import com.togetherly.content.validation.CatalogueValidationException
import com.togetherly.content.validation.QuestCatalogueValidator
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** A validator failure caught elsewhere: an unsupported schema version, produced by mutating the shared minimal fixture. */
private val VALIDATION_FAILURE_JSON = VALID_MINIMAL_CATALOGUE_JSON.replace(
    "\"schemaVersion\": 1,",
    "\"schemaVersion\": 999,",
)

/**
 * A title with leading whitespace passes catalogue validation (only blank/length are checked
 * there) but fails the stricter domain constructor [com.togetherly.domain.quest.QuestTitle]
 * enforces (no surrounding whitespace) — a real, already-existing gap between the Step 5.3
 * validator and Step 5.2 mapper, used here to exercise the loader's mapping-failure branch.
 */
private val MAPPING_FAILURE_JSON = VALID_MINIMAL_CATALOGUE_JSON.replace(
    "\"title\": \"Backyard Scavenger Hunt\",",
    "\"title\": \" Backyard Scavenger Hunt\",",
)

class DefaultQuestCatalogueLoaderTest {

    private fun loaderWith(resource: FakeQuestCatalogueResource) = DefaultQuestCatalogueLoader(
        resource = resource,
        parser = KotlinxQuestCatalogueParser(questCatalogueJson),
        validator = QuestCatalogueValidator(),
        mapper = DefaultQuestCatalogueMapper(DefaultFamilyQuestMapper(), DefaultQuestPackMapper()),
        diagnostics = FakeOperationalDiagnostics(),
    )

    @Test
    fun resourceReadSuccessProducesTheMappedCatalogue() = runTest {
        val resource = FakeQuestCatalogueResource(DataResult.Success(VALID_MINIMAL_CATALOGUE_JSON))
        val loader = loaderWith(resource)

        val result = loader.load()

        val catalogue = (result as DataResult.Success).value
        assertEquals(1, catalogue.packs.size)
        assertEquals(1, catalogue.quests.size)
    }

    @Test
    fun missingResourcePropagatesTheResourceError() = runTest {
        val missingResourceError = AppError.Content(ContentError.CATALOGUE_UNAVAILABLE)
        val resource = FakeQuestCatalogueResource(DataResult.Error(missingResourceError))
        val loader = loaderWith(resource)

        val result = loader.load()

        assertEquals(DataResult.Error(missingResourceError), result)
    }

    @Test
    fun malformedResourceReturnsTypedContentError() = runTest {
        val resource = FakeQuestCatalogueResource(DataResult.Success("not json at all"))
        val loader = loaderWith(resource)

        val result = loader.load()

        assertContentError(result)
    }

    @Test
    fun validationFailureReturnsTypedErrorAndPreservesTheReport() = runTest {
        val resource = FakeQuestCatalogueResource(DataResult.Success(VALIDATION_FAILURE_JSON))
        val loader = loaderWith(resource)

        val result = loader.load()

        val error = assertContentError(result)
        val cause = assertIs<CatalogueValidationException>(error.cause)
        assertTrue(cause.issues.isNotEmpty())
    }

    @Test
    fun mappingFailureReturnsTypedError() = runTest {
        val resource = FakeQuestCatalogueResource(DataResult.Success(MAPPING_FAILURE_JSON))
        val loader = loaderWith(resource)

        val result = loader.load()

        val error = assertContentError(result)
        assertIs<ContentMappingException>(error.cause)
    }

    @Test
    fun successfulLoadIsCachedAndNotReparsed() = runTest {
        val resource = FakeQuestCatalogueResource(DataResult.Success(VALID_MINIMAL_CATALOGUE_JSON))
        val loader = loaderWith(resource)

        val first = loader.load()
        val second = loader.load()

        assertEquals(1, resource.readCount)
        assertEquals(first, second)
    }

    @Test
    fun failedLoadCanBeRetried() = runTest {
        val resource = FakeQuestCatalogueResource(DataResult.Success("not json at all"))
        val loader = loaderWith(resource)

        assertContentError(loader.load())

        resource.setResult(DataResult.Success(VALID_MINIMAL_CATALOGUE_JSON))
        val retried = loader.load()

        assertIs<DataResult.Success<*>>(retried)
        assertEquals(2, resource.readCount)
    }

    @Test
    fun concurrentLoadDoesNotParseRepeatedly() = runTest {
        val resource = FakeQuestCatalogueResource(DataResult.Success(VALID_MINIMAL_CATALOGUE_JSON))
        val loader = loaderWith(resource)

        val results = (1..20).map { async { loader.load() } }.awaitAll()

        assertEquals(1, resource.readCount)
        assertTrue(results.all { it == results.first() })
    }

    private fun assertContentError(result: DataResult<*>): AppError.Content {
        assertIs<DataResult.Error>(result)
        val error = assertIs<AppError.Content>(result.error)
        assertEquals(ContentError.INVALID_CATALOGUE, error.reason)
        return error
    }
}

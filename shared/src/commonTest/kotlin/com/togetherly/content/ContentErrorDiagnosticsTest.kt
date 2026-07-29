package com.togetherly.content

import com.togetherly.content.loader.DefaultQuestCatalogueLoader
import com.togetherly.content.mapper.ContentMappingException
import com.togetherly.content.mapper.DefaultFamilyQuestMapper
import com.togetherly.content.mapper.DefaultQuestCatalogueMapper
import com.togetherly.content.mapper.DefaultQuestPackMapper
import com.togetherly.content.resource.FakeQuestCatalogueResource
import com.togetherly.content.schema.KotlinxQuestCatalogueParser
import com.togetherly.content.schema.VALID_MINIMAL_CATALOGUE_JSON
import com.togetherly.content.schema.questCatalogueJson
import com.togetherly.content.validation.CatalogueIssueCode
import com.togetherly.content.validation.CatalogueValidationException
import com.togetherly.content.validation.QuestCatalogueValidator
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import com.togetherly.domain.quest.QuestId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Every content-pipeline failure mode must be distinguishable by inspecting [AppError.Content] —
 * its [ContentError] reason plus the internal-only [AppError.Content.cause] — without ever
 * exposing a raw file path or JSON value to a user. "Missing quest" is exercised end to end in
 * `integration.SavedQuestContentIntegrationTest`/`StartQuestContentIntegrationTest` (a use case
 * turns a repository's `Success(null)` into [ContentError.QUEST_NOT_FOUND]); "missing pack" has
 * no consuming use case yet (see Step 4.6), so [ContentError.PACK_NOT_FOUND] exists as a typed
 * reason for one to use, verified as `Success(null)` at the repository level in
 * `content.repository.BundledQuestRepositoryTest`.
 */
class ContentErrorDiagnosticsTest {

    private fun loaderWith(resource: FakeQuestCatalogueResource) = DefaultQuestCatalogueLoader(
        resource = resource,
        parser = KotlinxQuestCatalogueParser(questCatalogueJson),
        validator = QuestCatalogueValidator(),
        mapper = DefaultQuestCatalogueMapper(DefaultFamilyQuestMapper(), DefaultQuestPackMapper()),
        diagnostics = FakeOperationalDiagnostics(),
    )

    @Test
    fun resourceReadFailureIsIdentifiableAsCatalogueUnavailable() = runTest {
        val resource = FakeQuestCatalogueResource(DataResult.Error(AppError.Content(ContentError.CATALOGUE_UNAVAILABLE)))

        val error = (loaderWith(resource).load() as DataResult.Error).error

        val content = assertIs<AppError.Content>(error)
        assertEquals(ContentError.CATALOGUE_UNAVAILABLE, content.reason)
    }

    @Test
    fun malformedJsonIsIdentifiableAsInvalidCatalogueWithASerializationCause() = runTest {
        val resource = FakeQuestCatalogueResource(DataResult.Success("not json at all"))

        val error = (loaderWith(resource).load() as DataResult.Error).error

        val content = assertIs<AppError.Content>(error)
        assertEquals(ContentError.INVALID_CATALOGUE, content.reason)
        // Malformed JSON fails before validation ever runs, so the cause is neither of the two
        // validation/mapping exception types — that absence is itself part of the diagnosis.
        assertTrue(content.cause !is CatalogueValidationException && content.cause !is ContentMappingException)
    }

    @Test
    fun unsupportedSchemaIsIdentifiableViaTheValidationReport() = runTest {
        val json = VALID_MINIMAL_CATALOGUE_JSON.replace("\"schemaVersion\": 1,", "\"schemaVersion\": 999,")
        val resource = FakeQuestCatalogueResource(DataResult.Success(json))

        val error = (loaderWith(resource).load() as DataResult.Error).error

        val content = assertIs<AppError.Content>(error)
        assertEquals(ContentError.INVALID_CATALOGUE, content.reason)
        val cause = assertIs<CatalogueValidationException>(content.cause)
        assertTrue(cause.issues.any { it.code == CatalogueIssueCode.UNSUPPORTED_SCHEMA_VERSION })
    }

    @Test
    fun generalValidationFailureIsIdentifiableViaTheValidationReport() = runTest {
        val json = VALID_MINIMAL_CATALOGUE_JSON.replace(
            "{ \"order\": 1, \"text\": \"Hide five small objects in the yard.\" }",
            "",
        )
        val resource = FakeQuestCatalogueResource(DataResult.Success(json))

        val error = (loaderWith(resource).load() as DataResult.Error).error

        val content = assertIs<AppError.Content>(error)
        assertEquals(ContentError.INVALID_CATALOGUE, content.reason)
        val cause = assertIs<CatalogueValidationException>(content.cause)
        assertTrue(cause.issues.isNotEmpty())
    }

    @Test
    fun mappingFailureIsIdentifiableViaTheMappingIssue() = runTest {
        val json = VALID_MINIMAL_CATALOGUE_JSON.replace(
            "\"title\": \"Backyard Scavenger Hunt\",",
            "\"title\": \" Backyard Scavenger Hunt\",",
        )
        val resource = FakeQuestCatalogueResource(DataResult.Success(json))

        val error = (loaderWith(resource).load() as DataResult.Error).error

        val content = assertIs<AppError.Content>(error)
        assertEquals(ContentError.INVALID_CATALOGUE, content.reason)
        assertIs<ContentMappingException>(content.cause)
    }

    @Test
    fun missingQuestAndMissingPackReasonsExistAsDistinctTypedErrors() {
        // Both are declared for use cases that need them (see this class's own KDoc) — this just
        // guards that the two stay distinct enum values, never collapsed into one generic reason.
        assertTrue(ContentError.QUEST_NOT_FOUND != ContentError.PACK_NOT_FOUND)
    }
}

package com.togetherly.content.schema

import com.togetherly.content.model.QuestCatalogueDto
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.result.DataResult
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Never leaks [SerializationException] — malformed or schema-invalid JSON is mapped to a typed
 * content error instead. The full JSON body is never logged; only the exception is retained
 * internally as [AppError.Content.cause] for diagnostics.
 */
internal class KotlinxQuestCatalogueParser(
    private val json: Json,
) : QuestCatalogueParser {
    override fun parse(json: String): DataResult<QuestCatalogueDto> = try {
        DataResult.Success(this.json.decodeFromString(QuestCatalogueDto.serializer(), json))
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (e: SerializationException) {
        DataResult.Error(AppError.Content(ContentError.INVALID_CATALOGUE, cause = e))
    }
}

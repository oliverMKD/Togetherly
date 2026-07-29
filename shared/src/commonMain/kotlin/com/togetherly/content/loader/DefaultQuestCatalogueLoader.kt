package com.togetherly.content.loader

import com.togetherly.content.mapper.ContentMappingException
import com.togetherly.content.mapper.ContentMappingResult
import com.togetherly.content.mapper.QuestCatalogue
import com.togetherly.content.mapper.QuestCatalogueMapper
import com.togetherly.content.resource.QuestCatalogueResource
import com.togetherly.content.schema.QuestCatalogueParser
import com.togetherly.content.validation.CatalogueValidationException
import com.togetherly.content.validation.CatalogueValidationResult
import com.togetherly.content.validation.QuestCatalogueValidator
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.result.DataResult
import com.togetherly.core.result.flatMap
import com.togetherly.core.telemetry.DiagnosticContext
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.core.telemetry.toDiagnosticThrowable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The bundled catalogue is immutable for the lifetime of the process, so a successful [load] is
 * cached forever and never re-parsed; a failed [load] is never cached, so the next call retries
 * the whole pipeline. [mutex] makes the first concurrent load safe: only one caller actually runs
 * the pipeline, and every other concurrent caller suspends on the mutex and then reads the same
 * result the winner produced, rather than each racing through parsing independently.
 *
 * A validator failure is rejected before mapping ever runs; the full [CatalogueValidationException.issues]
 * report is preserved as the returned error's cause for tests/diagnostics, never surfaced as
 * user-facing text.
 */
internal class DefaultQuestCatalogueLoader(
    private val resource: QuestCatalogueResource,
    private val parser: QuestCatalogueParser,
    private val validator: QuestCatalogueValidator,
    private val mapper: QuestCatalogueMapper,
    private val diagnostics: OperationalDiagnostics,
) : QuestCatalogueLoader {

    private val mutex = Mutex()
    private var cached: DataResult<QuestCatalogue>? = null

    override suspend fun load(): DataResult<QuestCatalogue> {
        cached?.let { return it }
        return mutex.withLock {
            cached?.let { return@withLock it }
            loadFresh().also { result ->
                when (result) {
                    is DataResult.Success -> cached = result
                    is DataResult.Error -> diagnostics.captureHandledException(
                        result.error.toDiagnosticThrowable(),
                        DiagnosticContext(mapOf("feature" to "content", "operation" to "catalogue_load")),
                    )
                }
            }
        }
    }

    private suspend fun loadFresh(): DataResult<QuestCatalogue> =
        resource.readCatalogue()
            .flatMap { json -> parser.parse(json) }
            .flatMap { dto ->
                when (val validation = validator.validate(dto)) {
                    is CatalogueValidationResult.Invalid -> DataResult.Error(
                        AppError.Content(
                            ContentError.INVALID_CATALOGUE,
                            cause = CatalogueValidationException(validation.issues),
                        ),
                    )

                    is CatalogueValidationResult.Valid -> when (val mapped = mapper.map(dto)) {
                        is ContentMappingResult.Success -> DataResult.Success(mapped.value)
                        is ContentMappingResult.Failure -> DataResult.Error(
                            AppError.Content(
                                ContentError.INVALID_CATALOGUE,
                                cause = ContentMappingException(mapped.issue),
                            ),
                        )
                    }
                }
            }
}

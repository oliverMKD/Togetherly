package com.togetherly.content.resource

import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.result.DataResult
import kotlinx.coroutines.CancellationException
import togetherly.shared.generated.resources.Res

/**
 * Reads the bundled catalogue via the generated Compose Resources `Res` API — the only resource
 * mechanism this project depends on. Both a missing resource (`MissingResourceException`) and any
 * other unreadable-resource failure map to the same typed error; the full file content is never
 * logged, only the exception is retained internally as [AppError.Content.cause] for diagnostics.
 */
internal class ComposeQuestCatalogueResource : QuestCatalogueResource {

    override suspend fun readCatalogue(): DataResult<String> = try {
        DataResult.Success(Res.readBytes(QUEST_CATALOGUE_RESOURCE_PATH).decodeToString())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (e: Exception) {
        DataResult.Error(AppError.Content(ContentError.CATALOGUE_UNAVAILABLE, cause = e))
    }
}

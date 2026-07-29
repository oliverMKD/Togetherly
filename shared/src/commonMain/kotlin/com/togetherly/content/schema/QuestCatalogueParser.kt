package com.togetherly.content.schema

import com.togetherly.content.model.QuestCatalogueDto
import com.togetherly.core.result.DataResult

/**
 * A successful parse only proves the JSON is well-formed and schema-shaped — it says nothing
 * about business-rule validity, which this boundary deliberately does not check.
 */
internal interface QuestCatalogueParser {
    fun parse(
        json: String,
    ): DataResult<QuestCatalogueDto>
}

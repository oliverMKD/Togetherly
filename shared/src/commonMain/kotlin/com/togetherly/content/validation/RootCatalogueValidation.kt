package com.togetherly.content.validation

import com.togetherly.content.model.QuestCatalogueDto
import com.togetherly.content.schema.QuestCatalogueSchema

/**
 * Format-only check (e.g. "en", "en-US") — there is no maintained list of supported locales yet,
 * only a schema shape to enforce.
 */
private val LOCALE_FORMAT = Regex("^[a-z]{2}(-[A-Z]{2})?$")

internal fun validateRoot(
    dto: QuestCatalogueDto,
    issues: MutableList<CatalogueValidationIssue>,
) {
    if (dto.schemaVersion != QuestCatalogueSchema.CURRENT_SCHEMA_VERSION) {
        issues += errorIssue("schemaVersion", CatalogueIssueCode.UNSUPPORTED_SCHEMA_VERSION, dto.schemaVersion.toString())
    }

    if (dto.catalogueVersion <= 0) {
        issues += errorIssue("catalogueVersion", CatalogueIssueCode.NON_POSITIVE_VALUE, dto.catalogueVersion.toString())
    }

    if (!LOCALE_FORMAT.matches(dto.locale)) {
        issues += errorIssue("locale", CatalogueIssueCode.UNSUPPORTED_LOCALE, dto.locale)
    }

    if (dto.packs.isEmpty()) {
        issues += errorIssue("packs", CatalogueIssueCode.EMPTY_COLLECTION)
    }
    if (dto.quests.isEmpty()) {
        issues += errorIssue("quests", CatalogueIssueCode.EMPTY_COLLECTION)
    }

    findDuplicates(dto.packs.map { it.id }).forEach { duplicateId ->
        issues += errorIssue("packs", CatalogueIssueCode.DUPLICATE_VALUE, duplicateId)
    }
    findDuplicates(dto.quests.map { it.id }).forEach { duplicateId ->
        issues += errorIssue("quests", CatalogueIssueCode.DUPLICATE_VALUE, duplicateId)
    }
}

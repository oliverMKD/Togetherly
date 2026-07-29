package com.togetherly.content.validation

import com.togetherly.content.mapper.ContentMappingResult
import com.togetherly.content.mapper.mapQuestCategory
import com.togetherly.content.model.QuestPackDto
import com.togetherly.domain.quest.ArtworkKey
import com.togetherly.domain.quest.QuestSummary
import com.togetherly.domain.quest.QuestTitle
import com.togetherly.domain.validation.MAX_DOMAIN_ID_LENGTH

internal fun validatePack(
    dto: QuestPackDto,
    path: String,
    issues: MutableList<CatalogueValidationIssue>,
) {
    requireNonBlank(dto.id, "$path.id", issues)
    requireMaxLength(dto.id, MAX_DOMAIN_ID_LENGTH, "$path.id", issues)

    if (dto.version <= 0) {
        issues += errorIssue("$path.version", CatalogueIssueCode.NON_POSITIVE_VALUE, dto.version.toString())
    }

    requireNonBlank(dto.title, "$path.title", issues)
    requireMaxLength(dto.title, QuestTitle.MAX_LENGTH, "$path.title", issues)

    requireNonBlank(dto.description, "$path.description", issues)
    requireMaxLength(dto.description, QuestSummary.MAX_LENGTH, "$path.description", issues)

    dto.category?.let { raw ->
        if (mapQuestCategory(raw, "$path.category") is ContentMappingResult.Failure) {
            issues += errorIssue("$path.category", CatalogueIssueCode.UNSUPPORTED_ENUM_VALUE, raw)
        }
    }

    validateAccess(dto.access, "$path.access", issues)

    if (dto.questIds.isEmpty()) {
        issues += errorIssue("$path.questIds", CatalogueIssueCode.EMPTY_COLLECTION)
    }
    findDuplicates(dto.questIds).forEach { duplicate ->
        issues += errorIssue("$path.questIds", CatalogueIssueCode.DUPLICATE_VALUE, duplicate)
    }

    requireNonBlank(dto.artworkKey, "$path.artworkKey", issues)
    requireMaxLength(dto.artworkKey, ArtworkKey.MAX_LENGTH, "$path.artworkKey", issues)

    if (dto.sortOrder < 0) {
        issues += errorIssue("$path.sortOrder", CatalogueIssueCode.NON_POSITIVE_VALUE, dto.sortOrder.toString())
    }
}

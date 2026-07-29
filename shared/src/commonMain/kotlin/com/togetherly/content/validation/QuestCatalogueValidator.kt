package com.togetherly.content.validation

import com.togetherly.content.model.QuestCatalogueDto

/**
 * Collects every detectable problem in one pass rather than failing on the first — content
 * authors get a complete report. Issue order is deterministic and always: root issues, pack
 * issues in source order, quest issues in source order, cross-reference issues, then warnings.
 */
internal class QuestCatalogueValidator {

    fun validate(catalogue: QuestCatalogueDto): CatalogueValidationResult {
        val issues = mutableListOf<CatalogueValidationIssue>()

        validateRoot(catalogue, issues)
        catalogue.packs.forEachIndexed { index, pack -> validatePack(pack, "packs[$index]", issues) }
        catalogue.quests.forEachIndexed { index, quest -> validateQuest(quest, "quests[$index]", issues) }
        validateCrossReferences(catalogue, issues)
        validateContentQualityWarnings(catalogue, issues)

        val hasError = issues.any { it.severity == CatalogueIssueSeverity.ERROR }
        return if (hasError) {
            CatalogueValidationResult.Invalid(issues)
        } else {
            CatalogueValidationResult.Valid(warnings = issues)
        }
    }
}

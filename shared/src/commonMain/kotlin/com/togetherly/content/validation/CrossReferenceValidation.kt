package com.togetherly.content.validation

import com.togetherly.content.model.QuestCatalogueDto

/**
 * Chosen convention: a pack's artwork key must start with "packs/" — matching what every
 * fixture and mapper test in this codebase already uses. There is no broader asset-naming
 * scheme defined yet beyond this prefix.
 */
private const val ARTWORK_KEY_PREFIX = "packs/"

/**
 * The MVP enforces exactly one pack per quest even though the domain's [com.togetherly.domain.quest.FamilyQuest]
 * itself has no structural limit on how many packs could reference it — [com.togetherly.content.model.FamilyQuestDto.packId]
 * is a single value by design, and this validator additionally requires every pack's
 * [com.togetherly.content.model.QuestPackDto.questIds] to agree with it.
 */
internal fun validateCrossReferences(
    dto: QuestCatalogueDto,
    issues: MutableList<CatalogueValidationIssue>,
) {
    val packsById = dto.packs.associateBy { it.id }
    val questsById = dto.quests.associateBy { it.id }

    dto.quests.forEachIndexed { index, quest ->
        if (quest.packId !in packsById) {
            issues += errorIssue("quests[$index].packId", CatalogueIssueCode.REFERENCE_NOT_FOUND, quest.packId)
        }
    }

    dto.packs.forEachIndexed { packIndex, pack ->
        val packPath = "packs[$packIndex]"
        var resolvedQuestCount = 0

        pack.questIds.forEachIndexed { questIndex, questId ->
            val quest = questsById[questId]
            if (quest == null) {
                issues += errorIssue("$packPath.questIds[$questIndex]", CatalogueIssueCode.REFERENCE_NOT_FOUND, questId)
                return@forEachIndexed
            }
            resolvedQuestCount++

            if (quest.packId != pack.id) {
                issues += errorIssue("$packPath.questIds[$questIndex]", CatalogueIssueCode.REFERENCE_MISMATCH, questId)
            }
            if (pack.access.type == "free" && quest.access.type == "premium") {
                issues += errorIssue("$packPath.questIds[$questIndex]", CatalogueIssueCode.CONTRADICTORY_STATE, questId)
            }
            if (pack.category != null && quest.category != pack.category) {
                issues += errorIssue("$packPath.questIds[$questIndex]", CatalogueIssueCode.CONTRADICTORY_STATE, quest.category)
            }
        }

        if (resolvedQuestCount == 0) {
            issues += errorIssue(packPath, CatalogueIssueCode.EMPTY_COLLECTION)
        }

        if (pack.artworkKey.isNotBlank() && !pack.artworkKey.startsWith(ARTWORK_KEY_PREFIX)) {
            issues += errorIssue("$packPath.artworkKey", CatalogueIssueCode.NAMING_CONVENTION_VIOLATION, pack.artworkKey)
        }
    }

    val packCountByQuestId = dto.packs.flatMap { it.questIds }.groupingBy { it }.eachCount()
    dto.quests.forEachIndexed { index, quest ->
        when (packCountByQuestId[quest.id] ?: 0) {
            0 -> issues += errorIssue("quests[$index]", CatalogueIssueCode.REFERENCE_NOT_FOUND, quest.id)
            1 -> Unit
            else -> issues += errorIssue("quests[$index]", CatalogueIssueCode.REFERENCE_MISMATCH, quest.id)
        }
    }
}

package com.togetherly.content.validation

import com.togetherly.content.model.QuestCatalogueDto
import com.togetherly.domain.quest.QuestSummary
import com.togetherly.domain.quest.QuestTitle

/** 75% of the domain's hard maximum — warns well before content risks the actual limit. */
private val UNUSUALLY_LONG_TITLE_THRESHOLD = (QuestTitle.MAX_LENGTH * 0.75).toInt()
private val UNUSUALLY_LONG_SUMMARY_THRESHOLD = (QuestSummary.MAX_LENGTH * 0.75).toInt()
private const val UNBALANCED_CATEGORY_SHARE_THRESHOLD = 0.7

/**
 * Suspicious-but-valid content only. Deliberately does not warn about a quest's instructions
 * "suggesting timed behavior" without a timer — there is no structural signal for that (no
 * `expectsTimer` field or similar), and inferring it from title/summary/instruction text would
 * be natural-language analysis, which this validator does not perform.
 *
 * Duplicate pack [com.togetherly.content.model.QuestPackDto.sortOrder] values are treated as a
 * warning, not an error: a tie is a legitimate (if imprecise) authoring choice with a
 * well-defined fallback (stable sort preserves source order) — it doesn't corrupt catalogue
 * structure the way a duplicate ID would.
 */
internal fun validateContentQualityWarnings(
    dto: QuestCatalogueDto,
    issues: MutableList<CatalogueValidationIssue>,
) {
    dto.quests.forEachIndexed { index, quest ->
        val path = "quests[$index]"
        if (quest.title.length > UNUSUALLY_LONG_TITLE_THRESHOLD) {
            issues += warningIssue("$path.title", CatalogueIssueCode.SUSPICIOUS_CONTENT, quest.title)
        }
        if (quest.summary.length > UNUSUALLY_LONG_SUMMARY_THRESHOLD) {
            issues += warningIssue("$path.summary", CatalogueIssueCode.SUSPICIOUS_CONTENT, quest.summary)
        }
        if (quest.hints.isEmpty()) {
            issues += warningIssue("$path.hints", CatalogueIssueCode.SUSPICIOUS_CONTENT)
        }
        if (quest.cooldownDays == 0) {
            issues += warningIssue("$path.cooldownDays", CatalogueIssueCode.SUSPICIOUS_CONTENT, "0")
        }
    }

    dto.packs.forEachIndexed { index, pack ->
        if (pack.questIds.size == 1) {
            issues += warningIssue("packs[$index]", CatalogueIssueCode.SUSPICIOUS_CONTENT, "single quest")
        }
    }

    findDuplicates(dto.packs.map { it.sortOrder }).forEach { duplicateSortOrder ->
        issues += warningIssue("packs", CatalogueIssueCode.DUPLICATE_VALUE, duplicateSortOrder.toString())
    }

    if (dto.quests.isNotEmpty()) {
        val categoryCounts = dto.quests.groupingBy { it.category }.eachCount()
        if (categoryCounts.size == 1) {
            issues += warningIssue(
                "quests",
                CatalogueIssueCode.SUSPICIOUS_CONTENT,
                "uniform category: ${categoryCounts.keys.single()}",
            )
        } else {
            val dominantShare = categoryCounts.values.max().toDouble() / dto.quests.size
            if (dominantShare >= UNBALANCED_CATEGORY_SHARE_THRESHOLD) {
                issues += warningIssue("quests", CatalogueIssueCode.SUSPICIOUS_CONTENT, "unbalanced category distribution")
            }
        }
    }
}

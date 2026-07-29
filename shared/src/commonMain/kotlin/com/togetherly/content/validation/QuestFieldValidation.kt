package com.togetherly.content.validation

import com.togetherly.content.mapper.ContentMappingResult
import com.togetherly.content.mapper.mapAgeBand
import com.togetherly.content.mapper.mapEnergyLevel
import com.togetherly.content.mapper.mapPreparationLevel
import com.togetherly.content.mapper.mapQuestCategory
import com.togetherly.content.mapper.mapQuestLocation
import com.togetherly.content.model.FamilyQuestDto
import com.togetherly.content.model.QuestAccessDto
import com.togetherly.domain.completion.CompletionPrompt
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.InstructionText
import com.togetherly.domain.quest.MaterialName
import com.togetherly.domain.quest.QuestSummary
import com.togetherly.domain.quest.QuestTimer
import com.togetherly.domain.quest.QuestTitle
import com.togetherly.domain.quest.SafetyNote
import com.togetherly.domain.validation.MAX_DOMAIN_ID_LENGTH
import kotlin.time.Duration.Companion.seconds

/**
 * Every rule here runs before domain mapping and reuses the same constants
 * [com.togetherly.domain.quest.FamilyQuest] and its value objects already enforce
 * ([QuestTitle.MAX_LENGTH], [FamilyQuest.MAX_DURATION_MINUTES], etc.) rather than
 * re-declaring them.
 */
internal fun validateQuest(
    dto: FamilyQuestDto,
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

    requireNonBlank(dto.summary, "$path.summary", issues)
    requireMaxLength(dto.summary, QuestSummary.MAX_LENGTH, "$path.summary", issues)

    if (dto.instructions.isEmpty()) {
        issues += errorIssue("$path.instructions", CatalogueIssueCode.EMPTY_COLLECTION)
    } else {
        validateInstructionOrder(dto.instructions.map { it.order }, "$path.instructions", issues)
        dto.instructions.forEachIndexed { index, step ->
            requireNonBlank(step.text, "$path.instructions[$index].text", issues)
            requireMaxLength(step.text, InstructionText.MAX_LENGTH, "$path.instructions[$index].text", issues)
        }
    }

    if (mapQuestCategory(dto.category, "$path.category") is ContentMappingResult.Failure) {
        issues += errorIssue("$path.category", CatalogueIssueCode.UNSUPPORTED_ENUM_VALUE, dto.category)
    }

    if (dto.ageBands.isEmpty()) {
        issues += errorIssue("$path.ageBands", CatalogueIssueCode.EMPTY_COLLECTION)
    }
    findDuplicates(dto.ageBands).forEach { duplicate ->
        issues += errorIssue("$path.ageBands", CatalogueIssueCode.DUPLICATE_VALUE, duplicate)
    }
    dto.ageBands.forEachIndexed { index, raw ->
        if (mapAgeBand(raw, "$path.ageBands[$index]") is ContentMappingResult.Failure) {
            issues += errorIssue("$path.ageBands[$index]", CatalogueIssueCode.UNSUPPORTED_ENUM_VALUE, raw)
        }
    }

    if (dto.durationMinutes <= 0) {
        issues += errorIssue("$path.durationMinutes", CatalogueIssueCode.NON_POSITIVE_VALUE, dto.durationMinutes.toString())
    } else if (dto.durationMinutes > FamilyQuest.MAX_DURATION_MINUTES) {
        issues += errorIssue("$path.durationMinutes", CatalogueIssueCode.VALUE_TOO_LONG, dto.durationMinutes.toString())
    }

    if (mapQuestLocation(dto.location, "$path.location") is ContentMappingResult.Failure) {
        issues += errorIssue("$path.location", CatalogueIssueCode.UNSUPPORTED_ENUM_VALUE, dto.location)
    }
    if (mapPreparationLevel(dto.preparation, "$path.preparation") is ContentMappingResult.Failure) {
        issues += errorIssue("$path.preparation", CatalogueIssueCode.UNSUPPORTED_ENUM_VALUE, dto.preparation)
    }
    if (mapEnergyLevel(dto.energy, "$path.energy") is ContentMappingResult.Failure) {
        issues += errorIssue("$path.energy", CatalogueIssueCode.UNSUPPORTED_ENUM_VALUE, dto.energy)
    }

    findDuplicates(dto.materials).forEach { duplicate ->
        issues += errorIssue("$path.materials", CatalogueIssueCode.DUPLICATE_VALUE, duplicate)
    }
    dto.materials.forEachIndexed { index, raw ->
        requireNonBlank(raw, "$path.materials[$index]", issues)
        requireMaxLength(raw, MaterialName.MAX_LENGTH, "$path.materials[$index]", issues)
    }

    findDuplicates(dto.hints).forEach { duplicate ->
        issues += errorIssue("$path.hints", CatalogueIssueCode.DUPLICATE_VALUE, duplicate)
    }
    dto.hints.forEachIndexed { index, raw ->
        requireNonBlank(raw, "$path.hints[$index]", issues)
        requireMaxLength(raw, InstructionText.MAX_LENGTH, "$path.hints[$index]", issues)
    }

    requireNonBlank(dto.completionPrompt, "$path.completionPrompt", issues)
    requireMaxLength(dto.completionPrompt, CompletionPrompt.MAX_LENGTH, "$path.completionPrompt", issues)

    dto.safetyNote?.let { note ->
        requireNonBlank(note, "$path.safetyNote", issues)
        requireMaxLength(note, SafetyNote.MAX_LENGTH, "$path.safetyNote", issues)
    }

    requireNonBlank(dto.packId, "$path.packId", issues)
    requireMaxLength(dto.packId, MAX_DOMAIN_ID_LENGTH, "$path.packId", issues)

    validateAccess(dto.access, "$path.access", issues)

    dto.timer?.let { timer ->
        if (timer.durationSeconds <= 0) {
            issues += errorIssue(
                "$path.timer.durationSeconds",
                CatalogueIssueCode.NON_POSITIVE_VALUE,
                timer.durationSeconds.toString(),
            )
        } else if (timer.durationSeconds.seconds > QuestTimer.MAX_DURATION) {
            issues += errorIssue(
                "$path.timer.durationSeconds",
                CatalogueIssueCode.VALUE_TOO_LONG,
                timer.durationSeconds.toString(),
            )
        }
    }

    if (dto.cooldownDays < 0) {
        issues += errorIssue("$path.cooldownDays", CatalogueIssueCode.NON_POSITIVE_VALUE, dto.cooldownDays.toString())
    }
}

internal fun validateInstructionOrder(
    orders: List<Int>,
    path: String,
    issues: MutableList<CatalogueValidationIssue>,
) {
    findDuplicates(orders).forEach { duplicate ->
        issues += errorIssue(path, CatalogueIssueCode.DUPLICATE_VALUE, duplicate.toString())
    }
    val expected = (1..orders.size).toList()
    if (orders.sorted() != expected) {
        issues += errorIssue(path, CatalogueIssueCode.INVALID_ORDER)
    }
}

internal fun validateAccess(
    access: QuestAccessDto,
    path: String,
    issues: MutableList<CatalogueValidationIssue>,
) {
    when (access.type) {
        "free" -> if (access.entitlementId != null) {
            issues += errorIssue("$path.entitlementId", CatalogueIssueCode.INVALID_ACCESS_COMBINATION, access.entitlementId)
        }

        "premium" -> if (access.entitlementId.isNullOrBlank()) {
            issues += errorIssue("$path.entitlementId", CatalogueIssueCode.INVALID_ACCESS_COMBINATION)
        }

        else -> issues += errorIssue("$path.type", CatalogueIssueCode.UNSUPPORTED_ENUM_VALUE, access.type)
    }
}

package com.togetherly.content.mapper

import com.togetherly.content.model.FamilyQuestDto
import com.togetherly.domain.completion.CompletionPrompt
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.InstructionStep
import com.togetherly.domain.quest.InstructionText
import com.togetherly.domain.quest.MaterialName
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.domain.quest.QuestSummary
import com.togetherly.domain.quest.QuestTitle
import com.togetherly.domain.quest.SafetyNote

internal interface FamilyQuestMapper {
    fun map(
        dto: FamilyQuestDto,
        path: String,
    ): ContentMappingResult<FamilyQuest>
}

/**
 * Performs no cross-reference validation — it does not check that [FamilyQuestDto.packId]
 * actually exists among the catalogue's packs. That belongs to full catalogue validation later.
 */
internal class DefaultFamilyQuestMapper : FamilyQuestMapper {

    override fun map(dto: FamilyQuestDto, path: String): ContentMappingResult<FamilyQuest> {
        val category = mapQuestCategory(dto.category, "$path.category")
            .getOrElse { return ContentMappingResult.Failure(it) }

        val ageBands = mutableSetOf<AgeBand>()
        for ((index, raw) in dto.ageBands.withIndex()) {
            ageBands += mapAgeBand(raw, "$path.ageBands[$index]")
                .getOrElse { return ContentMappingResult.Failure(it) }
        }

        val location = mapQuestLocation(dto.location, "$path.location")
            .getOrElse { return ContentMappingResult.Failure(it) }
        val preparation = mapPreparationLevel(dto.preparation, "$path.preparation")
            .getOrElse { return ContentMappingResult.Failure(it) }
        val energy = mapEnergyLevel(dto.energy, "$path.energy")
            .getOrElse { return ContentMappingResult.Failure(it) }
        val access = mapQuestAccess(dto.access, "$path.access")
            .getOrElse { return ContentMappingResult.Failure(it) }
        val timer = mapQuestTimer(dto.timer, "$path.timer")
            .getOrElse { return ContentMappingResult.Failure(it) }

        val instructions = mutableListOf<InstructionStep>()
        for ((index, stepDto) in dto.instructions.withIndex()) {
            instructions += mapCatching("$path.instructions[$index]", stepDto.text) {
                InstructionStep(order = stepDto.order, text = InstructionText(stepDto.text))
            }.getOrElse { return ContentMappingResult.Failure(it) }
        }

        val materials = mutableListOf<MaterialName>()
        for ((index, raw) in dto.materials.withIndex()) {
            materials += mapCatching("$path.materials[$index]", raw) { MaterialName(raw) }
                .getOrElse { return ContentMappingResult.Failure(it) }
        }

        val hints = mutableListOf<InstructionText>()
        for ((index, raw) in dto.hints.withIndex()) {
            hints += mapCatching("$path.hints[$index]", raw) { InstructionText(raw) }
                .getOrElse { return ContentMappingResult.Failure(it) }
        }

        return mapCatching(path, dto.id) {
            FamilyQuest(
                id = QuestId(dto.id),
                version = dto.version,
                title = QuestTitle(dto.title),
                summary = QuestSummary(dto.summary),
                instructions = instructions,
                category = category,
                ageBands = ageBands,
                durationMinutes = dto.durationMinutes,
                location = location,
                preparation = preparation,
                energy = energy,
                materials = materials,
                hints = hints,
                completionPrompt = CompletionPrompt(dto.completionPrompt),
                safetyNote = dto.safetyNote?.let { SafetyNote(it) },
                packId = QuestPackId(dto.packId),
                access = access,
                timer = timer,
                cooldownDays = dto.cooldownDays,
            )
        }
    }
}

package com.togetherly.domain.quest

import com.togetherly.domain.completion.CompletionPrompt
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.validation.DomainValidationException
import com.togetherly.domain.validation.DomainValidationReason
import com.togetherly.domain.validation.requireAtMost
import com.togetherly.domain.validation.requireNonNegative
import com.togetherly.domain.validation.requireNotEmpty
import com.togetherly.domain.validation.requirePositive
import com.togetherly.domain.validation.requireUniqueValues

data class FamilyQuest(
    val id: QuestId,
    val version: Int,
    val title: QuestTitle,
    val summary: QuestSummary,
    val instructions: List<InstructionStep>,
    val category: QuestCategory,
    val ageBands: Set<AgeBand>,
    val durationMinutes: Int,
    val location: QuestLocation,
    val preparation: PreparationLevel,
    val energy: EnergyLevel,
    val materials: List<MaterialName>,
    val hints: List<InstructionText>,
    val completionPrompt: CompletionPrompt,
    val safetyNote: SafetyNote?,
    val packId: QuestPackId,
    val access: QuestAccess,
    val timer: QuestTimer?,
    val cooldownDays: Int,
) {
    init {
        requirePositive(version)
        requireNotEmpty(instructions)
        requireContinuousInstructionOrder(instructions)
        requireNotEmpty(ageBands)
        requirePositive(durationMinutes)
        requireAtMost(durationMinutes, MAX_DURATION_MINUTES)
        requireUniqueValues(materials)
        requireUniqueValues(hints)
        requireNonNegative(cooldownDays)
    }

    companion object {
        const val MAX_DURATION_MINUTES = 180
    }
}

private fun requireContinuousInstructionOrder(instructions: List<InstructionStep>) {
    val orders = instructions.map { it.order }
    requireUniqueValues(orders)
    if (orders.sorted() != (1..orders.size).toList()) {
        throw DomainValidationException(DomainValidationReason.INVALID_ORDER)
    }
}

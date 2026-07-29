package com.togetherly.content.model

import kotlinx.serialization.Serializable

@Serializable
internal data class FamilyQuestDto(
    val id: String,
    val version: Int,
    val title: String,
    val summary: String,
    val instructions: List<InstructionStepDto>,
    val category: String,
    val ageBands: List<String>,
    val durationMinutes: Int,
    val location: String,
    val preparation: String,
    val energy: String,
    val materials: List<String> = emptyList(),
    val hints: List<String> = emptyList(),
    val completionPrompt: String,
    val safetyNote: String? = null,
    val packId: String,
    val access: QuestAccessDto,
    val timer: QuestTimerDto? = null,
    val cooldownDays: Int = 30,
)

package com.togetherly.domain.quest

import com.togetherly.domain.completion.CompletionPrompt
import com.togetherly.domain.family.AgeBand

internal fun validFamilyQuest(
    id: QuestId = QuestId("quest-1"),
    version: Int = 1,
    title: QuestTitle = QuestTitle("Backyard Scavenger Hunt"),
    summary: QuestSummary = QuestSummary("Find five hidden treasures together."),
    instructions: List<InstructionStep> = listOf(
        InstructionStep(1, InstructionText("Hide five small objects in the yard.")),
        InstructionStep(2, InstructionText("Give clues to find each one.")),
    ),
    category: QuestCategory = QuestCategory.DISCOVER,
    ageBands: Set<AgeBand> = setOf(AgeBand.AGE_6_TO_8),
    durationMinutes: Int = 20,
    location: QuestLocation = QuestLocation.OUTDOOR,
    preparation: PreparationLevel = PreparationLevel.SIMPLE_MATERIALS,
    energy: EnergyLevel = EnergyLevel.MODERATE,
    materials: List<MaterialName> = listOf(MaterialName("Small toys")),
    hints: List<InstructionText> = listOf(InstructionText("Try the garden.")),
    completionPrompt: CompletionPrompt = CompletionPrompt("Share what you found!"),
    safetyNote: SafetyNote? = null,
    packId: QuestPackId = QuestPackId("pack-1"),
    access: QuestAccess = QuestAccess.Free,
    timer: QuestTimer? = null,
    cooldownDays: Int = 0,
) = FamilyQuest(
    id = id,
    version = version,
    title = title,
    summary = summary,
    instructions = instructions,
    category = category,
    ageBands = ageBands,
    durationMinutes = durationMinutes,
    location = location,
    preparation = preparation,
    energy = energy,
    materials = materials,
    hints = hints,
    completionPrompt = completionPrompt,
    safetyNote = safetyNote,
    packId = packId,
    access = access,
    timer = timer,
    cooldownDays = cooldownDays,
)

internal fun validQuestPack(
    id: QuestPackId = QuestPackId("pack-1"),
    title: QuestTitle = QuestTitle("Outdoor Adventures"),
    description: QuestSummary = QuestSummary("A pack of outdoor quests."),
    category: QuestCategory? = QuestCategory.DISCOVER,
    access: QuestAccess = QuestAccess.Free,
    questIds: List<QuestId> = listOf(QuestId("quest-1"), QuestId("quest-2")),
    artworkKey: ArtworkKey = ArtworkKey("packs/outdoor-adventures"),
    sortOrder: Int = 0,
) = QuestPack(
    id = id,
    title = title,
    description = description,
    category = category,
    access = access,
    questIds = questIds,
    artworkKey = artworkKey,
    sortOrder = sortOrder,
)

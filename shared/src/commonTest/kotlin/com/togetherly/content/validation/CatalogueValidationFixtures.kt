package com.togetherly.content.validation

import com.togetherly.content.model.FamilyQuestDto
import com.togetherly.content.model.InstructionStepDto
import com.togetherly.content.model.QuestAccessDto
import com.togetherly.content.model.QuestCatalogueDto
import com.togetherly.content.model.QuestPackDto
import com.togetherly.content.schema.QuestCatalogueSchema

internal fun validQuestDto(
    id: String = "quest-1",
    packId: String = "pack-1",
    category: String = "discover",
    access: QuestAccessDto = QuestAccessDto(type = "free"),
    version: Int = 1,
    instructions: List<InstructionStepDto> = listOf(InstructionStepDto(order = 1, text = "Hide five small objects.")),
) = FamilyQuestDto(
    id = id,
    version = version,
    title = "Backyard Scavenger Hunt",
    summary = "Find five hidden treasures together.",
    instructions = instructions,
    category = category,
    ageBands = listOf("6-8"),
    durationMinutes = 20,
    location = "outdoor",
    preparation = "simple-materials",
    energy = "moderate",
    completionPrompt = "Share what you found!",
    packId = packId,
    access = access,
)

internal fun validPackDto(
    id: String = "pack-1",
    category: String? = "discover",
    access: QuestAccessDto = QuestAccessDto(type = "free"),
    questIds: List<String> = listOf("quest-1"),
) = QuestPackDto(
    id = id,
    version = 1,
    title = "Backyard Adventures",
    description = "A pack of outdoor quests.",
    category = category,
    access = access,
    questIds = questIds,
    artworkKey = "packs/backyard-adventures",
    sortOrder = 0,
)

internal fun validCatalogueDto(
    packs: List<QuestPackDto> = listOf(validPackDto()),
    quests: List<FamilyQuestDto> = listOf(validQuestDto()),
    schemaVersion: Int = QuestCatalogueSchema.CURRENT_SCHEMA_VERSION,
    catalogueVersion: Int = 1,
    locale: String = QuestCatalogueSchema.DEFAULT_LOCALE,
) = QuestCatalogueDto(
    schemaVersion = schemaVersion,
    catalogueVersion = catalogueVersion,
    locale = locale,
    packs = packs,
    quests = quests,
)

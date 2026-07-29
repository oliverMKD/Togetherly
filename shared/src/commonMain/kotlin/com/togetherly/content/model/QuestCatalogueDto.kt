package com.togetherly.content.model

import kotlinx.serialization.Serializable

@Serializable
internal data class QuestCatalogueDto(
    val schemaVersion: Int,
    val catalogueVersion: Int,
    val locale: String,
    val packs: List<QuestPackDto>,
    val quests: List<FamilyQuestDto>,
)

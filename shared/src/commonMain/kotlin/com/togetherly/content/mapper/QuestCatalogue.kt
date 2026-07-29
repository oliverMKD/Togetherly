package com.togetherly.content.mapper

import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestPack

/**
 * A content/data-boundary aggregate, not a domain model — the domain layer has no notion of "a
 * catalogue"; [com.togetherly.domain.quest.repository.QuestRepository] exposes packs and quests
 * independently.
 */
internal data class QuestCatalogue(
    val schemaVersion: Int,
    val catalogueVersion: Int,
    val locale: String,
    val packs: List<QuestPack>,
    val quests: List<FamilyQuest>,
)

package com.togetherly.content.repository

import com.togetherly.content.mapper.QuestCatalogue
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPack
import com.togetherly.domain.quest.QuestPackId

/**
 * A one-time-computed, lookup-friendly view over a loaded [QuestCatalogue]. Built once per
 * successful load and cached alongside it (see [BundledQuestRepository]) — a single-quest or
 * single-pack lookup never linearly scans [quests]/[packs], and [packs] is pre-sorted by
 * [QuestPack.sortOrder] once rather than on every `getPacks()`/`observePacks()` call.
 */
internal data class IndexedQuestCatalogue(
    val quests: List<FamilyQuest>,
    val questsById: Map<QuestId, FamilyQuest>,
    val packs: List<QuestPack>,
    val packsById: Map<QuestPackId, QuestPack>,
)

internal fun QuestCatalogue.indexed(): IndexedQuestCatalogue = IndexedQuestCatalogue(
    quests = quests,
    questsById = quests.associateBy { it.id },
    packs = packs.sortedBy(QuestPack::sortOrder),
    packsById = packs.associateBy { it.id },
)

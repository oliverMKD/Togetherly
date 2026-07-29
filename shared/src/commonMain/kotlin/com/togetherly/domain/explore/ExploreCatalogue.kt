package com.togetherly.domain.explore

import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestPack

/**
 * The raw, unfiltered pairing [ObserveExploreCatalogueUseCase][com.togetherly.domain.explore.usecase.ObserveExploreCatalogueUseCase]
 * emits — search/filter/access are applied on top of this by separate, focused use cases, never
 * baked into the observation itself. Not a duplicate of [com.togetherly.content.mapper.QuestCatalogue]:
 * that type is an `internal` content/data-boundary aggregate the domain layer never sees; this one
 * is the small, provider-neutral pairing Explore's own presentation layer actually depends on.
 */
data class ExploreCatalogue(
    val quests: List<FamilyQuest>,
    val packs: List<QuestPack>,
)

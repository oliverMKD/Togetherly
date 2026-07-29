package com.togetherly.domain.explore

import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestPack

/**
 * [pack.questIds][QuestPack.questIds] resolved into the actual [FamilyQuest]s that currently exist
 * in the catalogue — [com.togetherly.domain.explore.usecase.GetQuestPackUseCase]'s own result. A
 * quest id the pack references that no longer resolves (a removed/renamed quest) is silently
 * skipped rather than surfaced as an error: a pack detail screen showing N-1 of N quests is far
 * better than the whole screen failing to load over one stale reference.
 */
data class QuestPackDetail(
    val pack: QuestPack,
    val quests: List<FamilyQuest>,
)

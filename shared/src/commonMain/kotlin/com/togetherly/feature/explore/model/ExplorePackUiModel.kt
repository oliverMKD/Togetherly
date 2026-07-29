package com.togetherly.feature.explore.model

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.feature.today.model.QuestCategoryUi

/**
 * What a pack card actually renders — never a raw [com.togetherly.domain.quest.QuestPack]. [theme]
 * is a purely decorative color token, never a claim about the pack's real content category: most
 * bundled packs mix categories on purpose (see the catalogue's own pack definitions), so [theme] is
 * [com.togetherly.domain.quest.QuestPack.category] when the pack genuinely has one, otherwise a
 * value deterministically derived from [id] (see [com.togetherly.feature.explore.mapper.toExplorePackUi])
 * so the same pack always renders with the same visual theme rather than a random one.
 *
 * [locked] communicates access, not disablement — [com.togetherly.feature.explore.presentation.ExplorePackCard]
 * renders a locked pack with the same full-color, fully legible treatment as an unlocked one; only
 * a small badge differs (see this feature's own task spec: "The lock should communicate access
 * without making the content look disabled or unimportant").
 */
@Immutable
data class ExplorePackUiModel(
    val id: QuestPackId,
    val title: String,
    val description: String,
    val questCount: Int,
    val durationLabel: UiText?,
    val isPremium: Boolean,
    val locked: Boolean,
    val theme: QuestCategoryUi,
)

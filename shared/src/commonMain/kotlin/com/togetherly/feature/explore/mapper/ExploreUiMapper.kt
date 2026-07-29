package com.togetherly.feature.explore.mapper

import com.togetherly.core.ui.UiText
import com.togetherly.domain.explore.QuestAccessFilter
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestPack
import com.togetherly.domain.recommendation.durationBandFor
import com.togetherly.feature.explore.model.ExplorePackUiModel
import com.togetherly.feature.explore.model.ExploreQuestUiModel
import com.togetherly.feature.today.mapper.label
import com.togetherly.feature.today.mapper.toUi
import com.togetherly.feature.today.model.QuestCategoryUi
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.explore_filter_access_all
import togetherly.shared.generated.resources.explore_pack_duration_range
import togetherly.shared.generated.resources.explore_pack_duration_single
import togetherly.shared.generated.resources.explore_pack_free
import togetherly.shared.generated.resources.explore_pack_premium

/**
 * Builds the immutable cards Explore actually renders — never hands a [FamilyQuest]/[QuestPack] to
 * a Composable. Reuses Today's own label/category mappers ([label], [toUi]) rather than duplicating
 * them a third time (Today and Quest Detail already share them) — see [ExploreQuestUiModel]'s own
 * KDoc for why the *card type* still isn't shared.
 */
fun FamilyQuest.toExploreQuestUi(isSaved: Boolean, locked: Boolean): ExploreQuestUiModel = ExploreQuestUiModel(
    id = id,
    title = title.value,
    summary = summary.value,
    category = category.toUi(),
    durationLabel = durationBandFor(durationMinutes).label(),
    energyLabel = energy.label(),
    locationLabel = location.label(),
    isSaved = isSaved,
    isPremium = access is QuestAccess.Premium,
    locked = locked,
)

/**
 * [memberQuests] should be the pack's own quests (resolved from [QuestPack.questIds]) — used only
 * to compute [ExplorePackUiModel.durationLabel]'s approximate range, never stored on the model
 * itself (a pack card shows a duration range, not a quest list).
 */
fun QuestPack.toExplorePackUi(locked: Boolean, memberQuests: List<FamilyQuest>): ExplorePackUiModel {
    val durations = memberQuests.map { it.durationMinutes }
    val durationLabel = when {
        durations.isEmpty() -> null
        durations.min() == durations.max() -> UiText.Resource(Res.string.explore_pack_duration_single, listOf(durations.min()))
        else -> UiText.Resource(Res.string.explore_pack_duration_range, listOf(durations.min(), durations.max()))
    }
    return ExplorePackUiModel(
        id = id,
        title = title.value,
        description = description.value,
        questCount = questIds.size,
        durationLabel = durationLabel,
        isPremium = access is QuestAccess.Premium,
        locked = locked,
        theme = category?.toUi() ?: themeFor(id.value),
    )
}

fun QuestAccessFilter.label(): UiText = UiText.Resource(
    when (this) {
        QuestAccessFilter.ALL -> Res.string.explore_filter_access_all
        QuestAccessFilter.FREE -> Res.string.explore_pack_free
        QuestAccessFilter.PREMIUM -> Res.string.explore_pack_premium
    },
)

fun QuestCategoryUi.toDomain(): QuestCategory = when (this) {
    QuestCategoryUi.TALK -> QuestCategory.TALK
    QuestCategoryUi.CREATE -> QuestCategory.CREATE
    QuestCategoryUi.MOVE -> QuestCategory.MOVE
    QuestCategoryUi.KINDNESS -> QuestCategory.KINDNESS
    QuestCategoryUi.DISCOVER -> QuestCategory.DISCOVER
    QuestCategoryUi.SILLY -> QuestCategory.SILLY
    QuestCategoryUi.MEMORIES -> QuestCategory.MEMORIES
}

/**
 * A pack whose own [QuestPack.category] is `null` (most bundled packs mix categories on purpose)
 * still needs *some* stable visual theme — deterministically derived from its id, rather than
 * random, so the same pack always renders with the same color across recompositions and app
 * launches instead of flickering between themes.
 */
private fun themeFor(packId: String): QuestCategoryUi {
    val entries = QuestCategoryUi.entries
    val index = packId.hashCode().mod(entries.size)
    return entries[index]
}

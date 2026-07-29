package com.togetherly.domain.explore

import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestPack

/**
 * A quest or pack the family isn't currently entitled to still appears here — [locked] is a
 * *presentation* flag, never a reason to exclude content from results (see [ExploreResult]'s own
 * KDoc: "access level should affect availability, not whether premium content exists in results").
 * [locked] is computed once (by [com.togetherly.domain.explore.usecase.EvaluateQuestAccessUseCase]/
 * [com.togetherly.domain.explore.usecase.EvaluatePackAccessUseCase], the same
 * [com.togetherly.domain.purchase.QuestAccessPolicy] reasoning [com.togetherly.feature.questdetail.presentation.QuestDetailViewModel]
 * already uses for its own `locked` field) rather than re-derived by every screen that reads it.
 */
data class ExploreQuestItem(
    val quest: FamilyQuest,
    val locked: Boolean,
)

data class ExplorePackItem(
    val pack: QuestPack,
    val locked: Boolean,
)

/**
 * The complete, provider-neutral shape a future Explore screen renders from — assembled by
 * composing [ExploreCatalogue] with [ExploreFilters]/a search query (via
 * [com.togetherly.domain.explore.usecase.SearchQuestsUseCase]/[com.togetherly.domain.explore.usecase.FilterQuestsUseCase])
 * and the family's current [FamilyAccess] (via [com.togetherly.domain.explore.usecase.EvaluateQuestAccessUseCase]/
 * [com.togetherly.domain.explore.usecase.EvaluatePackAccessUseCase]) — this type itself holds no
 * RevenueCat awareness; [access] is already the provider-neutral snapshot
 * [com.togetherly.domain.purchase.repository.EntitlementRepository] exposes.
 *
 * [isEmpty] reflects *results*, not the underlying catalogue — a catalogue with content but a
 * search/filter combination that matches nothing is exactly the "empty results" state a screen
 * needs to distinguish from "still loading" or "catalogue failed to load" (those stay
 * [com.togetherly.core.result.DataResult]-level concerns above this type, the same way
 * [com.togetherly.feature.questdetail.model.QuestDetailUiState] keeps `Loading`/`Error` as
 * sibling states rather than folding them into `Content`).
 */
data class ExploreResult(
    val packs: List<ExplorePackItem>,
    val quests: List<ExploreQuestItem>,
    val filters: ExploreFilters,
    val searchQuery: String,
    val access: FamilyAccess,
) {
    val isEmpty: Boolean
        get() = packs.isEmpty() && quests.isEmpty()
}

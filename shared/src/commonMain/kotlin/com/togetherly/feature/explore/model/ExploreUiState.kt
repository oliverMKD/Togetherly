package com.togetherly.feature.explore.model

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.feature.today.model.QuestCategoryUi
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/**
 * One flat, always-valid state rather than a sealed loading/content/error hierarchy — Explore has
 * too many states that legitimately coexist (a stale result list showing while a debounced search
 * recomputes, an inline error banner over an otherwise-populated screen) for a sealed hierarchy to
 * model without combinatorial duplication. [isLoading] only ever covers the *first* catalogue load
 * or an explicit [com.togetherly.feature.explore.presentation.ExploreAction.RetryClicked] — never a
 * routine search/category recompute, so typing never flashes a full-screen spinner (see
 * [com.togetherly.feature.explore.presentation.ExploreViewModel]'s own reasoning).
 *
 * [featuredPacks]/[packs]/[quests] double as both the normal curated-browse lists and, while
 * [isSearchActive], the query-matching result lists — [com.togetherly.feature.explore.presentation.ExploreScreen]
 * decides which section headings to show based on [isSearchActive], not a separate field.
 *
 * [activeFilterCount] (Step 12.4) is [com.togetherly.domain.explore.ExploreFilters.activeCount] —
 * every active dimension across the full filter screen, category included. [selectedCategory] is
 * still tracked as its own field for the quick top-row chip's selected state, but it and the filter
 * screen's own Category group always agree: both read/write [com.togetherly.feature.explore.presentation.ExploreFilterStore],
 * the single committed-filters source of truth.
 */
@Immutable
data class ExploreUiState(
    val isLoading: Boolean,
    val searchQuery: String,
    val selectedCategory: QuestCategoryUi?,
    val activeFilterCount: Int,
    val featuredPacks: PersistentList<ExplorePackUiModel>,
    val packs: PersistentList<ExplorePackUiModel>,
    val quests: PersistentList<ExploreQuestUiModel>,
    val access: FamilyAccess,
    val isSearchActive: Boolean,
    val emptyState: ExploreEmptyState?,
    val error: UiText?,
) {
    companion object {
        fun initial(): ExploreUiState = ExploreUiState(
            isLoading = true,
            searchQuery = "",
            selectedCategory = null,
            activeFilterCount = 0,
            featuredPacks = persistentListOf(),
            packs = persistentListOf(),
            quests = persistentListOf(),
            access = FamilyAccess.free(),
            isSearchActive = false,
            emptyState = null,
            error = null,
        )
    }
}

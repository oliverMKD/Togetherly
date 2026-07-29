package com.togetherly.feature.explore.model

/**
 * Which "nothing to show" reason is currently active, so [com.togetherly.feature.explore.presentation.ExploreScreen]
 * can render distinct, warm copy for each rather than one generic "no results" message. [SEARCH]
 * takes priority over [FILTER] whenever both a query and an active filter combine to zero results —
 * see [com.togetherly.feature.explore.presentation.ExploreViewModel]'s own reasoning for why.
 *
 * [FILTER] covers *any* of [com.togetherly.domain.explore.ExploreFilters]'s six dimensions being
 * active with zero matches — not category alone (Step 12.3 originally scoped this narrower, before
 * Step 12.4 wired the full filter sheet in).
 */
enum class ExploreEmptyState {
    SEARCH,
    FILTER,
}

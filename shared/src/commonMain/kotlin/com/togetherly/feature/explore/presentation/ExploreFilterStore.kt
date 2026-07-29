package com.togetherly.feature.explore.presentation

import com.togetherly.domain.explore.ExploreFilters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The single, in-memory source of truth for Explore's *committed* filters — a Koin singleton
 * (`single { ExploreFilterStore() }`), not a nav argument or a `SavedStateHandle` result. Both
 * [ExploreViewModel] (reads [filters] as part of its own combine pipeline) and
 * [ExploreFiltersViewModel] (reads the current value as its draft's starting point, writes back on
 * Apply) share this instance, so committed filters survive navigating to
 * [com.togetherly.navigation.destination.RootDestination.ExploreFilters] and back — Explore's own
 * `ExploreViewModel` instance is paused, not destroyed, by that navigation (it stays on
 * `MainShell`'s nested back stack), and even if it weren't, this store would still hold the answer.
 *
 * [commit] is the only way filters change — there is no "draft" here; the draft lives entirely in
 * [ExploreFiltersViewModel]'s own state until Apply calls [commit]. [clear] exists for "filters
 * reset when Explore state is intentionally cleared" — today the only thing that counts as
 * "intentional" is the filter screen's own Clear all + Apply flow (which calls [commit] with a
 * fresh [ExploreFilters]), since no broader app-wide state reset (sign-out, family switch) exists
 * yet to hook this into.
 */
class ExploreFilterStore {
    private val _filters = MutableStateFlow(ExploreFilters())
    val filters: StateFlow<ExploreFilters> = _filters.asStateFlow()

    fun commit(filters: ExploreFilters) {
        _filters.value = filters
    }

    fun clear() {
        _filters.value = ExploreFilters()
    }
}

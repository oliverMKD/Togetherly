package com.togetherly.feature.explore.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.telemetry.ExploreFiltered
import com.togetherly.core.telemetry.ProductAnalytics
import com.togetherly.domain.explore.ExploreFilters
import com.togetherly.feature.explore.model.ExploreFiltersUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds one local, immutable draft — [store]'s own committed [ExploreFilters] is only ever read
 * once, at construction, and only ever written by [onAction] handling [ExploreFiltersAction.ApplyClicked].
 * [ExploreFiltersAction.CancelClicked] (and the sheet/screen being dismissed any other way — back
 * gesture, system back) simply discards this whole ViewModel and its draft, since [store] was never
 * touched.
 */
class ExploreFiltersViewModel(
    private val store: ExploreFilterStore,
    private val analytics: ProductAnalytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreFiltersUiState(draft = store.filters.value))
    val uiState: StateFlow<ExploreFiltersUiState> = _uiState.asStateFlow()

    private val _events = Channel<ExploreFiltersEvent>(Channel.BUFFERED)
    val events: Flow<ExploreFiltersEvent> = _events.receiveAsFlow()

    fun onAction(action: ExploreFiltersAction) {
        when (action) {
            is ExploreFiltersAction.DurationChanged -> updateDraft { it.copy(duration = action.value) }
            is ExploreFiltersAction.EnergyChanged -> updateDraft { it.copy(energy = action.value) }
            is ExploreFiltersAction.LocationChanged -> updateDraft { it.copy(location = action.value) }
            is ExploreFiltersAction.AgeBandChanged -> updateDraft { it.copy(ageBand = action.value) }
            is ExploreFiltersAction.CategoryChanged -> updateDraft { it.copy(category = action.value) }
            is ExploreFiltersAction.AccessChanged -> updateDraft { it.copy(access = action.value) }
            ExploreFiltersAction.ClearAllClicked -> _uiState.update { it.copy(draft = ExploreFilters()) }
            ExploreFiltersAction.ApplyClicked -> {
                val draft = _uiState.value.draft
                store.commit(draft)
                analytics.capture(
                    ExploreFiltered(
                        category = draft.category,
                        durationBucket = draft.duration,
                        energyLevel = draft.energy,
                        location = draft.location,
                        accessFilter = draft.access,
                    ),
                )
                viewModelScope.launch { _events.send(ExploreFiltersEvent.NavigateBack) }
            }
            ExploreFiltersAction.CancelClicked -> viewModelScope.launch { _events.send(ExploreFiltersEvent.NavigateBack) }
        }
    }

    private inline fun updateDraft(transform: (ExploreFilters) -> ExploreFilters) {
        _uiState.update { it.copy(draft = transform(it.draft)) }
    }
}

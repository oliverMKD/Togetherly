package com.togetherly.feature.explore.presentation

import app.cash.turbine.test
import com.togetherly.core.telemetry.ExploreFiltered
import com.togetherly.core.telemetry.FakeProductAnalytics
import com.togetherly.domain.explore.ExploreFilters
import com.togetherly.domain.explore.QuestAccessFilter
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.QuestCategory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExploreFiltersViewModelTest {

    @Test
    fun draftStartsAsACopyOfTheStoresCommittedFilters() = runTest {
        val store = ExploreFilterStore()
        store.commit(ExploreFilters(category = QuestCategory.CREATE))

        val viewModel = ExploreFiltersViewModel(store, FakeProductAnalytics().apply { setCollectionEnabled(true) })

        assertEquals(ExploreFilters(category = QuestCategory.CREATE), viewModel.uiState.value.draft)
    }

    @Test
    fun changingAFilterOnlyUpdatesTheDraftNeverTheStore() = runTest {
        val store = ExploreFilterStore()
        val viewModel = ExploreFiltersViewModel(store, FakeProductAnalytics().apply { setCollectionEnabled(true) })

        viewModel.onAction(ExploreFiltersAction.DurationChanged(DurationBand.FIVE_MINUTES))

        assertEquals(DurationBand.FIVE_MINUTES, viewModel.uiState.value.draft.duration)
        assertEquals(ExploreFilters(), store.filters.value)
    }

    @Test
    fun applyCommitsTheDraftToTheStoreAndNavigatesBack() = runTest {
        val store = ExploreFilterStore()
        val viewModel = ExploreFiltersViewModel(store, FakeProductAnalytics().apply { setCollectionEnabled(true) })
        viewModel.onAction(ExploreFiltersAction.EnergyChanged(EnergyLevel.ACTIVE))

        viewModel.events.test {
            viewModel.onAction(ExploreFiltersAction.ApplyClicked)
            assertEquals(ExploreFiltersEvent.NavigateBack, awaitItem())
        }
        assertEquals(EnergyLevel.ACTIVE, store.filters.value.energy)
    }

    @Test
    fun cancelDiscardsTheDraftAndNeverTouchesTheStore() = runTest {
        val store = ExploreFilterStore()
        val viewModel = ExploreFiltersViewModel(store, FakeProductAnalytics().apply { setCollectionEnabled(true) })
        viewModel.onAction(ExploreFiltersAction.CategoryChanged(QuestCategory.SILLY))

        viewModel.events.test {
            viewModel.onAction(ExploreFiltersAction.CancelClicked)
            assertEquals(ExploreFiltersEvent.NavigateBack, awaitItem())
        }
        assertEquals(ExploreFilters(), store.filters.value)
    }

    @Test
    fun clearAllResetsTheDraftOnlyNotYetTheStore() = runTest {
        val store = ExploreFilterStore()
        val viewModel = ExploreFiltersViewModel(store, FakeProductAnalytics().apply { setCollectionEnabled(true) })
        viewModel.onAction(ExploreFiltersAction.DurationChanged(DurationBand.TEN_MINUTES))
        viewModel.onAction(ExploreFiltersAction.AccessChanged(QuestAccessFilter.PREMIUM))

        viewModel.onAction(ExploreFiltersAction.ClearAllClicked)

        assertEquals(ExploreFilters(), viewModel.uiState.value.draft)
        assertEquals(ExploreFilters(), store.filters.value)
    }

    @Test
    fun accessDefaultsToAllAndCanBeChanged() = runTest {
        val store = ExploreFilterStore()
        val viewModel = ExploreFiltersViewModel(store, FakeProductAnalytics().apply { setCollectionEnabled(true) })

        assertEquals(QuestAccessFilter.ALL, viewModel.uiState.value.draft.access)

        viewModel.onAction(ExploreFiltersAction.AccessChanged(QuestAccessFilter.PREMIUM))

        assertEquals(QuestAccessFilter.PREMIUM, viewModel.uiState.value.draft.access)
    }

    // -- Analytics --------------------------------------------------------------------------

    @Test
    fun applyCapturesExploreFilteredWithTheDraft() = runTest {
        val store = ExploreFilterStore()
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val viewModel = ExploreFiltersViewModel(store, analytics)
        viewModel.onAction(ExploreFiltersAction.EnergyChanged(EnergyLevel.ACTIVE))

        viewModel.onAction(ExploreFiltersAction.ApplyClicked)

        val event = analytics.capturedEvents.single() as ExploreFiltered
        assertEquals(EnergyLevel.ACTIVE, event.energyLevel)
    }

    @Test
    fun cancelCapturesNoFilteredEvent() = runTest {
        val store = ExploreFilterStore()
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val viewModel = ExploreFiltersViewModel(store, analytics)
        viewModel.onAction(ExploreFiltersAction.CategoryChanged(QuestCategory.SILLY))

        viewModel.onAction(ExploreFiltersAction.CancelClicked)

        assertTrue(analytics.capturedEvents.isEmpty())
    }

    @Test
    fun noEventIsCapturedWithoutConsent() = runTest {
        val store = ExploreFilterStore()
        val analytics = FakeProductAnalytics()
        val viewModel = ExploreFiltersViewModel(store, analytics)
        viewModel.onAction(ExploreFiltersAction.EnergyChanged(EnergyLevel.ACTIVE))

        viewModel.onAction(ExploreFiltersAction.ApplyClicked)

        assertTrue(analytics.capturedEvents.isEmpty())
    }
}

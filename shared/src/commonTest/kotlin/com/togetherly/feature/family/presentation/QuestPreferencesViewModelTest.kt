package com.togetherly.feature.family.presentation

import app.cash.turbine.test
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.family.QuestPreferences
import com.togetherly.domain.family.repository.FakeFamilySettingsRepository
import com.togetherly.domain.family.testFamilySettings
import com.togetherly.domain.family.testQuestPreferences
import com.togetherly.domain.family.usecase.ObserveFamilySettings
import com.togetherly.domain.family.usecase.UpdateQuestPreferences
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.feature.family.model.QuestPreferencesAction
import com.togetherly.feature.family.model.QuestPreferencesEvent
import com.togetherly.feature.family.model.QuestPreferencesField
import com.togetherly.integration.testFamilyProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class QuestPreferencesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeFamilySettingsRepository, saveMessageStore: FamilySaveMessageStore = FamilySaveMessageStore()) =
        QuestPreferencesViewModel(
            observeFamilySettings = ObserveFamilySettings(repository),
            updateQuestPreferences = UpdateQuestPreferences(repository),
            saveMessageStore = saveMessageStore,
        )

    private suspend fun startedViewModel(repository: FakeFamilySettingsRepository): QuestPreferencesViewModel {
        val viewModel = viewModel(repository)
        viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    private fun seededRepository(preferences: QuestPreferences = testQuestPreferences()): FakeFamilySettingsRepository {
        val repository = FakeFamilySettingsRepository()
        repository.setSettings(testFamilySettings(profile = testFamilyProfile(), questPreferences = preferences))
        return repository
    }

    @Test
    fun loadingPopulatesTheDraftFromStoredQuestPreferences() = runTest {
        val preferences = testQuestPreferences(
            preferredDurations = setOf(DurationBand.TEN_MINUTES, DurationBand.TWENTY_MINUTES),
            preferredEnergyLevels = setOf(EnergyLevel.CALM),
            locationPreference = LocationPreference.INDOOR,
            preparationPreference = PreparationPreference.NONE,
        )
        val viewModel = startedViewModel(seededRepository(preferences))

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(setOf(DurationBand.TEN_MINUTES, DurationBand.TWENTY_MINUTES), state.selectedDurations)
        assertEquals(setOf(EnergyLevel.CALM), state.selectedEnergyLevels)
        assertEquals(LocationPreference.INDOOR, state.locationPreference)
        assertEquals(PreparationPreference.NONE, state.preparationPreference)
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun updatingDurationTogglesMembership() = runTest {
        val viewModel = startedViewModel(seededRepository(testQuestPreferences(preferredDurations = setOf(DurationBand.TEN_MINUTES))))

        viewModel.onAction(QuestPreferencesAction.DurationToggled(DurationBand.TWENTY_MINUTES))

        assertEquals(setOf(DurationBand.TEN_MINUTES, DurationBand.TWENTY_MINUTES), viewModel.uiState.value.selectedDurations)
    }

    @Test
    fun updatingEnergyTogglesMembership() = runTest {
        val viewModel = startedViewModel(seededRepository(testQuestPreferences(preferredEnergyLevels = setOf(EnergyLevel.CALM))))

        viewModel.onAction(QuestPreferencesAction.EnergyToggled(EnergyLevel.ACTIVE))

        assertEquals(setOf(EnergyLevel.CALM, EnergyLevel.ACTIVE), viewModel.uiState.value.selectedEnergyLevels)
    }

    @Test
    fun updatingLocationReplacesTheSingleSelection() = runTest {
        val viewModel = startedViewModel(seededRepository(testQuestPreferences(locationPreference = LocationPreference.BOTH)))

        viewModel.onAction(QuestPreferencesAction.LocationPreferenceChanged(LocationPreference.OUTDOOR))

        assertEquals(LocationPreference.OUTDOOR, viewModel.uiState.value.locationPreference)
    }

    @Test
    fun updatingPreparationReplacesTheSingleSelection() = runTest {
        val viewModel = startedViewModel(seededRepository(testQuestPreferences(preparationPreference = PreparationPreference.ANY)))

        viewModel.onAction(QuestPreferencesAction.PreparationPreferenceChanged(PreparationPreference.NONE))

        assertEquals(PreparationPreference.NONE, viewModel.uiState.value.preparationPreference)
    }

    @Test
    fun multipleSelectedDurationsAndEnergyLevelsAreBothPreserved() = runTest {
        val viewModel = startedViewModel(seededRepository(testQuestPreferences(preferredDurations = emptySet(), preferredEnergyLevels = emptySet())))

        viewModel.onAction(QuestPreferencesAction.DurationToggled(DurationBand.FIVE_MINUTES))
        viewModel.onAction(QuestPreferencesAction.DurationToggled(DurationBand.TWENTY_MINUTES))
        viewModel.onAction(QuestPreferencesAction.EnergyToggled(EnergyLevel.CALM))
        viewModel.onAction(QuestPreferencesAction.EnergyToggled(EnergyLevel.MODERATE))

        val state = viewModel.uiState.value
        assertEquals(setOf(DurationBand.FIVE_MINUTES, DurationBand.TWENTY_MINUTES), state.selectedDurations)
        assertEquals(setOf(EnergyLevel.CALM, EnergyLevel.MODERATE), state.selectedEnergyLevels)
    }

    @Test
    fun savingWithNoDurationsSelectedFailsValidation() = runTest {
        val repository = seededRepository()
        val viewModel = startedViewModel(repository)
        viewModel.uiState.value.selectedDurations.forEach { viewModel.onAction(QuestPreferencesAction.DurationToggled(it)) }

        viewModel.onAction(QuestPreferencesAction.SaveClicked)

        assertTrue(viewModel.uiState.value.validationErrors.containsKey(QuestPreferencesField.DURATIONS))
    }

    @Test
    fun savingWithNoEnergyLevelsSelectedFailsValidation() = runTest {
        val repository = seededRepository()
        val viewModel = startedViewModel(repository)
        viewModel.uiState.value.selectedEnergyLevels.forEach { viewModel.onAction(QuestPreferencesAction.EnergyToggled(it)) }

        viewModel.onAction(QuestPreferencesAction.SaveClicked)

        assertTrue(viewModel.uiState.value.validationErrors.containsKey(QuestPreferencesField.ENERGY))
    }

    @Test
    fun resettingAppliesTheMaximallyPermissiveDefaults() = runTest {
        val preferences = testQuestPreferences(
            preferredDurations = setOf(DurationBand.FIVE_MINUTES),
            preferredEnergyLevels = setOf(EnergyLevel.CALM),
            locationPreference = LocationPreference.INDOOR,
            preparationPreference = PreparationPreference.NONE,
        )
        val viewModel = startedViewModel(seededRepository(preferences))

        viewModel.onAction(QuestPreferencesAction.ResetToDefaultsClicked)

        val state = viewModel.uiState.value
        assertEquals(DurationBand.entries.toSet(), state.selectedDurations)
        assertEquals(EnergyLevel.entries.toSet(), state.selectedEnergyLevels)
        assertEquals(LocationPreference.BOTH, state.locationPreference)
        assertEquals(PreparationPreference.ANY, state.preparationPreference)
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun validSaveDelegatesToTheRepositoryAndEmitsSaveCompleted() = runTest {
        val repository = seededRepository(testQuestPreferences(preferredEnergyLevels = setOf(EnergyLevel.CALM)))
        val viewModel = startedViewModel(repository)

        viewModel.onAction(QuestPreferencesAction.EnergyToggled(EnergyLevel.ACTIVE))

        viewModel.events.test {
            viewModel.onAction(QuestPreferencesAction.SaveClicked)
            assertEquals(QuestPreferencesEvent.SaveCompleted, awaitItem())
        }

        assertFalse(viewModel.uiState.value.isSaving)
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun successfulSavePublishesTheSuccessMessageToTheFamilyRootStore() = runTest {
        val saveMessageStore = FamilySaveMessageStore()
        val repository = seededRepository()
        val viewModel = viewModel(repository, saveMessageStore)
        viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(QuestPreferencesAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(saveMessageStore.message.value != null)
    }

    @Test
    fun saveFailureSurfacesAnErrorAndKeepsTheDraftEditable() = runTest {
        val repository = seededRepository()
        val viewModel = startedViewModel(repository)
        repository.setNextError(AppError.Storage(StorageError.WRITE_FAILED))

        viewModel.onAction(QuestPreferencesAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertTrue(state.error != null)
    }

    @Test
    fun backWithUnsavedChangesShowsTheDiscardDialog() = runTest {
        val viewModel = startedViewModel(seededRepository())
        viewModel.onAction(QuestPreferencesAction.LocationPreferenceChanged(LocationPreference.OUTDOOR))

        viewModel.onAction(QuestPreferencesAction.BackClicked)

        assertTrue(viewModel.uiState.value.showDiscardDialog)
    }

    @Test
    fun discardingChangesNavigatesAwayWithoutSaving() = runTest {
        val repository = seededRepository()
        val viewModel = startedViewModel(repository)
        viewModel.onAction(QuestPreferencesAction.LocationPreferenceChanged(LocationPreference.OUTDOOR))
        viewModel.onAction(QuestPreferencesAction.BackClicked)

        viewModel.events.test {
            viewModel.onAction(QuestPreferencesAction.DiscardConfirmed)
            assertEquals(QuestPreferencesEvent.NavigatedBackWithoutSaving, awaitItem())
        }
    }
}

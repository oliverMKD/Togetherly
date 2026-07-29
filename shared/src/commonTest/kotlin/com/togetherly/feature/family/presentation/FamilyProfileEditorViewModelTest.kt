package com.togetherly.feature.family.presentation

import app.cash.turbine.test
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.FamilyDisplayName
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.repository.FakeFamilyRepository
import com.togetherly.domain.family.usecase.UpdateFamilyProfile
import com.togetherly.feature.family.model.FamilyProfileAction
import com.togetherly.feature.family.model.FamilyProfileEditorEvent
import com.togetherly.feature.family.model.FamilyProfileField
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

// After FamilySettingsTestFixtures/IntegrationTestFixtures' FIXED_NOW (2026-07-24T09:00:00Z) —
// testFamilyProfile()'s own createdAt/updatedAt — since FamilyProfile's init block rejects
// updatedAt < createdAt (see that class's own invariant).
private val NOW = Instant.parse("2026-07-24T10:00:00Z")

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyProfileEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeFamilyRepository, saveMessageStore: FamilySaveMessageStore = FamilySaveMessageStore()) =
        FamilyProfileEditorViewModel(
            familyRepository = repository,
            updateFamilyProfile = UpdateFamilyProfile(repository, TestAppClock(NOW)),
            saveMessageStore = saveMessageStore,
        )

    private suspend fun startedViewModel(repository: FakeFamilyRepository): FamilyProfileEditorViewModel {
        val viewModel = viewModel(repository)
        viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    @Test
    fun loadingExistingProfilePopulatesTheDraft() = runTest {
        val repository = FakeFamilyRepository()
        val profile = testFamilyProfile(
            childAgeBands = setOf(AgeBand.AGE_6_TO_8),
            preferredDurations = setOf(DurationBand.TEN_MINUTES),
            locationPreference = LocationPreference.OUTDOOR,
        ).copy(displayName = FamilyDisplayName("The Riveras"))
        repository.saveProfile(profile)

        val viewModel = startedViewModel(repository)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("The Riveras", state.familyName)
        assertEquals(setOf(AgeBand.AGE_6_TO_8), state.selectedAgeBands)
        assertEquals(setOf(DurationBand.TEN_MINUTES), state.selectedDurations)
        assertEquals(LocationPreference.OUTDOOR, state.locationPreference)
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun validUpdateSavesAndEmitsSaveCompleted() = runTest {
        val repository = FakeFamilyRepository()
        repository.saveProfile(testFamilyProfile())
        val viewModel = startedViewModel(repository)

        viewModel.onAction(FamilyProfileAction.FamilyNameChanged("The Riveras"))
        viewModel.onAction(FamilyProfileAction.AgeBandToggled(AgeBand.AGE_6_TO_8))

        viewModel.events.test {
            viewModel.onAction(FamilyProfileAction.SaveClicked)
            assertEquals(FamilyProfileEditorEvent.SaveCompleted, awaitItem())
        }

        val saved = repository.savedProfiles.last()
        assertEquals("The Riveras", saved.displayName?.value)
        assertFalse(viewModel.uiState.value.isSaving)
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun invalidNameBlocksSaveWithAFieldError() = runTest {
        val repository = FakeFamilyRepository()
        repository.saveProfile(testFamilyProfile())
        val viewModel = startedViewModel(repository)

        viewModel.onAction(FamilyProfileAction.FamilyNameChanged("x".repeat(FamilyDisplayName.MAX_LENGTH + 1)))
        viewModel.onAction(FamilyProfileAction.SaveClicked)

        assertTrue(viewModel.uiState.value.validationErrors.containsKey(FamilyProfileField.FAMILY_NAME))
        // Only the seed profile saved in setup — the failed SaveClicked wrote nothing new.
        assertEquals(1, repository.savedProfiles.size)
    }

    /**
     * There is no participant-count field to validate — see [com.togetherly.feature.family.model.FamilyProfileUiState]'s
     * own KDoc for why. The closest real equivalent to "reject an invalid count" is rejecting an
     * empty age-band selection, since both would otherwise leave the household unmatchable by quest
     * recommendation.
     */
    @Test
    fun clearingAllAgeBandsBlocksSaveWithAFieldError() = runTest {
        val repository = FakeFamilyRepository()
        repository.saveProfile(testFamilyProfile(childAgeBands = setOf(AgeBand.AGE_6_TO_8)))
        val viewModel = startedViewModel(repository)

        viewModel.onAction(FamilyProfileAction.AgeBandToggled(AgeBand.AGE_6_TO_8))
        viewModel.onAction(FamilyProfileAction.SaveClicked)

        assertTrue(viewModel.uiState.value.validationErrors.containsKey(FamilyProfileField.AGE_BANDS))
        // Only the seed profile saved in setup — the failed SaveClicked wrote nothing new.
        assertEquals(1, repository.savedProfiles.size)
    }

    @Test
    fun savingPreservesTheExistingProfileIdAndCreationMetadata() = runTest {
        val repository = FakeFamilyRepository()
        val original = testFamilyProfile()
        repository.saveProfile(original)
        val viewModel = startedViewModel(repository)

        viewModel.onAction(FamilyProfileAction.FamilyNameChanged("The Riveras"))
        viewModel.onAction(FamilyProfileAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val saved = repository.savedProfiles.last()
        assertEquals(original.id, saved.id)
        assertEquals(original.createdAt, saved.createdAt)
        assertEquals(NOW, saved.updatedAt)
        assertTrue(saved.updatedAt > original.updatedAt)
    }

    @Test
    fun backWithUnsavedChangesShowsTheDiscardDialogInsteadOfNavigatingAway() = runTest {
        val repository = FakeFamilyRepository()
        repository.saveProfile(testFamilyProfile())
        val viewModel = startedViewModel(repository)
        viewModel.onAction(FamilyProfileAction.FamilyNameChanged("The Riveras"))
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.onAction(FamilyProfileAction.BackClicked)

        assertTrue(viewModel.uiState.value.showDiscardDialog)
    }

    @Test
    fun backWithNoUnsavedChangesNavigatesAwayImmediately() = runTest {
        val repository = FakeFamilyRepository()
        repository.saveProfile(testFamilyProfile())
        val viewModel = startedViewModel(repository)

        viewModel.events.test {
            viewModel.onAction(FamilyProfileAction.BackClicked)
            assertEquals(FamilyProfileEditorEvent.NavigatedBackWithoutSaving, awaitItem())
        }
    }

    @Test
    fun discardingChangesClearsTheDialogAndNavigatesAway() = runTest {
        val repository = FakeFamilyRepository()
        repository.saveProfile(testFamilyProfile())
        val viewModel = startedViewModel(repository)
        viewModel.onAction(FamilyProfileAction.FamilyNameChanged("The Riveras"))
        viewModel.onAction(FamilyProfileAction.BackClicked)
        assertTrue(viewModel.uiState.value.showDiscardDialog)

        viewModel.events.test {
            viewModel.onAction(FamilyProfileAction.DiscardConfirmed)
            assertEquals(FamilyProfileEditorEvent.NavigatedBackWithoutSaving, awaitItem())
        }
        assertFalse(viewModel.uiState.value.showDiscardDialog)
    }

    @Test
    fun dismissingTheDiscardDialogKeepsEditingWithChangesIntact() = runTest {
        val repository = FakeFamilyRepository()
        repository.saveProfile(testFamilyProfile())
        val viewModel = startedViewModel(repository)
        viewModel.onAction(FamilyProfileAction.FamilyNameChanged("The Riveras"))
        viewModel.onAction(FamilyProfileAction.BackClicked)

        viewModel.onAction(FamilyProfileAction.DismissDiscardDialog)

        val state = viewModel.uiState.value
        assertFalse(state.showDiscardDialog)
        assertEquals("The Riveras", state.familyName)
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun saveFailureSurfacesAnErrorAndKeepsTheDraftEditable() = runTest {
        val repository = FakeFamilyRepository()
        repository.saveProfile(testFamilyProfile())
        val viewModel = startedViewModel(repository)
        viewModel.onAction(FamilyProfileAction.FamilyNameChanged("The Riveras"))
        val error = AppError.Storage(StorageError.WRITE_FAILED)
        repository.setNextError(error)

        viewModel.onAction(FamilyProfileAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals(error.toUiText(), state.error)
        assertEquals("The Riveras", state.familyName)
    }

    @Test
    fun successfulSavePublishesTheSuccessMessageToTheFamilyRootStore() = runTest {
        val repository = FakeFamilyRepository()
        repository.saveProfile(testFamilyProfile())
        val saveMessageStore = FamilySaveMessageStore()
        val viewModel = viewModel(repository, saveMessageStore)
        viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(saveMessageStore.message.value)
        viewModel.onAction(FamilyProfileAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, saveMessageStore.message.value != null)
    }
}

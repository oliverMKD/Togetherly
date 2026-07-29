package com.togetherly.feature.family.presentation

import app.cash.turbine.test
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.domain.family.MemoryPreferences
import com.togetherly.domain.family.repository.FakeFamilySettingsRepository
import com.togetherly.domain.family.testFamilySettings
import com.togetherly.domain.family.usecase.ObserveFamilySettings
import com.togetherly.domain.family.usecase.UpdateMemoryPreferences
import com.togetherly.feature.family.model.MemorySettingsAction
import com.togetherly.feature.family.model.MemorySettingsEvent
import com.togetherly.integration.testFamilyProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class MemorySettingsViewModelTest {

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
        MemorySettingsViewModel(
            observeFamilySettings = ObserveFamilySettings(repository),
            updateMemoryPreferences = UpdateMemoryPreferences(repository),
            saveMessageStore = saveMessageStore,
        )

    private fun seededRepository(preferences: MemoryPreferences = MemoryPreferences.defaults()): FakeFamilySettingsRepository {
        val repository = FakeFamilySettingsRepository()
        repository.setSettings(testFamilySettings(profile = testFamilyProfile(), memoryPreferences = preferences))
        return repository
    }

    private suspend fun startedViewModel(repository: FakeFamilySettingsRepository): MemorySettingsViewModel {
        val viewModel = viewModel(repository)
        viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    @Test
    fun loadingPopulatesTheDraftFromStoredMemoryPreferences() = runTest {
        val preferences = MemoryPreferences(allowPhotos = false, allowVoiceMemories = true, allowTextNotes = false, showMemoryPromptAfterQuests = false)
        val viewModel = startedViewModel(seededRepository(preferences))

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.allowPhotos)
        assertTrue(state.allowVoiceMemories)
        assertFalse(state.allowTextNotes)
        assertFalse(state.showMemoryPromptAfterQuests)
    }

    @Test
    fun disablingPhotoCaptureAndSavingPersistsTheChange() = runTest {
        val repository = seededRepository()
        val viewModel = startedViewModel(repository)

        viewModel.onAction(MemorySettingsAction.AllowPhotosChanged(false))
        viewModel.onAction(MemorySettingsAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val stored = (repository.observeSettings().first() as com.togetherly.core.result.DataResult.Success).value
        assertFalse(requireNotNull(stored).memoryPreferences.allowPhotos)
    }

    @Test
    fun disablingVoiceCaptureAndSavingPersistsTheChange() = runTest {
        val repository = seededRepository()
        val viewModel = startedViewModel(repository)

        viewModel.onAction(MemorySettingsAction.AllowVoiceMemoriesChanged(false))
        viewModel.onAction(MemorySettingsAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val stored = (repository.observeSettings().first() as com.togetherly.core.result.DataResult.Success).value
        assertFalse(requireNotNull(stored).memoryPreferences.allowVoiceMemories)
    }

    @Test
    fun manageMemoriesClickedEmitsTheNavigationEvent() = runTest {
        val viewModel = startedViewModel(seededRepository())

        viewModel.events.test {
            viewModel.onAction(MemorySettingsAction.ManageMemoriesClicked)
            assertEquals(MemorySettingsEvent.OpenManageMemories, awaitItem())
        }
    }

    @Test
    fun togglingPreferencesNeverEmitsAnyPermissionRelatedEvent() = runTest {
        val viewModel = startedViewModel(seededRepository())

        viewModel.events.test {
            viewModel.onAction(MemorySettingsAction.AllowPhotosChanged(false))
            viewModel.onAction(MemorySettingsAction.AllowVoiceMemoriesChanged(false))
            viewModel.onAction(MemorySettingsAction.AllowTextNotesChanged(false))
            viewModel.onAction(MemorySettingsAction.ShowMemoryPromptAfterQuestsChanged(false))
            // MemorySettingsEvent has no permission-request case at all — this screen has no
            // camera/microphone/notification dependency to request from in the first place.
            expectNoEvents()
        }
    }

    @Test
    fun saveFailureSurfacesAnError() = runTest {
        val repository = seededRepository()
        val viewModel = startedViewModel(repository)
        repository.setNextError(AppError.Storage(StorageError.WRITE_FAILED))

        viewModel.onAction(MemorySettingsAction.AllowPhotosChanged(false))
        viewModel.onAction(MemorySettingsAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.error != null)
    }

    @Test
    fun backWithUnsavedChangesShowsTheDiscardDialog() = runTest {
        val viewModel = startedViewModel(seededRepository())

        viewModel.onAction(MemorySettingsAction.AllowPhotosChanged(false))
        viewModel.onAction(MemorySettingsAction.BackClicked)

        assertTrue(viewModel.uiState.value.showDiscardDialog)
    }
}

package com.togetherly.feature.family.presentation

import app.cash.turbine.test
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.ui.UiText
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.repository.FakeMemoryCleaner
import com.togetherly.domain.completion.repository.FakePrivateMediaCommitter
import com.togetherly.domain.completion.validQuestCompletion
import com.togetherly.domain.family.repository.FakeFamilyDataCleaner
import com.togetherly.domain.family.repository.FakeQuestHistoryCleaner
import com.togetherly.domain.localdata.usecase.DeleteAllLocalData
import com.togetherly.domain.localdata.usecase.DeleteMemories
import com.togetherly.domain.localdata.usecase.ResetQuestHistory
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import com.togetherly.domain.telemetry.repository.FakeTelemetryConsentRepository
import com.togetherly.core.notification.FakeReminderScheduler
import com.togetherly.feature.family.model.DataManagementAction
import com.togetherly.feature.family.model.DataManagementConfirmationStage
import com.togetherly.feature.family.model.DataManagementEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.data_management_delete_memories_success
import togetherly.shared.generated.resources.data_management_partial_file_failure_message
import togetherly.shared.generated.resources.data_management_reset_quest_history_success
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private val NOW = Instant.parse("2026-07-25T09:00:00Z")
private val TEST_PHOTO: MemoryMedia = MemoryMedia.Photo(id = MemoryMediaId("photo-1"), localReference = MediaReference("ref-photo"))

@OptIn(ExperimentalCoroutinesApi::class)
class DataManagementViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        completionRepository: FakeCompletionRepository = FakeCompletionRepository(),
        memoryCleaner: FakeMemoryCleaner = FakeMemoryCleaner(),
        questHistoryCleaner: FakeQuestHistoryCleaner = FakeQuestHistoryCleaner(),
        familyDataCleaner: FakeFamilyDataCleaner = FakeFamilyDataCleaner(),
        mediaCommitter: FakePrivateMediaCommitter = FakePrivateMediaCommitter(),
        reminderScheduler: FakeReminderScheduler = FakeReminderScheduler(),
        entitlementRepository: FakeEntitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)),
        telemetryConsentRepository: FakeTelemetryConsentRepository = FakeTelemetryConsentRepository(),
    ) = DataManagementViewModel(
        deleteMemories = DeleteMemories(completionRepository, memoryCleaner, mediaCommitter),
        resetQuestHistory = ResetQuestHistory(completionRepository, questHistoryCleaner, mediaCommitter),
        deleteAllLocalData = DeleteAllLocalData(completionRepository, familyDataCleaner, mediaCommitter, reminderScheduler, entitlementRepository, telemetryConsentRepository),
    )

    @Test
    fun deleteMemoriesClickedShowsItsOwnConfirmationStage() {
        val model = viewModel()

        model.onAction(DataManagementAction.DeleteMemoriesClicked)

        assertEquals(DataManagementConfirmationStage.CONFIRM_DELETE_MEMORIES, model.uiState.value.confirmationStage)
    }

    @Test
    fun confirmingDeleteMemoriesRunsItAndShowsSuccessMessage() = runTest {
        val model = viewModel()
        model.onAction(DataManagementAction.DeleteMemoriesClicked)

        model.onAction(DataManagementAction.DestructiveActionConfirmed)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(DataManagementConfirmationStage.NONE, model.uiState.value.confirmationStage)
        assertFalse(model.uiState.value.isBusy)
        assertEquals(UiText.Resource(Res.string.data_management_delete_memories_success), model.uiState.value.message)
    }

    @Test
    fun confirmingDeleteMemoriesWithAPartialFileFailureShowsTheSofterNotice() = runTest {
        val completionRepository = FakeCompletionRepository().apply { saveCompletion(validQuestCompletion(media = listOf(TEST_PHOTO))) }
        val mediaCommitter = FakePrivateMediaCommitter().apply { setDeleteCommittedError(AppError.Storage(StorageError.DELETE_FAILED)) }
        val model = viewModel(completionRepository = completionRepository, mediaCommitter = mediaCommitter)
        model.onAction(DataManagementAction.DeleteMemoriesClicked)

        model.onAction(DataManagementAction.DestructiveActionConfirmed)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(UiText.Resource(Res.string.data_management_partial_file_failure_message), model.uiState.value.message)
    }

    @Test
    fun confirmingDeleteMemoriesWithADatabaseErrorShowsTheGenericErrorMessage() = runTest {
        val memoryCleaner = FakeMemoryCleaner().apply { setNextError(AppError.Storage(StorageError.DELETE_FAILED)) }
        val model = viewModel(memoryCleaner = memoryCleaner)
        model.onAction(DataManagementAction.DeleteMemoriesClicked)

        model.onAction(DataManagementAction.DestructiveActionConfirmed)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AppError.Storage(StorageError.DELETE_FAILED).toUiText(), model.uiState.value.message)
    }

    @Test
    fun confirmingResetQuestHistoryRunsItAndShowsSuccessMessage() = runTest {
        val model = viewModel()
        model.onAction(DataManagementAction.ResetQuestHistoryClicked)

        model.onAction(DataManagementAction.DestructiveActionConfirmed)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(UiText.Resource(Res.string.data_management_reset_quest_history_success), model.uiState.value.message)
    }

    @Test
    fun deleteAllDataClickedShowsStageOneThenContinueAdvancesToStageTwo() {
        val model = viewModel()

        model.onAction(DataManagementAction.DeleteAllDataClicked)
        assertEquals(DataManagementConfirmationStage.DELETE_ALL_STAGE_ONE, model.uiState.value.confirmationStage)

        model.onAction(DataManagementAction.DeleteAllDataContinueClicked)
        assertEquals(DataManagementConfirmationStage.DELETE_ALL_STAGE_TWO, model.uiState.value.confirmationStage)
    }

    @Test
    fun confirmingAtStageOneNeverRunsTheDeletion() = runTest {
        val familyDataCleaner = FakeFamilyDataCleaner()
        val model = viewModel(familyDataCleaner = familyDataCleaner)
        model.onAction(DataManagementAction.DeleteAllDataClicked)

        model.onAction(DataManagementAction.DestructiveActionConfirmed)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, familyDataCleaner.deleteCallCount)
    }

    @Test
    fun confirmingAtStageTwoRunsDeleteAllAndEmitsLocalDataDeletedOnSuccess() = runTest {
        val familyDataCleaner = FakeFamilyDataCleaner()
        val model = viewModel(familyDataCleaner = familyDataCleaner)
        model.onAction(DataManagementAction.DeleteAllDataClicked)
        model.onAction(DataManagementAction.DeleteAllDataContinueClicked)

        model.events.test {
            model.onAction(DataManagementAction.DestructiveActionConfirmed)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(DataManagementEvent.LocalDataDeleted, awaitItem())
        }
        assertEquals(1, familyDataCleaner.deleteCallCount)
        assertFalse(model.uiState.value.isBusy)
        assertNull(model.uiState.value.message)
    }

    @Test
    fun deleteAllDataFailureShowsAnErrorAndNeverEmitsLocalDataDeleted() = runTest {
        val familyDataCleaner = FakeFamilyDataCleaner().apply { setNextError(AppError.Storage(StorageError.DELETE_FAILED)) }
        val model = viewModel(familyDataCleaner = familyDataCleaner)
        model.onAction(DataManagementAction.DeleteAllDataClicked)
        model.onAction(DataManagementAction.DeleteAllDataContinueClicked)

        model.onAction(DataManagementAction.DestructiveActionConfirmed)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AppError.Storage(StorageError.DELETE_FAILED).toUiText(), model.uiState.value.message)
        assertFalse(model.uiState.value.isBusy)
    }

    @Test
    fun destructiveEntryPointsAreIgnoredWhileBusy() = runTest {
        val model = viewModel()
        model.onAction(DataManagementAction.DeleteMemoriesClicked)
        model.onAction(DataManagementAction.DestructiveActionConfirmed)
        // Busy now (isDeletingMemories = true) — the underlying coroutine hasn't resumed yet.
        assertTrue(model.uiState.value.isBusy)

        model.onAction(DataManagementAction.ResetQuestHistoryClicked)
        model.onAction(DataManagementAction.DeleteAllDataClicked)
        model.onAction(DataManagementAction.BackClicked)

        assertEquals(DataManagementConfirmationStage.NONE, model.uiState.value.confirmationStage)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun backClickedWhileIdleEmitsNavigateBack() = runTest {
        val model = viewModel()

        model.events.test {
            model.onAction(DataManagementAction.BackClicked)
            assertEquals(DataManagementEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun confirmationDismissedResetsStageWithoutRunningAnything() = runTest {
        val memoryCleaner = FakeMemoryCleaner()
        val model = viewModel(memoryCleaner = memoryCleaner)
        model.onAction(DataManagementAction.DeleteMemoriesClicked)

        model.onAction(DataManagementAction.ConfirmationDismissed)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(DataManagementConfirmationStage.NONE, model.uiState.value.confirmationStage)
        assertEquals(0, memoryCleaner.clearCallCount)
    }

    @Test
    fun messageDismissedClearsTheMessage() = runTest {
        val model = viewModel()
        model.onAction(DataManagementAction.DeleteMemoriesClicked)
        model.onAction(DataManagementAction.DestructiveActionConfirmed)
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(DataManagementAction.MessageDismissed)

        assertNull(model.uiState.value.message)
    }
}

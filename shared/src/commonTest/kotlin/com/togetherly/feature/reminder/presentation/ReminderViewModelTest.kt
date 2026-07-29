package com.togetherly.feature.reminder.presentation

import app.cash.turbine.test
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.notification.FakeNotificationPermissionStatusProvider
import com.togetherly.core.notification.FakeReminderScheduler
import com.togetherly.core.notification.NotificationPermissionState
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.FakeProductAnalytics
import com.togetherly.core.telemetry.ReminderPreferenceChanged
import com.togetherly.domain.family.ReminderPreference
import com.togetherly.domain.family.repository.FakeFamilySettingsRepository
import com.togetherly.domain.family.testFamilySettings
import com.togetherly.domain.family.usecase.ObserveFamilySettings
import com.togetherly.domain.family.usecase.UpdateReminderPreference
import com.togetherly.feature.family.presentation.FamilySaveMessageStore
import com.togetherly.feature.reminder.model.ReminderAction
import com.togetherly.feature.reminder.model.ReminderEvent
import com.togetherly.feature.reminder.model.ReminderField
import com.togetherly.integration.testFamilyProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderViewModelTest {

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
        repository: FakeFamilySettingsRepository,
        scheduler: FakeReminderScheduler = FakeReminderScheduler(),
        permissionStatusProvider: FakeNotificationPermissionStatusProvider = FakeNotificationPermissionStatusProvider(),
        saveMessageStore: FamilySaveMessageStore = FamilySaveMessageStore(),
        analytics: FakeProductAnalytics = FakeProductAnalytics().apply { setCollectionEnabled(true) },
    ) = ReminderViewModel(
        observeFamilySettings = ObserveFamilySettings(repository),
        updateReminderPreference = UpdateReminderPreference(repository),
        notificationPermissionStatusProvider = permissionStatusProvider,
        reminderScheduler = scheduler,
        saveMessageStore = saveMessageStore,
        analytics = analytics,
    )

    private fun seededRepository(reminderPreference: ReminderPreference? = null): FakeFamilySettingsRepository {
        val repository = FakeFamilySettingsRepository()
        repository.setSettings(testFamilySettings(profile = testFamilyProfile(), reminderPreference = reminderPreference))
        return repository
    }

    private suspend fun startedViewModel(
        repository: FakeFamilySettingsRepository,
        scheduler: FakeReminderScheduler = FakeReminderScheduler(),
        permissionStatusProvider: FakeNotificationPermissionStatusProvider = FakeNotificationPermissionStatusProvider(),
        saveMessageStore: FamilySaveMessageStore = FamilySaveMessageStore(),
        analytics: FakeProductAnalytics = FakeProductAnalytics().apply { setCollectionEnabled(true) },
    ): ReminderViewModel {
        val viewModel = viewModel(repository, scheduler, permissionStatusProvider, saveMessageStore, analytics)
        viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    @Test
    fun loadingWithNoStoredPreferenceDefaultsToDisabled() = runTest {
        val viewModel = startedViewModel(seededRepository(reminderPreference = null))

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.enabled)
        assertTrue(state.days.isEmpty())
    }

    @Test
    fun loadingPopulatesTheDraftFromAStoredPreference() = runTest {
        val preference = ReminderPreference(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), LocalTime(18, 0))
        val viewModel = startedViewModel(seededRepository(preference))

        val state = viewModel.uiState.value
        assertTrue(state.enabled)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), state.days)
        assertEquals(LocalTime(18, 0), state.time)
    }

    @Test
    fun loadingAlsoReadsTheCurrentNotificationPermissionStatusWithoutRequestingIt() = runTest {
        val permissionStatusProvider = FakeNotificationPermissionStatusProvider(NotificationPermissionState.Denied)
        val viewModel = startedViewModel(seededRepository(), permissionStatusProvider = permissionStatusProvider)

        assertEquals(NotificationPermissionState.Denied, viewModel.uiState.value.permissionState)
    }

    @Test
    fun permissionIsRequestedOnlyAfterExplicitEnableNeverOnLoad() = runTest {
        val viewModel = viewModel(seededRepository())

        viewModel.events.test {
            viewModel.onScreenStarted()
            testDispatcher.scheduler.advanceUntilIdle()
            expectNoEvents()

            viewModel.onAction(ReminderAction.EnabledChanged(true))

            assertEquals(ReminderEvent.RequestNotificationPermission, awaitItem())
        }
    }

    @Test
    fun disablingNeverRequestsPermission() = runTest {
        val viewModel = startedViewModel(seededRepository(ReminderPreference(setOf(DayOfWeek.MONDAY), LocalTime(18, 0))))

        viewModel.events.test {
            viewModel.onAction(ReminderAction.EnabledChanged(false))
            expectNoEvents()
        }
    }

    @Test
    fun aDeniedPermissionResultNeverForcesReminderOffButDoesNotClaimSuccess() = runTest {
        val viewModel = startedViewModel(seededRepository())
        viewModel.onAction(ReminderAction.EnabledChanged(true))

        viewModel.onAction(ReminderAction.PermissionResultReceived(NotificationPermissionState.PermanentlyDenied))

        val state = viewModel.uiState.value
        // The family's own intent (the toggle) is preserved — never silently flipped back off —
        // but the permission state on screen honestly reflects that nothing will actually fire.
        assertTrue(state.enabled)
        assertEquals(NotificationPermissionState.PermanentlyDenied, state.permissionState)
    }

    @Test
    fun changingTheSelectedDaysUpdatesTheDraft() = runTest {
        val viewModel = startedViewModel(seededRepository(ReminderPreference(setOf(DayOfWeek.MONDAY), LocalTime(18, 0))))

        viewModel.onAction(ReminderAction.DayToggled(DayOfWeek.WEDNESDAY))

        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), viewModel.uiState.value.days)
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun changingTheTimeUpdatesTheDraft() = runTest {
        val viewModel = startedViewModel(seededRepository(ReminderPreference(setOf(DayOfWeek.MONDAY), LocalTime(18, 0))))

        viewModel.onAction(ReminderAction.TimeChanged(LocalTime(9, 30)))

        assertEquals(LocalTime(9, 30), viewModel.uiState.value.time)
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun savingWithNoDaysSelectedFailsValidation() = runTest {
        val viewModel = startedViewModel(seededRepository())
        viewModel.onAction(ReminderAction.EnabledChanged(true))
        viewModel.onAction(ReminderAction.TimeChanged(LocalTime(18, 0)))

        viewModel.onAction(ReminderAction.SaveClicked)

        assertTrue(viewModel.uiState.value.validationErrors.containsKey(ReminderField.DAYS))
    }

    @Test
    fun savingWithNoTimeChosenFailsValidation() = runTest {
        val viewModel = startedViewModel(seededRepository())
        viewModel.onAction(ReminderAction.EnabledChanged(true))
        viewModel.onAction(ReminderAction.DayToggled(DayOfWeek.MONDAY))

        viewModel.onAction(ReminderAction.SaveClicked)

        assertTrue(viewModel.uiState.value.validationErrors.containsKey(ReminderField.TIME))
    }

    @Test
    fun disablingAndSavingCancelsTheSchedulerAndPersistsNull() = runTest {
        val repository = seededRepository(ReminderPreference(setOf(DayOfWeek.MONDAY), LocalTime(18, 0)))
        val scheduler = FakeReminderScheduler()
        val viewModel = startedViewModel(repository, scheduler)

        viewModel.onAction(ReminderAction.EnabledChanged(false))
        viewModel.onAction(ReminderAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, scheduler.cancelCallCount)
        val stored = (repository.observeSettings().first() as DataResult.Success).value
        assertEquals(null, stored?.reminderPreference)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun successfulSaveSchedulesTheReminderAndEmitsSaveCompleted() = runTest {
        val repository = seededRepository()
        val scheduler = FakeReminderScheduler()
        val viewModel = startedViewModel(repository, scheduler)
        viewModel.onAction(ReminderAction.EnabledChanged(true))
        viewModel.onAction(ReminderAction.DayToggled(DayOfWeek.TUESDAY))
        viewModel.onAction(ReminderAction.TimeChanged(LocalTime(19, 0)))

        viewModel.events.test {
            assertEquals(ReminderEvent.RequestNotificationPermission, awaitItem())
            viewModel.onAction(ReminderAction.SaveClicked)
            assertEquals(ReminderEvent.SaveCompleted, awaitItem())
        }

        assertEquals(listOf(ReminderPreference(setOf(DayOfWeek.TUESDAY), LocalTime(19, 0))), scheduler.scheduledCalls)
    }

    @Test
    fun openSettingsClickedEmitsThePlatformSettingsNavigationEvent() = runTest {
        val viewModel = startedViewModel(seededRepository())

        viewModel.events.test {
            viewModel.onAction(ReminderAction.OpenSettingsClicked)
            assertEquals(ReminderEvent.OpenSystemSettings, awaitItem())
        }
    }

    @Test
    fun backWithUnsavedChangesShowsTheDiscardDialog() = runTest {
        val viewModel = startedViewModel(seededRepository(ReminderPreference(setOf(DayOfWeek.MONDAY), LocalTime(18, 0))))
        viewModel.onAction(ReminderAction.TimeChanged(LocalTime(19, 0)))

        viewModel.onAction(ReminderAction.BackClicked)

        assertTrue(viewModel.uiState.value.showDiscardDialog)
    }

    @Test
    fun saveFailureSurfacesAnErrorAndNeverSchedules() = runTest {
        val repository = seededRepository()
        val scheduler = FakeReminderScheduler()
        val viewModel = startedViewModel(repository, scheduler)
        viewModel.onAction(ReminderAction.EnabledChanged(true))
        viewModel.onAction(ReminderAction.DayToggled(DayOfWeek.MONDAY))
        viewModel.onAction(ReminderAction.TimeChanged(LocalTime(18, 0)))
        repository.setNextError(AppError.Storage(StorageError.WRITE_FAILED))

        viewModel.onAction(ReminderAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.error != null)
        assertTrue(scheduler.scheduledCalls.isEmpty())
    }

    // -- Analytics --------------------------------------------------------------------------

    @Test
    fun screenStartedCapturesTheReminderScreenExactlyOnce() = runTest {
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val viewModel = viewModel(seededRepository(), analytics = analytics)

        viewModel.onScreenStarted()
        viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(AnalyticsScreen.REMINDER), analytics.screensViewed)
    }

    @Test
    fun successfulSaveWithAChangeCapturesPreferenceChangedWithoutExactTimeOrDays() = runTest {
        val repository = seededRepository()
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val viewModel = startedViewModel(repository, analytics = analytics)
        viewModel.onAction(ReminderAction.EnabledChanged(true))
        viewModel.onAction(ReminderAction.DayToggled(DayOfWeek.TUESDAY))
        viewModel.onAction(ReminderAction.DayToggled(DayOfWeek.THURSDAY))
        viewModel.onAction(ReminderAction.TimeChanged(LocalTime(19, 0)))

        viewModel.onAction(ReminderAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = analytics.capturedEvents.single() as ReminderPreferenceChanged
        assertTrue(event.enabled)
        assertEquals(2, event.selectedDayCount)
    }

    @Test
    fun savingWithoutAnyActualChangeCapturesNoEvent() = runTest {
        val repository = seededRepository(ReminderPreference(setOf(DayOfWeek.MONDAY), LocalTime(18, 0)))
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val viewModel = startedViewModel(repository, analytics = analytics)

        viewModel.onAction(ReminderAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(analytics.capturedEvents.none { it is ReminderPreferenceChanged })
    }

    @Test
    fun failedSaveCapturesNoPreferenceChangedEvent() = runTest {
        val repository = seededRepository()
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val viewModel = startedViewModel(repository, analytics = analytics)
        viewModel.onAction(ReminderAction.EnabledChanged(true))
        viewModel.onAction(ReminderAction.DayToggled(DayOfWeek.MONDAY))
        viewModel.onAction(ReminderAction.TimeChanged(LocalTime(18, 0)))
        repository.setNextError(AppError.Storage(StorageError.WRITE_FAILED))

        viewModel.onAction(ReminderAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(analytics.capturedEvents.none { it is ReminderPreferenceChanged })
    }

    @Test
    fun noEventsAreCapturedWithoutConsent() = runTest {
        val repository = seededRepository()
        val analytics = FakeProductAnalytics()
        val viewModel = viewModel(repository, analytics = analytics)

        viewModel.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onAction(ReminderAction.EnabledChanged(true))
        viewModel.onAction(ReminderAction.DayToggled(DayOfWeek.MONDAY))
        viewModel.onAction(ReminderAction.TimeChanged(LocalTime(18, 0)))
        viewModel.onAction(ReminderAction.SaveClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(analytics.screensViewed.isEmpty())
        assertTrue(analytics.capturedEvents.isEmpty())
    }
}

package com.togetherly.navigation.state

import app.cash.turbine.test
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.family.FamilyProfile
import com.togetherly.domain.family.repository.FakeFamilyRepository
import com.togetherly.core.notification.FakeReminderScheduler
import com.togetherly.data.media.FakePendingMediaOrphanCleaner
import com.togetherly.domain.family.repository.FamilyRepository
import com.togetherly.integration.testFamilyProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * [BootstrapViewModel] reads `viewModelScope`, which defaults to `Dispatchers.Main.immediate` —
 * these tests swap it for a deterministic test dispatcher exactly like any other
 * `viewModelScope`-based unit test.
 *
 * A [StandardTestDispatcher] (not `Unconfined`) matters here specifically: `uiState` is a
 * `stateIn`-shared `StateFlow`, and with an unconfined dispatcher the upstream production and
 * Turbine's own collector race in one synchronous burst, letting the `Loading` seed value be
 * skipped past before the collector ever observes it. `StandardTestDispatcher` queues work
 * instead, so `runTest`/Turbine's `awaitItem()` advance it one step at a time and every
 * intermediate state is actually observed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BootstrapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadingIsTheOnlyStateBeforeAnyCollector() = runTest {
        val viewModel = BootstrapViewModel(FakeFamilyRepository(), FakePendingMediaOrphanCleaner(), FakeReminderScheduler(), TestAppClock(Instant.fromEpochSeconds(0)), FakeOperationalDiagnostics())

        assertEquals(BootstrapUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun sweepsExpiredPendingMediaOnceOnConstruction() = runTest {
        val cleaner = FakePendingMediaOrphanCleaner()
        val now = Instant.fromEpochSeconds(1_700_000_000)

        BootstrapViewModel(FakeFamilyRepository(), cleaner, FakeReminderScheduler(), TestAppClock(now), FakeOperationalDiagnostics())
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(now), cleaner.calls)
    }

    @Test
    fun missingProfileRoutesToRequiresOnboarding() = runTest {
        val viewModel = BootstrapViewModel(FakeFamilyRepository(), FakePendingMediaOrphanCleaner(), FakeReminderScheduler(), TestAppClock(Instant.fromEpochSeconds(0)), FakeOperationalDiagnostics())

        viewModel.uiState.test {
            assertEquals(BootstrapUiState.Loading, awaitItem())
            assertEquals(BootstrapUiState.RequiresOnboarding, awaitItem())
        }
    }

    @Test
    fun existingProfileRoutesToReady() = runTest {
        val repository = FakeFamilyRepository()
        repository.saveProfile(testFamilyProfile())
        val viewModel = BootstrapViewModel(repository, FakePendingMediaOrphanCleaner(), FakeReminderScheduler(), TestAppClock(Instant.fromEpochSeconds(0)), FakeOperationalDiagnostics())

        viewModel.uiState.test {
            assertEquals(BootstrapUiState.Loading, awaitItem())
            assertEquals(BootstrapUiState.Ready, awaitItem())
        }
    }

    @Test
    fun observeErrorSurfacesAsUiTextNeverAsRawAppError() = runTest {
        val error = AppError.Storage(StorageError.READ_FAILED)
        val repository = FakeFamilyRepository()
        repository.emitObserveError(error)
        val viewModel = BootstrapViewModel(repository, FakePendingMediaOrphanCleaner(), FakeReminderScheduler(), TestAppClock(Instant.fromEpochSeconds(0)), FakeOperationalDiagnostics())

        viewModel.uiState.test {
            assertEquals(BootstrapUiState.Loading, awaitItem())
            assertEquals(BootstrapUiState.Error(error.toUiText()), awaitItem())
        }
    }

    @Test
    fun retryReSubscribesAfterTheObservingFlowTerminatesOnError() = runTest {
        val error = AppError.Storage(StorageError.READ_FAILED)
        val repository = ObserveOnceFamilyRepository(
            mutableListOf(DataResult.Error(error), DataResult.Success(null)),
        )
        val viewModel = BootstrapViewModel(repository, FakePendingMediaOrphanCleaner(), FakeReminderScheduler(), TestAppClock(Instant.fromEpochSeconds(0)), FakeOperationalDiagnostics())

        viewModel.uiState.test {
            assertEquals(BootstrapUiState.Loading, awaitItem())
            assertEquals(BootstrapUiState.Error(error.toUiText()), awaitItem())

            viewModel.retry()

            assertEquals(BootstrapUiState.RequiresOnboarding, awaitItem())
        }
        assertEquals(2, repository.observeCallCount)
    }
}

/**
 * A [FamilyRepository] double whose [observeProfile] returns a *fresh*, single-value Flow on
 * every call and never updates on its own — unlike [FakeFamilyRepository] (a single long-lived
 * `MutableStateFlow`, unsuitable here), this mirrors the real `RoomFamilyRepository`'s Flow
 * *terminating* after an error, so a test can prove [BootstrapViewModel.retry] genuinely issues a
 * new [observeProfile] call rather than the same Flow somehow recovering on its own.
 */
private class ObserveOnceFamilyRepository(
    private val results: MutableList<DataResult<FamilyProfile?>>,
) : FamilyRepository {

    var observeCallCount = 0
        private set

    override fun observeProfile(): Flow<DataResult<FamilyProfile?>> {
        observeCallCount++
        return flowOf(results.removeAt(0))
    }

    override suspend fun getProfile(): DataResult<FamilyProfile?> = results.first()

    override suspend fun saveProfile(profile: FamilyProfile): DataResult<Unit> = DataResult.Success(Unit)

    override suspend fun deleteProfile(): DataResult<Unit> = DataResult.Success(Unit)
}

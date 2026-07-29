package com.togetherly.feature.familyplus.presentation

import app.cash.turbine.test
import com.togetherly.core.datetime.AppTimeZoneProvider
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.FakeProductAnalytics
import com.togetherly.core.telemetry.RestoreOutcome
import com.togetherly.core.telemetry.RestoreOutcomeResult
import com.togetherly.core.telemetry.RestoreStarted
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.PurchaseError
import com.togetherly.domain.purchase.RestoreResult
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import com.togetherly.domain.purchase.usecase.RestoreFamilyPlus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")
private val UTC_PROVIDER = object : AppTimeZoneProvider {
    override fun current(): TimeZone = TimeZone.UTC
}

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyPlusManagementViewModelTest {

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
        repository: FakeEntitlementRepository,
        analytics: FakeProductAnalytics = FakeProductAnalytics().apply { setCollectionEnabled(true) },
    ) = FamilyPlusManagementViewModel(
        entitlementRepository = repository,
        restoreFamilyPlus = RestoreFamilyPlus(repository),
        timeZoneProvider = UTC_PROVIDER,
        analytics = analytics,
    )

    @Test
    fun freeAccessShowsNoRenewalInfo() = runTest {
        val repository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val model = viewModel(repository)

        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(model.uiState.value.access.isPlus)
        assertNull(model.uiState.value.renewalInfo)
    }

    @Test
    fun activeSubscriptionShowsRenewalInfo() = runTest {
        val repository = FakeEntitlementRepository(
            AccessSnapshot(FamilyAccess.subscription(expiresAt = NOW, willRenew = true), emptySet(), NOW),
        )
        val model = viewModel(repository)

        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(model.uiState.value.access.isPlus)
        assertTrue(model.uiState.value.renewalInfo != null)
    }

    @Test
    fun lifetimeAccessShowsLifetimeMessage() = runTest {
        val repository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.lifetime(), emptySet(), NOW))
        val model = viewModel(repository)

        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(model.uiState.value.access.isPlus)
        assertTrue(model.uiState.value.renewalInfo != null)
    }

    @Test
    fun manageSubscriptionOpensCustomerCenterWhenProviderIsReady() = runTest {
        val repository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.lifetime(), emptySet(), NOW))
        repository.setReady(true)
        val model = viewModel(repository)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        model.events.test {
            model.onAction(FamilyPlusManagementAction.ManageSubscriptionClicked)
            assertEquals(FamilyPlusManagementEvent.OpenCustomerCenter, awaitItem())
        }
    }

    @Test
    fun manageSubscriptionShowsMessageWhenProviderIsNotReady() = runTest {
        val repository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.lifetime(), emptySet(), NOW))
        repository.setReady(false)
        val model = viewModel(repository)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(FamilyPlusManagementAction.ManageSubscriptionClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(model.uiState.value.message != null)
    }

    @Test
    fun providerNotReadyMarksCustomerCenterUnavailable() = runTest {
        val repository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        repository.setReady(false)
        val model = viewModel(repository)

        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(model.uiState.value.customerCenterAvailable)
    }

    @Test
    fun viewPlansClickedEmitsOpenPaywallEvent() = runTest {
        val repository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val model = viewModel(repository)

        model.events.test {
            model.onAction(FamilyPlusManagementAction.ViewPlansClicked)
            assertEquals(FamilyPlusManagementEvent.OpenPaywall, awaitItem())
        }
    }

    @Test
    fun restoreWithEligiblePurchaseUpdatesAccessAndShowsRestoredMessage() = runTest {
        val repository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        repository.setRestoreResult(RestoreResult.Success(AccessSnapshot(FamilyAccess.lifetime(), emptySet(), NOW)))
        val model = viewModel(repository)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(FamilyPlusManagementAction.RestoreClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(model.uiState.value.access.isPlus)
        assertFalse(model.uiState.value.isRestoring)
        assertTrue(model.uiState.value.message != null)
    }

    @Test
    fun restoreWithNoEligiblePurchaseShowsMessage() = runTest {
        val repository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        repository.setRestoreResult(RestoreResult.Success(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)))
        val model = viewModel(repository)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(FamilyPlusManagementAction.RestoreClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(model.uiState.value.access.isPlus)
        assertTrue(model.uiState.value.message != null)
    }

    @Test
    fun restoreFailureShowsErrorMessage() = runTest {
        val repository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        repository.setRestoreResult(RestoreResult.Failure(PurchaseError.NetworkProblem))
        val model = viewModel(repository)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(FamilyPlusManagementAction.RestoreClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(model.uiState.value.isRestoring)
        assertTrue(model.uiState.value.message != null)
    }

    // -- Analytics --------------------------------------------------------------------------

    @Test
    fun successfulRestoreCapturesStartedThenSuccessOutcome() = runTest {
        val repository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        repository.setRestoreResult(RestoreResult.Success(AccessSnapshot(FamilyAccess.lifetime(), emptySet(), NOW)))
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(repository, analytics)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(FamilyPlusManagementAction.RestoreClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(analytics.capturedEvents.any { it == RestoreStarted(AnalyticsScreen.FAMILY_PLUS_MANAGEMENT) })
        val outcome = analytics.capturedEvents.single { it is RestoreOutcome } as RestoreOutcome
        assertEquals(RestoreOutcomeResult.SUCCESS, outcome.result)
    }

    @Test
    fun restoreWithNoEligiblePurchaseCapturesNoPurchasesOutcome() = runTest {
        val repository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        repository.setRestoreResult(RestoreResult.Success(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)))
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(repository, analytics)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(FamilyPlusManagementAction.RestoreClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val outcome = analytics.capturedEvents.single { it is RestoreOutcome } as RestoreOutcome
        assertEquals(RestoreOutcomeResult.NO_PURCHASES, outcome.result)
    }

    @Test
    fun restoreFailureCapturesFailureOutcome() = runTest {
        val repository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        repository.setRestoreResult(RestoreResult.Failure(PurchaseError.NetworkProblem))
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(repository, analytics)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(FamilyPlusManagementAction.RestoreClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val outcome = analytics.capturedEvents.single { it is RestoreOutcome } as RestoreOutcome
        assertEquals(RestoreOutcomeResult.NETWORK_ERROR, outcome.result)
    }

    @Test
    fun noEventsAreCapturedWithoutConsent() = runTest {
        val repository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        repository.setRestoreResult(RestoreResult.Success(AccessSnapshot(FamilyAccess.lifetime(), emptySet(), NOW)))
        val analytics = FakeProductAnalytics()
        val model = viewModel(repository, analytics)

        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(FamilyPlusManagementAction.RestoreClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(analytics.capturedEvents.isEmpty())
    }
}

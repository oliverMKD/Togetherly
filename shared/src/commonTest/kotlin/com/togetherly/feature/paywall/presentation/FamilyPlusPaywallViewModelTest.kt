package com.togetherly.feature.paywall.presentation

import app.cash.turbine.test
import com.togetherly.core.error.AppError
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.FakeProductAnalytics
import com.togetherly.core.telemetry.PaywallDismissed
import com.togetherly.core.telemetry.PaywallPresented
import com.togetherly.core.telemetry.PurchaseOutcome
import com.togetherly.core.telemetry.PurchaseOutcomeResult
import com.togetherly.core.telemetry.PurchaseStarted
import com.togetherly.core.telemetry.RestoreOutcome
import com.togetherly.core.telemetry.RestoreOutcomeResult
import com.togetherly.core.telemetry.RestoreStarted
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.BillingPeriod
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.ProductId
import com.togetherly.domain.purchase.PurchaseError
import com.togetherly.domain.purchase.PurchasePackage
import com.togetherly.domain.purchase.PurchasePackageType
import com.togetherly.domain.purchase.PurchaseResult
import com.togetherly.domain.purchase.RestoreResult
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import com.togetherly.domain.purchase.usecase.PurchaseFamilyPlus
import com.togetherly.domain.purchase.usecase.RestoreFamilyPlus
import com.togetherly.feature.paywall.model.PaywallContext
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
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")

private val monthlyPackage = PurchasePackage(
    productId = ProductId("togetherly_monthly"),
    type = PurchasePackageType.MONTHLY,
    title = "Monthly",
    formattedPrice = "$4.99",
    billingPeriod = BillingPeriod.MONTH,
    offeringIdentifier = "default",
)

private val annualPackage = PurchasePackage(
    productId = ProductId("togetherly_annual"),
    type = PurchasePackageType.ANNUAL,
    title = "Annual",
    formattedPrice = "$39.99",
    billingPeriod = BillingPeriod.YEAR,
    offeringIdentifier = "default",
)

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyPlusPaywallViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun freeSnapshot() = AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)

    private fun viewModel(
        repository: FakeEntitlementRepository,
        context: PaywallContext = PaywallContext.FAMILY_PLUS_MANAGEMENT,
        analytics: FakeProductAnalytics = FakeProductAnalytics().apply { setCollectionEnabled(true) },
    ) = FamilyPlusPaywallViewModel(
        context = context,
        entitlementRepository = repository,
        purchaseFamilyPlus = PurchaseFamilyPlus(repository),
        restoreFamilyPlus = RestoreFamilyPlus(repository),
        analytics = analytics,
    )

    @Test
    fun loadingPackagesDefaultsSelectionToAnnualWhenAvailable() = runTest {
        val repository = FakeEntitlementRepository(freeSnapshot()).apply {
            setPackages(listOf(monthlyPackage, annualPackage))
        }
        val model = viewModel(repository)

        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(annualPackage.productId, model.uiState.value.selectedPackageId)
        assertFalse(model.uiState.value.isLoading)
    }

    @Test
    fun loadingPackagesDefaultsToFirstPackageWhenNoAnnual() = runTest {
        val repository = FakeEntitlementRepository(freeSnapshot()).apply {
            setPackages(listOf(monthlyPackage))
        }
        val model = viewModel(repository)

        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(monthlyPackage.productId, model.uiState.value.selectedPackageId)
    }

    @Test
    fun packageLoadFailureSurfacesAsError() = runTest {
        val repository = FakeEntitlementRepository(freeSnapshot()).apply {
            setPackagesError(AppError.Purchase(PurchaseError.ConfigurationProblem))
        }
        val model = viewModel(repository)

        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(model.uiState.value.packages.isEmpty())
        assertEquals(false, model.uiState.value.isLoading)
    }

    @Test
    fun successfulPurchaseUpdatesAccessAndEmitsEvent() = runTest {
        val repository = FakeEntitlementRepository(freeSnapshot()).apply {
            setPackages(listOf(monthlyPackage))
            setPurchaseResult(monthlyPackage.productId, PurchaseResult.Success(FamilyAccess.lifetime()))
        }
        val model = viewModel(repository)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(FamilyPlusPaywallAction.PackageSelected(monthlyPackage.productId))

        model.events.test {
            model.onAction(FamilyPlusPaywallAction.PurchaseClicked)
            assertEquals(FamilyPlusPaywallEvent.PurchaseSucceeded, awaitItem())
        }
        assertTrue(model.uiState.value.access.isPlus)
    }

    @Test
    fun cancelledPurchaseShowsGentleMessageNotAnAlarmingOne() = runTest {
        val repository = FakeEntitlementRepository(freeSnapshot()).apply {
            setPackages(listOf(monthlyPackage))
            setPurchaseResult(monthlyPackage.productId, PurchaseResult.Cancelled)
        }
        val model = viewModel(repository)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(FamilyPlusPaywallAction.PackageSelected(monthlyPackage.productId))

        model.onAction(FamilyPlusPaywallAction.PurchaseClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(model.uiState.value.access.isPlus)
        assertFalse(model.uiState.value.isPurchasing)
        assertTrue(model.uiState.value.error != null)
    }

    @Test
    fun pendingPurchaseShowsPendingMessageAndCapturesPendingOutcome() = runTest {
        val repository = FakeEntitlementRepository(freeSnapshot()).apply {
            setPackages(listOf(monthlyPackage))
            setPurchaseResult(monthlyPackage.productId, PurchaseResult.Pending(monthlyPackage.productId))
        }
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(repository, analytics = analytics)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(FamilyPlusPaywallAction.PackageSelected(monthlyPackage.productId))
        analytics.capturedEvents.clear()

        model.onAction(FamilyPlusPaywallAction.PurchaseClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(model.uiState.value.access.isPlus)
        assertFalse(model.uiState.value.isPurchasing)
        assertTrue(model.uiState.value.error != null)
        val outcome = analytics.capturedEvents.single { it is PurchaseOutcome } as PurchaseOutcome
        assertEquals(PurchaseOutcomeResult.PENDING, outcome.result)
    }

    @Test
    fun alreadyOwnedPurchaseShowsAnErrorAndCapturesAlreadyOwnedOutcome() = runTest {
        val repository = FakeEntitlementRepository(freeSnapshot()).apply {
            setPackages(listOf(monthlyPackage))
            setPurchaseResult(monthlyPackage.productId, PurchaseResult.Failure(PurchaseError.AlreadyOwned))
        }
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(repository, analytics = analytics)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(FamilyPlusPaywallAction.PackageSelected(monthlyPackage.productId))
        analytics.capturedEvents.clear()

        model.onAction(FamilyPlusPaywallAction.PurchaseClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(model.uiState.value.access.isPlus)
        assertFalse(model.uiState.value.isPurchasing)
        assertTrue(model.uiState.value.error != null)
        val outcome = analytics.capturedEvents.single { it is PurchaseOutcome } as PurchaseOutcome
        assertEquals(PurchaseOutcomeResult.ALREADY_OWNED, outcome.result)
    }

    @Test
    fun repeatedPurchaseClicksWhileInFlightAreIgnored() = runTest {
        val repository = FakeEntitlementRepository(freeSnapshot()).apply {
            setPackages(listOf(monthlyPackage))
            setPurchaseResult(monthlyPackage.productId, PurchaseResult.Success(FamilyAccess.lifetime()))
        }
        val model = viewModel(repository)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(FamilyPlusPaywallAction.PackageSelected(monthlyPackage.productId))

        model.onAction(FamilyPlusPaywallAction.PurchaseClicked)
        model.onAction(FamilyPlusPaywallAction.PurchaseClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.requestedProductIds.size)
    }

    @Test
    fun restoreWithEligiblePurchaseEmitsRestoreSucceeded() = runTest {
        val repository = FakeEntitlementRepository(freeSnapshot()).apply {
            setPackages(listOf(monthlyPackage))
            setRestoreResult(RestoreResult.Success(AccessSnapshot(FamilyAccess.lifetime(), emptySet(), NOW)))
        }
        val model = viewModel(repository)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        model.events.test {
            model.onAction(FamilyPlusPaywallAction.RestoreClicked)
            assertEquals(FamilyPlusPaywallEvent.RestoreSucceeded, awaitItem())
        }
        assertTrue(model.uiState.value.access.isPlus)
    }

    @Test
    fun restoreWithNoEligiblePurchaseShowsNoPurchasesFoundMessage() = runTest {
        val repository = FakeEntitlementRepository(freeSnapshot()).apply {
            setPackages(listOf(monthlyPackage))
            setRestoreResult(RestoreResult.Success(freeSnapshot()))
        }
        val model = viewModel(repository)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        model.onAction(FamilyPlusPaywallAction.RestoreClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(model.uiState.value.access.isPlus)
        assertFalse(model.uiState.value.isRestoring)
        assertTrue(model.uiState.value.error != null)
    }

    @Test
    fun closeClickedEmitsCloseEvent() = runTest {
        val repository = FakeEntitlementRepository(freeSnapshot())
        val model = viewModel(repository)

        model.events.test {
            model.onAction(FamilyPlusPaywallAction.CloseClicked)
            assertEquals(FamilyPlusPaywallEvent.Close, awaitItem())
        }
    }

    @Test
    fun eachPaywallContextGetsItsOwnDistinctIntroMessageExceptManagement() {
        val repository = FakeEntitlementRepository(freeSnapshot())
        val rerollIntro = viewModel(repository, PaywallContext.PREMIUM_REROLL).uiState.value.introMessage
        val questIntro = viewModel(repository, PaywallContext.PREMIUM_QUEST).uiState.value.introMessage
        val packIntro = viewModel(repository, PaywallContext.PREMIUM_PACK).uiState.value.introMessage
        val managementIntro = viewModel(repository, PaywallContext.FAMILY_PLUS_MANAGEMENT).uiState.value.introMessage

        assertTrue(rerollIntro != null)
        assertTrue(questIntro != null)
        assertTrue(packIntro != null)
        assertEquals(null, managementIntro)
        assertTrue(setOf(rerollIntro, questIntro, packIntro).size == 3)
    }

    @Test
    fun purchasesAlwaysUseTheCurrentOfferingRegardlessOfContext() = runTest {
        // The context only ever changes intro copy — purchasing still resolves the same package
        // catalogue and calls the same PurchaseFamilyPlus use case no matter why the paywall opened.
        val repository = FakeEntitlementRepository(freeSnapshot()).apply {
            setPackages(listOf(monthlyPackage))
            setPurchaseResult(monthlyPackage.productId, PurchaseResult.Success(FamilyAccess.lifetime()))
        }
        val model = viewModel(repository, PaywallContext.PREMIUM_QUEST)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(FamilyPlusPaywallAction.PackageSelected(monthlyPackage.productId))

        model.onAction(FamilyPlusPaywallAction.PurchaseClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(monthlyPackage.productId), repository.requestedProductIds)
        assertTrue(model.uiState.value.access.isPlus)
    }

    // -- Analytics --------------------------------------------------------------------------

    @Test
    fun screenStartedCapturesPaywallPresentedWithTheContext() = runTest {
        val repository = FakeEntitlementRepository(freeSnapshot())
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(repository, PaywallContext.PREMIUM_REROLL, analytics)

        model.onScreenStarted()
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()

        val expected = PaywallPresented(
            context = PaywallContext.PREMIUM_REROLL,
            sourceScreen = AnalyticsScreen.PAYWALL,
            offeringIdentifier = null,
            availablePackageTypes = emptySet(),
        )
        assertEquals(listOf<com.togetherly.core.telemetry.AnalyticsEvent>(expected), analytics.capturedEvents)
    }

    @Test
    fun closeClickedCapturesPaywallDismissed() = runTest {
        val repository = FakeEntitlementRepository(freeSnapshot())
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(repository, PaywallContext.PREMIUM_QUEST, analytics)

        model.onAction(FamilyPlusPaywallAction.CloseClicked)

        val expected = PaywallDismissed(
            context = PaywallContext.PREMIUM_QUEST,
            sourceScreen = AnalyticsScreen.PAYWALL,
            offeringIdentifier = null,
            availablePackageTypes = emptySet(),
        )
        assertTrue(analytics.capturedEvents.any { it == expected })
    }

    @Test
    fun successfulPurchaseCapturesStartedThenSuccessOutcome() = runTest {
        val repository = FakeEntitlementRepository(freeSnapshot()).apply {
            setPackages(listOf(monthlyPackage))
            setPurchaseResult(monthlyPackage.productId, PurchaseResult.Success(FamilyAccess.lifetime()))
        }
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(repository, analytics = analytics)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(FamilyPlusPaywallAction.PackageSelected(monthlyPackage.productId))
        analytics.capturedEvents.clear()

        model.onAction(FamilyPlusPaywallAction.PurchaseClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PurchasePackageType.MONTHLY, (analytics.capturedEvents[0] as PurchaseStarted).packageType)
        val outcome = analytics.capturedEvents[1] as PurchaseOutcome
        assertEquals(PurchaseOutcomeResult.SUCCESS, outcome.result)
        assertEquals(PurchasePackageType.MONTHLY, outcome.packageType)
    }

    @Test
    fun cancelledPurchaseCapturesCancelledOutcome() = runTest {
        val repository = FakeEntitlementRepository(freeSnapshot()).apply {
            setPackages(listOf(monthlyPackage))
            setPurchaseResult(monthlyPackage.productId, PurchaseResult.Cancelled)
        }
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(repository, analytics = analytics)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(FamilyPlusPaywallAction.PackageSelected(monthlyPackage.productId))
        analytics.capturedEvents.clear()

        model.onAction(FamilyPlusPaywallAction.PurchaseClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val outcome = analytics.capturedEvents.single { it is PurchaseOutcome } as PurchaseOutcome
        assertEquals(PurchaseOutcomeResult.CANCELLED, outcome.result)
    }

    @Test
    fun repeatedPurchaseClicksWhileInFlightNeverCaptureTwice() = runTest {
        val repository = FakeEntitlementRepository(freeSnapshot()).apply {
            setPackages(listOf(monthlyPackage))
            setPurchaseResult(monthlyPackage.productId, PurchaseResult.Success(FamilyAccess.lifetime()))
        }
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(repository, analytics = analytics)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(FamilyPlusPaywallAction.PackageSelected(monthlyPackage.productId))
        analytics.capturedEvents.clear()

        model.onAction(FamilyPlusPaywallAction.PurchaseClicked)
        model.onAction(FamilyPlusPaywallAction.PurchaseClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, analytics.capturedEvents.count { it is PurchaseStarted })
        assertEquals(1, analytics.capturedEvents.count { it is PurchaseOutcome })
    }

    @Test
    fun restoreWithEligiblePurchaseCapturesStartedThenSuccessOutcome() = runTest {
        val repository = FakeEntitlementRepository(freeSnapshot()).apply {
            setPackages(listOf(monthlyPackage))
            setRestoreResult(RestoreResult.Success(AccessSnapshot(FamilyAccess.lifetime(), emptySet(), NOW)))
        }
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(repository, analytics = analytics)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        analytics.capturedEvents.clear()

        model.onAction(FamilyPlusPaywallAction.RestoreClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(analytics.capturedEvents.any { it == RestoreStarted(AnalyticsScreen.PAYWALL) })
        val outcome = analytics.capturedEvents.single { it is RestoreOutcome } as RestoreOutcome
        assertEquals(RestoreOutcomeResult.SUCCESS, outcome.result)
    }

    @Test
    fun restoreWithNoEligiblePurchaseCapturesNoPurchasesOutcome() = runTest {
        val repository = FakeEntitlementRepository(freeSnapshot()).apply {
            setPackages(listOf(monthlyPackage))
            setRestoreResult(RestoreResult.Success(freeSnapshot()))
        }
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val model = viewModel(repository, analytics = analytics)
        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        analytics.capturedEvents.clear()

        model.onAction(FamilyPlusPaywallAction.RestoreClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val outcome = analytics.capturedEvents.single { it is RestoreOutcome } as RestoreOutcome
        assertEquals(RestoreOutcomeResult.NO_PURCHASES, outcome.result)
    }

    @Test
    fun noEventsAreCapturedWithoutConsent() = runTest {
        val repository = FakeEntitlementRepository(freeSnapshot()).apply {
            setPackages(listOf(monthlyPackage))
            setPurchaseResult(monthlyPackage.productId, PurchaseResult.Success(FamilyAccess.lifetime()))
        }
        val analytics = FakeProductAnalytics()
        val model = viewModel(repository, analytics = analytics)

        model.onScreenStarted()
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(FamilyPlusPaywallAction.PackageSelected(monthlyPackage.productId))
        model.onAction(FamilyPlusPaywallAction.PurchaseClicked)
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(FamilyPlusPaywallAction.RestoreClicked)
        testDispatcher.scheduler.advanceUntilIdle()
        model.onAction(FamilyPlusPaywallAction.CloseClicked)

        assertTrue(analytics.capturedEvents.isEmpty())
    }
}

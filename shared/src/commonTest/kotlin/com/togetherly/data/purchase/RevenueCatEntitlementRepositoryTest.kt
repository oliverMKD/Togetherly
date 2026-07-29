package com.togetherly.data.purchase

import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.data.local.database.FakeDatabaseMetadataDao
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.AccessSource
import com.togetherly.domain.purchase.BillingPeriod
import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.ProductId
import com.togetherly.domain.purchase.PurchaseError
import com.togetherly.domain.purchase.PurchasePackage
import com.togetherly.domain.purchase.PurchasePackageType
import com.togetherly.domain.purchase.PurchaseResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import app.cash.turbine.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")

/**
 * [RevenueCatEntitlementRepository] owns its own [kotlinx.coroutines.CoroutineScope] (production
 * lives as long as the app process) rather than the test's own [TestScope] — so every test drives
 * it with an [UnconfinedTestDispatcher] sharing the test's `testScheduler`, which runs the
 * cache-load/refresh/observe coroutines launched from `init` eagerly and synchronously (this
 * feature's fakes never truly suspend), rather than needing manual `advanceUntilIdle()` calls.
 *
 * Never mocks RevenueCat's SDK directly — [FakeRevenueCatDataSource] is the one substitute
 * standing in for it, per this feature's own task spec (no `spyk()`).
 */
class RevenueCatEntitlementRepositoryTest {

    private fun snapshot(access: FamilyAccess, verifiedAt: Instant = NOW): AccessSnapshot = AccessSnapshot(
        familyAccess = access,
        activeEntitlements = if (access.isPlus) setOf(EntitlementId(FAMILY_PLUS_ENTITLEMENT_ID)) else emptySet(),
        verifiedAt = verifiedAt,
    )

    private fun TestScope.createRepository(
        dataSource: FakeRevenueCatDataSource,
        cache: EntitlementCache = EntitlementCache(FakeDatabaseMetadataDao()),
        dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler),
    ) = RevenueCatEntitlementRepository(
        dataSource = dataSource,
        cache = cache,
        clock = TestAppClock(NOW),
        dispatchers = TestAppDispatchers(dispatcher),
    )

    @Test
    fun freeCustomerHasNoAccess() = runTest {
        val dataSource = FakeRevenueCatDataSource(initialAccess = snapshot(FamilyAccess.free()))
        val repository = createRepository(dataSource)

        val access = repository.getAccess()

        assertIs<DataResult.Success<AccessSnapshot>>(access)
        assertFalse(access.value.familyAccess.isPlus)
        assertTrue(access.value.activeEntitlements.isEmpty())
    }

    @Test
    fun activeMonthlyEntitlementGrantsAccess() = runTest {
        val expiry = NOW + 30.days
        val dataSource = FakeRevenueCatDataSource(
            initialAccess = snapshot(FamilyAccess.subscription(expiresAt = expiry, willRenew = true)),
        )
        val repository = createRepository(dataSource)

        val access = repository.getAccess()

        assertIs<DataResult.Success<AccessSnapshot>>(access)
        assertTrue(access.value.familyAccess.isPlus)
        assertEquals(AccessSource.SUBSCRIPTION, access.value.familyAccess.source)
        assertEquals(expiry, access.value.familyAccess.expiresAt)
    }

    @Test
    fun activeAnnualEntitlementGrantsAccess() = runTest {
        val expiry = NOW + 365.days
        val dataSource = FakeRevenueCatDataSource(
            initialAccess = snapshot(FamilyAccess.subscription(expiresAt = expiry, willRenew = false)),
        )
        val repository = createRepository(dataSource)

        val access = repository.getAccess()

        assertIs<DataResult.Success<AccessSnapshot>>(access)
        assertTrue(access.value.familyAccess.isPlus)
        assertEquals(AccessSource.SUBSCRIPTION, access.value.familyAccess.source)
        assertEquals(false, access.value.familyAccess.willRenew)
    }

    @Test
    fun activeLifetimeEntitlementGrantsPermanentAccess() = runTest {
        val dataSource = FakeRevenueCatDataSource(initialAccess = snapshot(FamilyAccess.lifetime()))
        val repository = createRepository(dataSource)

        val access = repository.getAccess()

        assertIs<DataResult.Success<AccessSnapshot>>(access)
        assertTrue(access.value.familyAccess.isPlus)
        assertEquals(AccessSource.LIFETIME, access.value.familyAccess.source)
        assertEquals(null, access.value.familyAccess.expiresAt)
    }

    @Test
    fun expiredEntitlementBecomesFreeOnceRefreshSucceeds() = runTest {
        val cache = EntitlementCache(FakeDatabaseMetadataDao())
        cache.save(snapshot(FamilyAccess.subscription(expiresAt = NOW - 1.hours, willRenew = false)))
        // RevenueCat itself has already resolved the now-expired entitlement to free — the
        // repository never re-derives expiry, it only ever trusts what the data source reports.
        val dataSource = FakeRevenueCatDataSource(initialAccess = snapshot(FamilyAccess.free()))
        val repository = createRepository(dataSource, cache = cache)

        val access = repository.getAccess()

        assertIs<DataResult.Success<AccessSnapshot>>(access)
        assertFalse(access.value.familyAccess.isPlus)
    }

    @Test
    fun missingFamilyPlusEntitlementHasNoActiveEntitlements() = runTest {
        val dataSource = FakeRevenueCatDataSource(initialAccess = snapshot(FamilyAccess.free()))
        val repository = createRepository(dataSource)

        val access = repository.getAccess()

        assertIs<DataResult.Success<AccessSnapshot>>(access)
        assertTrue(access.value.activeEntitlements.none { it == EntitlementId(FAMILY_PLUS_ENTITLEMENT_ID) })
    }

    @Test
    fun refreshFailureWithCachedPremiumStateKeepsAccessPremium() = runTest {
        val cache = EntitlementCache(FakeDatabaseMetadataDao())
        cache.save(snapshot(FamilyAccess.lifetime()))
        val dataSource = FakeRevenueCatDataSource(initialAccess = snapshot(FamilyAccess.lifetime()))
        dataSource.setCustomerAccessResult(DataResult.Error(AppError.Purchase(PurchaseError.NetworkProblem)))
        val repository = createRepository(dataSource, cache = cache)

        val access = repository.getAccess()

        assertIs<DataResult.Success<AccessSnapshot>>(access)
        assertTrue(access.value.familyAccess.isPlus)
        assertEquals(AccessSource.LIFETIME, access.value.familyAccess.source)
    }

    @Test
    fun refreshFailureWithoutCachedStateStaysFreeRatherThanCrashing() = runTest {
        val dataSource = FakeRevenueCatDataSource(initialAccess = snapshot(FamilyAccess.free()))
        dataSource.setCustomerAccessResult(DataResult.Error(AppError.Purchase(PurchaseError.NetworkProblem)))
        val repository = createRepository(dataSource)

        val access = repository.getAccess()

        assertIs<DataResult.Success<AccessSnapshot>>(access)
        assertFalse(access.value.familyAccess.isPlus)
    }

    @Test
    fun customerInfoUpdatePushesFreshAccessAndPersistsItToCache() = runTest {
        val cache = EntitlementCache(FakeDatabaseMetadataDao())
        val dataSource = FakeRevenueCatDataSource(initialAccess = snapshot(FamilyAccess.free()))
        val repository = createRepository(dataSource, cache = cache)

        val pushed = snapshot(FamilyAccess.subscription(expiresAt = NOW + 30.days, willRenew = true))
        dataSource.emitCustomerAccessUpdate(pushed)

        val access = repository.getAccess()
        assertIs<DataResult.Success<AccessSnapshot>>(access)
        assertTrue(access.value.familyAccess.isPlus)

        val cached = cache.load()
        assertTrue(cached?.familyAccess?.isPlus == true)
    }

    @Test
    fun partialPackageListStillSurfacesTheLoadedPackages() = runTest {
        val dataSource = FakeRevenueCatDataSource(initialAccess = snapshot(FamilyAccess.free()))
        val onlyMonthly = listOf(
            PurchasePackage(
                productId = ProductId("togetherly_monthly"),
                type = PurchasePackageType.MONTHLY,
                title = "Monthly",
                formattedPrice = "$4.99",
                billingPeriod = BillingPeriod.MONTH,
                offeringIdentifier = "default",
            ),
        )
        dataSource.setPackagesResult(DataResult.Success(onlyMonthly))
        val repository = createRepository(dataSource)

        val result = repository.getPackages()

        assertEquals(DataResult.Success(onlyMonthly), result)
        repository.observePackages().test {
            assertEquals(DataResult.Success(onlyMonthly), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun concurrentPurchaseRequestsForTheSameProductJoinTheSameInFlightAttempt() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val dataSource = FakeRevenueCatDataSource(initialAccess = snapshot(FamilyAccess.free()))
        val productId = ProductId("togetherly_monthly")
        dataSource.setPurchaseResult(productId, PurchaseResult.Success(FamilyAccess.lifetime()))
        val gate = dataSource.holdNextPurchaseUntilReleased()
        val repository = createRepository(dataSource, dispatcher = dispatcher)

        val first = async(dispatcher) { repository.purchase(productId) }
        val second = async(dispatcher) { repository.purchase(productId) }

        // Both callers are suspended (the first inside the fake, the second awaiting the first's
        // shared Deferred) without a second real purchase call ever having been made.
        assertEquals(1, dataSource.purchaseCalls.size)

        gate.complete(Unit)
        val firstResult = first.await()
        val secondResult = second.await()

        assertEquals(firstResult, secondResult)
        assertEquals(1, dataSource.purchaseCalls.size)
    }

    /**
     * Isolates the immediate reset (deliberately never asserts the *final* state, since
     * [clearCacheTriggersABackgroundRefreshFromTheProvider] below already proves the background
     * refresh reinstates real access on its own, and with this test's own eager
     * [UnconfinedTestDispatcher] that refresh can complete before this test would ever observe the
     * reset) by making the provider itself unreachable, so no background refresh can overwrite it.
     */
    @Test
    fun clearCacheResetsInMemoryAccessToFreeAndDeletesThePersistedRow() = runTest {
        val metadataDao = FakeDatabaseMetadataDao()
        val cache = EntitlementCache(metadataDao)
        val dataSource = FakeRevenueCatDataSource(initialAccess = snapshot(FamilyAccess.lifetime()))
        val repository = createRepository(dataSource, cache = cache)
        assertTrue(repository.getAccess().let { it is DataResult.Success && it.value.familyAccess.isPlus })
        dataSource.setCustomerAccessResult(DataResult.Error(AppError.Purchase(PurchaseError.NetworkProblem)))

        val result = repository.clearCache()

        assertEquals(DataResult.Success(Unit), result)
        val access = repository.getAccess()
        assertTrue(access is DataResult.Success && !access.value.familyAccess.isPlus)
        assertEquals(null, cache.load())
    }

    /**
     * Never a subscription cancellation or a RevenueCat `logOut()` — see [EntitlementRepository.clearCache]'s
     * own KDoc. This only re-triggers the normal [FakeRevenueCatDataSource.getCustomerAccess]
     * reconciliation path already used by every other refresh in this class, proving a
     * still-genuinely-entitled family gets its access back automatically — without this call site
     * (or whatever deletion flow triggered it) waiting on it.
     */
    @Test
    fun clearCacheTriggersABackgroundRefreshFromTheProvider() = runTest {
        val dataSource = FakeRevenueCatDataSource(initialAccess = snapshot(FamilyAccess.lifetime()))
        val repository = createRepository(dataSource)
        val callsBeforeClear = dataSource.getCustomerAccessCallCount

        repository.clearCache()

        assertTrue(dataSource.getCustomerAccessCallCount > callsBeforeClear)
        val access = repository.getAccess()
        assertTrue(access is DataResult.Success && access.value.familyAccess.isPlus, "A still-entitled family's access must reappear once the background refresh completes")
    }
}

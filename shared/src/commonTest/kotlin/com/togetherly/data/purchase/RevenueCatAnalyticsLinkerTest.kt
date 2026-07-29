package com.togetherly.data.purchase

import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.logging.FakeAppLogger
import com.togetherly.data.telemetry.FakePostHogSdkAdapter
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.telemetry.ConsentDecision
import com.togetherly.domain.telemetry.TelemetryConsent
import com.togetherly.domain.telemetry.repository.FakeTelemetryConsentRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

private val NOW = Instant.parse("2026-07-25T09:00:00Z")

/**
 * [UnconfinedTestDispatcher] so [RevenueCatAnalyticsLinker]'s own internally-owned
 * [kotlinx.coroutines.CoroutineScope] runs its `observeConsent().collect { ... }` loop eagerly and
 * synchronously within each test body — the same reasoning `TelemetryCoordinatorTest` documents.
 */
class RevenueCatAnalyticsLinkerTest {

    private fun linker(
        consentRepository: FakeTelemetryConsentRepository = FakeTelemetryConsentRepository(),
        dataSource: FakeRevenueCatDataSource = FakeRevenueCatDataSource(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)),
        postHogSdkAdapter: FakePostHogSdkAdapter = FakePostHogSdkAdapter(),
        logger: FakeAppLogger = FakeAppLogger(),
    ) = RevenueCatAnalyticsLinker(
        consentRepository = consentRepository,
        revenueCatDataSource = dataSource,
        postHogSdkAdapter = postHogSdkAdapter,
        logger = logger,
        dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
    )

    @Test
    fun noAssociationHappensBeforeConsentIsGranted() = runTest {
        val dataSource = FakeRevenueCatDataSource(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val linker = linker(dataSource = dataSource)

        linker.start()

        assertTrue(dataSource.postHogDistinctIdCalls.isEmpty())
    }

    @Test
    fun grantingConsentAssociatesThePostHogAnonymousIdWithRevenueCat() = runTest {
        val dataSource = FakeRevenueCatDataSource(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val postHogSdkAdapter = FakePostHogSdkAdapter().apply { anonymousIdValue = "posthog-anon-123" }
        val consentRepository = FakeTelemetryConsentRepository()
        val linker = linker(consentRepository, dataSource, postHogSdkAdapter)
        linker.start()

        consentRepository.setConsent(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.NotAsked))

        assertEquals(listOf<String?>("posthog-anon-123"), dataSource.postHogDistinctIdCalls)
    }

    @Test
    fun alreadyGrantedConsentAtStartupAlsoAssociates() = runTest {
        val dataSource = FakeRevenueCatDataSource(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val postHogSdkAdapter = FakePostHogSdkAdapter().apply { anonymousIdValue = "posthog-anon-456" }
        val consentRepository = FakeTelemetryConsentRepository(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.NotAsked))
        val linker = linker(consentRepository, dataSource, postHogSdkAdapter)

        linker.start()

        assertEquals(listOf<String?>("posthog-anon-456"), dataSource.postHogDistinctIdCalls)
    }

    @Test
    fun revokingConsentAfterGrantingClearsTheAttribute() = runTest {
        val dataSource = FakeRevenueCatDataSource(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val consentRepository = FakeTelemetryConsentRepository()
        val linker = linker(consentRepository, dataSource)
        linker.start()
        consentRepository.setConsent(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.NotAsked))

        consentRepository.setConsent(TelemetryConsent(ConsentDecision.Denied, ConsentDecision.NotAsked))

        assertEquals(listOf<String?>("test-anonymous-id", null), dataSource.postHogDistinctIdCalls)
    }

    @Test
    fun neverGrantedConsentNeverClears() = runTest {
        val dataSource = FakeRevenueCatDataSource(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val consentRepository = FakeTelemetryConsentRepository()
        val linker = linker(consentRepository, dataSource)
        linker.start()

        consentRepository.setConsent(TelemetryConsent(ConsentDecision.Denied, ConsentDecision.NotAsked))

        assertTrue(dataSource.postHogDistinctIdCalls.isEmpty())
    }

    @Test
    fun consentRevocationNeverTouchesEntitlementAccess() = runTest {
        val dataSource = FakeRevenueCatDataSource(AccessSnapshot(FamilyAccess.lifetime(), emptySet(), NOW))
        val consentRepository = FakeTelemetryConsentRepository()
        val linker = linker(consentRepository, dataSource)
        linker.start()
        consentRepository.setConsent(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.NotAsked))

        consentRepository.setConsent(TelemetryConsent(ConsentDecision.Denied, ConsentDecision.NotAsked))

        val access = dataSource.getCustomerAccess()
        assertEquals(FamilyAccess.lifetime(), (access as com.togetherly.core.result.DataResult.Success).value.familyAccess)
    }

    @Test
    fun aMissingAnonymousIdNeverCallsTheDataSource() = runTest {
        val dataSource = FakeRevenueCatDataSource(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val postHogSdkAdapter = FakePostHogSdkAdapter().apply { anonymousIdValue = null }
        val consentRepository = FakeTelemetryConsentRepository()
        val linker = linker(consentRepository, dataSource, postHogSdkAdapter)
        linker.start()

        consentRepository.setConsent(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.NotAsked))

        assertTrue(dataSource.postHogDistinctIdCalls.isEmpty())
    }

    @Test
    fun aRevenueCatFailureDuringAssociationNeverThrowsAndIsLogged() = runTest {
        val dataSource = object : RevenueCatDataSource by FakeRevenueCatDataSource(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)) {
            override fun setPostHogDistinctId(distinctId: String?) {
                throw IllegalStateException("RevenueCat SDK misbehaving")
            }
        }
        val consentRepository = FakeTelemetryConsentRepository()
        val logger = FakeAppLogger()
        val linker = RevenueCatAnalyticsLinker(
            consentRepository = consentRepository,
            revenueCatDataSource = dataSource,
            postHogSdkAdapter = FakePostHogSdkAdapter(),
            logger = logger,
            dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
        )
        linker.start()

        // Must not throw.
        consentRepository.setConsent(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.NotAsked))

        assertTrue(logger.calls.any { it.level == "warn" })
    }
}

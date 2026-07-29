package com.togetherly.data.telemetry

import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.logging.FakeAppLogger
import com.togetherly.data.local.database.FakeDatabaseMetadataDao
import com.togetherly.domain.telemetry.ConsentDecision
import com.togetherly.domain.telemetry.TelemetryConsent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Uses [UnconfinedTestDispatcher] the same reason
 * `com.togetherly.data.purchase.RevenueCatEntitlementRepositoryTest` does — this repository owns
 * its own long-lived [kotlinx.coroutines.CoroutineScope], so eager/synchronous execution avoids
 * needing manual `advanceUntilIdle()` calls for its `init`-time cache load.
 */
class DefaultTelemetryConsentRepositoryTest {

    private fun repository(metadataDao: FakeDatabaseMetadataDao = FakeDatabaseMetadataDao()) = DefaultTelemetryConsentRepository(
        cache = TelemetryConsentCache(metadataDao),
        logger = FakeAppLogger(),
        dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
    )

    @Test
    fun defaultConsentIsNotAskedForBoth() = runTest {
        val repository = repository()

        assertEquals(TelemetryConsent.default(), repository.currentConsent())
    }

    @Test
    fun grantingAnalyticsOnlyLeavesDiagnosticsUntouched() = runTest {
        val repository = repository()

        repository.updateAnalyticsConsent(ConsentDecision.Granted)

        assertEquals(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.NotAsked), repository.currentConsent())
    }

    @Test
    fun grantingDiagnosticsOnlyLeavesAnalyticsUntouched() = runTest {
        val repository = repository()

        repository.updateDiagnosticsConsent(ConsentDecision.Granted)

        assertEquals(TelemetryConsent(ConsentDecision.NotAsked, ConsentDecision.Granted), repository.currentConsent())
    }

    @Test
    fun grantingBothIndependently() = runTest {
        val repository = repository()

        repository.updateAnalyticsConsent(ConsentDecision.Granted)
        repository.updateDiagnosticsConsent(ConsentDecision.Granted)

        assertEquals(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.Granted), repository.currentConsent())
    }

    @Test
    fun denyingBoth() = runTest {
        val repository = repository()

        repository.updateAnalyticsConsent(ConsentDecision.Denied)
        repository.updateDiagnosticsConsent(ConsentDecision.Denied)

        assertEquals(TelemetryConsent(ConsentDecision.Denied, ConsentDecision.Denied), repository.currentConsent())
    }

    @Test
    fun revokingAnalyticsAfterGrantingLeavesDiagnosticsUnaffected() = runTest {
        val repository = repository()
        repository.updateAnalyticsConsent(ConsentDecision.Granted)
        repository.updateDiagnosticsConsent(ConsentDecision.Granted)

        repository.updateAnalyticsConsent(ConsentDecision.Denied)

        assertEquals(TelemetryConsent(ConsentDecision.Denied, ConsentDecision.Granted), repository.currentConsent())
    }

    @Test
    fun revokingDiagnosticsAfterGrantingLeavesAnalyticsUnaffected() = runTest {
        val repository = repository()
        repository.updateAnalyticsConsent(ConsentDecision.Granted)
        repository.updateDiagnosticsConsent(ConsentDecision.Granted)

        repository.updateDiagnosticsConsent(ConsentDecision.Denied)

        assertEquals(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.Denied), repository.currentConsent())
    }

    @Test
    fun updatesArePersistedAndSurviveANewRepositoryInstance() = runTest {
        val metadataDao = FakeDatabaseMetadataDao()
        val first = repository(metadataDao)
        first.updateAnalyticsConsent(ConsentDecision.Granted)
        first.updateDiagnosticsConsent(ConsentDecision.Denied)

        val second = repository(metadataDao)

        assertEquals(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.Denied), second.currentConsent())
    }

    @Test
    fun resetConsentReturnsToDefaultAndClearsThePersistedRow() = runTest {
        val metadataDao = FakeDatabaseMetadataDao()
        val first = repository(metadataDao)
        first.updateAnalyticsConsent(ConsentDecision.Granted)
        first.updateDiagnosticsConsent(ConsentDecision.Granted)

        first.resetConsent()

        assertEquals(TelemetryConsent.default(), first.currentConsent())
        val second = repository(metadataDao)
        assertEquals(TelemetryConsent.default(), second.currentConsent())
    }
}

private suspend fun DefaultTelemetryConsentRepository.currentConsent(): TelemetryConsent = observeConsent().first()

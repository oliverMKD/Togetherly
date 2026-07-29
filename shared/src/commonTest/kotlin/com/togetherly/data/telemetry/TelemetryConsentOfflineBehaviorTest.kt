package com.togetherly.data.telemetry

import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.logging.FakeAppLogger
import com.togetherly.data.local.database.DatabaseMetadataEntity
import com.togetherly.data.local.database.FakeDatabaseMetadataDao
import com.togetherly.domain.telemetry.ConsentDecision
import com.togetherly.domain.telemetry.TelemetryConsent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Step 14.6's offline-behavior suite for the consent side of telemetry: [DefaultTelemetryConsentRepository]
 * is backed entirely by [TelemetryConsentCache] — Room-backed local storage, never a network call
 * of any kind — so "offline" here means the same thing [DefaultTelemetryConsentRepository]'s own
 * KDoc already promises: a *local storage* I/O failure (the on-device equivalent of "the network is
 * unavailable" for a component with no network dependency to begin with) must never propagate to a
 * caller, and the in-memory consent state every `ProductAnalytics`/`OperationalDiagnostics` gating
 * decision reads must stay correct regardless of whether the persisted copy could be written.
 *
 * See [com.togetherly.core.telemetry.TelemetryOfflineBehaviorTest] for the analytics/diagnostics
 * provider half of this same Step 14.6 requirement.
 */
class TelemetryConsentOfflineBehaviorTest {

    private fun repository(metadataDao: FakeDatabaseMetadataDao) = DefaultTelemetryConsentRepository(
        cache = TelemetryConsentCache(metadataDao),
        logger = FakeAppLogger(),
        dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
    )

    @Test
    fun grantingConsentSucceedsImmediatelyEvenWhenPersistingItFails() = runTest {
        val dao = FakeDatabaseMetadataDao().apply { throwOnNextSet(IllegalStateException("storage unavailable")) }
        val repository = repository(dao)

        // Must not throw, and the in-memory/observable state must reflect the change immediately —
        // a family granting/revoking consent is never blocked on storage succeeding.
        repository.updateAnalyticsConsent(ConsentDecision.Granted)

        assertEquals(ConsentDecision.Granted, repository.observeConsent().first().analytics)
    }

    @Test
    fun revokingConsentSucceedsImmediatelyEvenWhenPersistingItFails() = runTest {
        val dao = FakeDatabaseMetadataDao()
        val repository = repository(dao)
        repository.updateAnalyticsConsent(ConsentDecision.Granted)
        dao.throwOnNextSet(IllegalStateException("storage unavailable"))

        repository.updateAnalyticsConsent(ConsentDecision.Denied)

        assertEquals(ConsentDecision.Denied, repository.observeConsent().first().analytics)
    }

    @Test
    fun resetConsentSucceedsImmediatelyEvenWhenClearingTheStoredRowFails() = runTest {
        val dao = FakeDatabaseMetadataDao()
        val repository = repository(dao)
        repository.updateAnalyticsConsent(ConsentDecision.Granted)
        dao.throwOnNextDelete(IllegalStateException("storage unavailable"))

        repository.resetConsent()

        assertEquals(TelemetryConsent.default(), repository.observeConsent().first())
    }

    @Test
    fun aNewRepositoryInstanceFallsBackToDefaultConsentWhenLoadingThePersistedRowFails() = runTest {
        val dao = FakeDatabaseMetadataDao()
        // Something was genuinely persisted by a prior app run...
        repository(dao).updateAnalyticsConsent(ConsentDecision.Granted)
        // ...but this run's load fails (a corrupt row, a storage error) — never surfaced as a crash,
        // and never silently treated as Granted; falls back to the same safe default a fresh install gets.
        dao.throwOnNextGetValue(IllegalStateException("storage unavailable"))

        val freshRepository = repository(dao)

        assertEquals(TelemetryConsent.default(), freshRepository.observeConsent().first())
    }

    @Test
    fun corruptedPersistedJsonFallsBackToDefaultConsentRatherThanThrowing() = runTest {
        val dao = FakeDatabaseMetadataDao()
        // "telemetry_consent" mirrors TelemetryConsentCache's own private METADATA_KEY constant.
        dao.set(DatabaseMetadataEntity(key = "telemetry_consent", value = "{not valid json"))

        val repository = repository(dao)

        assertEquals(TelemetryConsent.default(), repository.observeConsent().first())
    }
}

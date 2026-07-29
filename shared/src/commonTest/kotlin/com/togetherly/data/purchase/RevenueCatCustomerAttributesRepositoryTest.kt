package com.togetherly.data.purchase

import com.togetherly.core.logging.FakeAppLogger
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.telemetry.ConsentDecision
import com.togetherly.domain.telemetry.TelemetryConsent
import com.togetherly.domain.telemetry.repository.FakeTelemetryConsentRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

private val NOW = Instant.parse("2026-07-25T09:00:00Z")

class RevenueCatCustomerAttributesRepositoryTest {

    private fun repository(
        dataSource: FakeRevenueCatDataSource = FakeRevenueCatDataSource(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)),
        consentRepository: FakeTelemetryConsentRepository = FakeTelemetryConsentRepository(),
        logger: FakeAppLogger = FakeAppLogger(),
    ) = RevenueCatCustomerAttributesRepository(
        dataSource = dataSource,
        consentRepository = consentRepository,
        logger = logger,
    )

    @Test
    fun onboardingCompletedIsNeverSentWithoutConsent() = runTest {
        val dataSource = FakeRevenueCatDataSource(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val repository = repository(dataSource)

        repository.markOnboardingCompleted()

        assertTrue(dataSource.customerAttributeCalls.isEmpty())
    }

    @Test
    fun onboardingCompletedIsSentWhenConsentIsGranted() = runTest {
        val dataSource = FakeRevenueCatDataSource(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val consentRepository = FakeTelemetryConsentRepository(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.NotAsked))
        val repository = repository(dataSource, consentRepository)

        repository.markOnboardingCompleted()

        assertEquals(listOf<Map<String, String?>>(mapOf(ATTRIBUTE_ONBOARDING_COMPLETED to "true")), dataSource.customerAttributeCalls)
    }

    @Test
    fun onboardingCompletedIsNeverSentWhenConsentIsDenied() = runTest {
        val dataSource = FakeRevenueCatDataSource(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val consentRepository = FakeTelemetryConsentRepository(TelemetryConsent(ConsentDecision.Denied, ConsentDecision.NotAsked))
        val repository = repository(dataSource, consentRepository)

        repository.markOnboardingCompleted()

        assertTrue(dataSource.customerAttributeCalls.isEmpty())
    }

    @Test
    fun firstQuestCompletedIsIdempotentAcrossRepeatedCalls() = runTest {
        val dataSource = FakeRevenueCatDataSource(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val consentRepository = FakeTelemetryConsentRepository(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.NotAsked))
        val repository = repository(dataSource, consentRepository)

        repository.markFirstQuestCompleted()
        repository.markFirstQuestCompleted()

        assertEquals(2, dataSource.customerAttributeCalls.size)
        assertTrue(dataSource.customerAttributeCalls.all { it == mapOf(ATTRIBUTE_FIRST_QUEST_COMPLETED to "true") })
    }

    @Test
    fun preferredDurationBucketSendsTheLowercasedBandName() = runTest {
        val dataSource = FakeRevenueCatDataSource(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val consentRepository = FakeTelemetryConsentRepository(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.NotAsked))
        val repository = repository(dataSource, consentRepository)

        repository.setPreferredDurationBucket(DurationBand.TEN_MINUTES)

        assertEquals(listOf<Map<String, String?>>(mapOf(ATTRIBUTE_PREFERRED_DURATION_BUCKET to "ten_minutes")), dataSource.customerAttributeCalls)
    }

    @Test
    fun aDataSourceFailureNeverThrowsAndIsLogged() = runTest {
        val dataSource = object : RevenueCatDataSource by FakeRevenueCatDataSource(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)) {
            override fun setCustomerAttributes(attributes: Map<String, String?>) {
                throw IllegalStateException("RevenueCat SDK misbehaving")
            }
        }
        val consentRepository = FakeTelemetryConsentRepository(TelemetryConsent(ConsentDecision.Granted, ConsentDecision.NotAsked))
        val logger = FakeAppLogger()
        val repository = RevenueCatCustomerAttributesRepository(dataSource, consentRepository, logger)

        // Must not throw.
        repository.markOnboardingCompleted()

        assertTrue(logger.calls.any { it.level == "warn" })
    }
}

package com.togetherly.data.telemetry

import com.togetherly.app.application.AppConfiguration
import com.togetherly.app.foundation.PlatformInfoProvider
import com.togetherly.app.foundation.VersionInfoProvider
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.logging.FakeAppLogger
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.AnalyticsValue
import com.togetherly.core.telemetry.PaywallPresented
import com.togetherly.core.telemetry.PurchaseStarted
import com.togetherly.core.telemetry.QuestCompleted
import com.togetherly.core.telemetry.TelemetryEventRegistry
import com.togetherly.core.telemetry.TelemetryEventSchema
import com.togetherly.core.telemetry.TelemetryPrivacyValidator
import com.togetherly.core.telemetry.TestAnalyticsEvent
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.PurchasePackageType
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import com.togetherly.feature.paywall.model.PaywallContext
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

private val NOW = Instant.parse("2026-07-28T09:00:00Z")

private fun testPurchaseStarted(packageType: PurchasePackageType) = PurchaseStarted(
    context = PaywallContext.PREMIUM_REROLL,
    sourceScreen = AnalyticsScreen.PAYWALL,
    packageType = packageType,
    offeringIdentifier = "default",
    availablePackageTypes = setOf(packageType),
)

private fun testPaywallPresented(context: PaywallContext) = PaywallPresented(
    context = context,
    sourceScreen = AnalyticsScreen.PAYWALL,
    offeringIdentifier = "default",
    availablePackageTypes = setOf(PurchasePackageType.MONTHLY),
)

class PostHogProductAnalyticsTest {

    private fun commonProperties(
        entitlementRepository: FakeEntitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)),
    ) = PostHogCommonProperties(
        appConfiguration = AppConfiguration("Togetherly", debug = true),
        versionInfoProvider = object : VersionInfoProvider {
            override fun versionName() = "2.3"
            override fun buildNumber() = "42"
        },
        platformInfoProvider = object : PlatformInfoProvider {
            override fun platformName() = "Android 34"
        },
        entitlementRepository = entitlementRepository,
        dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
    )

    private fun analytics(
        adapter: FakePostHogSdkAdapter = FakePostHogSdkAdapter(),
        logger: FakeAppLogger = FakeAppLogger(),
        validator: TelemetryPrivacyValidator = TelemetryPrivacyValidator.default(),
    ) = PostHogProductAnalytics(
        adapter = adapter,
        validator = validator,
        commonProperties = commonProperties(),
        projectKey = "phc_test_project_key",
        host = "https://eu.i.posthog.com",
        debug = true,
        logger = logger,
    )

    @Test
    fun captureDoesNothingWhileCollectionIsDisabled() {
        val adapter = FakePostHogSdkAdapter()
        val analytics = analytics(adapter)

        analytics.capture(testPurchaseStarted(PurchasePackageType.MONTHLY))

        assertTrue(adapter.captureCalls.isEmpty())
    }

    @Test
    fun screenFlushDoNothingWhileCollectionIsDisabled() {
        val adapter = FakePostHogSdkAdapter()
        val analytics = analytics(adapter)

        analytics.screen(AnalyticsScreen.TODAY)
        analytics.flush()

        assertTrue(adapter.screenCalls.isEmpty())
        assertEquals(0, adapter.flushCallCount)
    }

    @Test
    fun captureSendsTheValidatedEventMergedWithCommonPropertiesWhenEnabled() {
        val adapter = FakePostHogSdkAdapter()
        val analytics = analytics(adapter)
        analytics.setCollectionEnabled(true)

        analytics.capture(testPurchaseStarted(PurchasePackageType.ANNUAL))

        val call = adapter.captureCalls.single()
        assertEquals(PurchaseStarted.EVENT_NAME, call.event)
        assertEquals("annual", call.properties["package_type"])
        assertEquals("android", call.properties["platform"])
        assertEquals("2.3", call.properties["app_version"])
        assertEquals("42", call.properties["build_number"])
    }

    @Test
    fun typedPropertyValuesMapToTheirPlainKotlinEquivalents() {
        val adapter = FakePostHogSdkAdapter()
        val analytics = analytics(adapter)
        analytics.setCollectionEnabled(true)

        analytics.capture(
            QuestCompleted(
                questId = QuestId("quest-1"),
                category = QuestCategory.DISCOVER,
                durationBucket = DurationBand.TEN_MINUTES,
                energyLevel = EnergyLevel.MODERATE,
                accessRequired = QuestAccess.Free,
                usedTimer = false,
                usedPhoneDown = true,
            ),
        )

        val properties = adapter.captureCalls.single().properties
        assertEquals("discover", properties["quest_category"])
        assertEquals("quest-1", properties["quest_id"])
        assertEquals(true, properties["used_phone_down"])
        assertTrue(properties["used_phone_down"] is Boolean)
    }

    @Test
    fun unregisteredEventNeverReachesTheAdapter() {
        val adapter = FakePostHogSdkAdapter()
        val logger = FakeAppLogger()
        val analytics = analytics(adapter, logger)
        analytics.setCollectionEnabled(true)

        analytics.capture(TestAnalyticsEvent(name = "not_a_real_event"))

        assertTrue(adapter.captureCalls.isEmpty())
        assertTrue(logger.calls.any { it.level == "warn" })
    }

    @Test
    fun forbiddenPropertyKeyNeverReachesTheAdapter() {
        val schemaWithForbiddenKey = mapOf(
            "leaky_event" to TelemetryEventSchema("leaky_event", setOf("email")),
        )
        val leakyValidator = TelemetryPrivacyValidator(schemaWithForbiddenKey)
        val adapter = FakePostHogSdkAdapter()
        val analytics = analytics(adapter, validator = leakyValidator)
        analytics.setCollectionEnabled(true)

        analytics.capture(TestAnalyticsEvent(name = "leaky_event", rawProperties = mapOf("email" to AnalyticsValue.Text("a@b.com"))))

        assertTrue(adapter.captureCalls.isEmpty())
    }

    @Test
    fun screenCapturesWithCommonPropertiesWhenEnabled() {
        val adapter = FakePostHogSdkAdapter()
        val analytics = analytics(adapter)
        analytics.setCollectionEnabled(true)

        analytics.screen(AnalyticsScreen.JOURNEY)

        val call = adapter.screenCalls.single()
        assertEquals("JOURNEY", call.screenName)
        assertEquals("android", call.properties["platform"])
    }

    @Test
    fun flushForwardsToTheAdapterWhenEnabled() {
        val adapter = FakePostHogSdkAdapter()
        val analytics = analytics(adapter)
        analytics.setCollectionEnabled(true)

        analytics.flush()

        assertEquals(1, adapter.flushCallCount)
    }

    @Test
    fun setCollectionEnabledTogglesOptInAndOptOut() {
        val adapter = FakePostHogSdkAdapter()
        val analytics = analytics(adapter)

        analytics.setCollectionEnabled(true)
        analytics.setCollectionEnabled(false)

        assertEquals(1, adapter.optInCallCount)
        assertEquals(1, adapter.optOutCallCount)
    }

    @Test
    fun resetForwardsToTheAdapter() {
        val adapter = FakePostHogSdkAdapter()
        val analytics = analytics(adapter)

        analytics.reset()

        assertEquals(1, adapter.resetCallCount)
    }

    @Test
    fun setupFailureLeavesTheInstancePermanentlyInertWithoutThrowing() {
        val adapter = FakePostHogSdkAdapter().apply { throwOnNextSetup(IllegalStateException("SDK misbehaving")) }
        val logger = FakeAppLogger()
        val analytics = analytics(adapter, logger)

        // None of these may throw, and none may reach the adapter beyond the failed setup call.
        analytics.setCollectionEnabled(true)
        analytics.capture(testPurchaseStarted(PurchasePackageType.MONTHLY))
        analytics.screen(AnalyticsScreen.TODAY)
        analytics.flush()
        analytics.reset()

        assertTrue(logger.calls.any { it.level == "error" })
        assertTrue(adapter.captureCalls.isEmpty())
        assertTrue(adapter.screenCalls.isEmpty())
        assertEquals(0, adapter.flushCallCount)
        assertEquals(0, adapter.resetCallCount)
        assertEquals(0, adapter.optInCallCount)
    }

    @Test
    fun captureFailureIsIsolatedAndNeverThrows() {
        val adapter = FakePostHogSdkAdapter().apply { throwOnNextCapture(IllegalStateException("network error")) }
        val logger = FakeAppLogger()
        val analytics = analytics(adapter, logger)
        analytics.setCollectionEnabled(true)

        // Must not throw.
        analytics.capture(testPurchaseStarted(PurchasePackageType.MONTHLY))

        assertTrue(logger.calls.any { it.level == "error" })
    }

    @Test
    fun everyRegisteredEventSchemaEventNameIsAccepted() {
        val adapter = FakePostHogSdkAdapter()
        val analytics = analytics(adapter)
        analytics.setCollectionEnabled(true)

        analytics.capture(testPaywallPresented(PaywallContext.FAMILY_PLUS_MANAGEMENT))

        assertTrue(TelemetryEventRegistry.schemas.containsKey(PaywallPresented.EVENT_NAME))
        assertEquals(1, adapter.captureCalls.size)
    }
}

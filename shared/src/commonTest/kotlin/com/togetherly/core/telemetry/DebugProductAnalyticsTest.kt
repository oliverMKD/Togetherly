package com.togetherly.core.telemetry

import com.togetherly.core.logging.FakeAppLogger
import com.togetherly.domain.purchase.PurchasePackageType
import com.togetherly.feature.paywall.model.PaywallContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun testPurchaseStarted() = PurchaseStarted(
    context = PaywallContext.PREMIUM_REROLL,
    sourceScreen = AnalyticsScreen.PAYWALL,
    packageType = PurchasePackageType.MONTHLY,
    offeringIdentifier = "default",
    availablePackageTypes = setOf(PurchasePackageType.MONTHLY),
)

class DebugProductAnalyticsTest {

    private fun analytics(logger: FakeAppLogger = FakeAppLogger()) = DebugProductAnalytics(logger = logger)

    @Test
    fun noEventIsLoggedBeforeCollectionIsEnabled() {
        val logger = FakeAppLogger()
        val analytics = analytics(logger)

        analytics.capture(testPurchaseStarted())

        assertTrue(logger.calls.isEmpty())
    }

    @Test
    fun aPreConsentCaptureIsNeverRetroactivelyLoggedAfterConsentIsGrantedLater() {
        val logger = FakeAppLogger()
        val analytics = analytics(logger)

        analytics.capture(testPurchaseStarted())
        analytics.setCollectionEnabled(true)

        // No "capture" log at all — the earlier, pre-consent capture was dropped, never queued and replayed once enabled.
        assertEquals(0, logger.calls.count { it.message.contains("capture") })
    }

    @Test
    fun eventIsLoggedAfterConsentIsGranted() {
        val logger = FakeAppLogger()
        val analytics = analytics(logger)
        analytics.setCollectionEnabled(true)

        analytics.capture(testPurchaseStarted())

        assertTrue(logger.calls.any { it.level == "debug" && it.message.contains("purchase_started") })
    }

    @Test
    fun disablingCollectionAfterEnablingStopsFurtherCaptures() {
        val logger = FakeAppLogger()
        val analytics = analytics(logger)
        analytics.setCollectionEnabled(true)
        analytics.setCollectionEnabled(false)
        logger.calls.clear()

        analytics.capture(testPurchaseStarted())

        assertTrue(logger.calls.isEmpty())
    }

    @Test
    fun unregisteredEventIsRejectedAndOnlyLogsAWarningReasonNeverTheValue() {
        val logger = FakeAppLogger()
        val analytics = analytics(logger)
        analytics.setCollectionEnabled(true)
        val unregisteredEvent = TestAnalyticsEvent(name = "not_a_real_event")

        analytics.capture(unregisteredEvent)

        val warnCall = logger.calls.single { it.level == "warn" }
        assertTrue(warnCall.message.contains("Rejected"))
        assertTrue(warnCall.message.contains(unregisteredEvent.name), "The event name itself is safe to log — only property values are never logged on rejection")
        assertEquals(0, logger.calls.count { it.message.contains("capture") }, "A rejected event must never also be printed as if it were accepted")
    }

    @Test
    fun screenAndFlushRespectCollectionEnabled() {
        val logger = FakeAppLogger()
        val analytics = analytics(logger)

        analytics.screen(AnalyticsScreen.TODAY)
        analytics.flush()
        assertTrue(logger.calls.none { it.message.contains("screen") || it.message == "flush" })

        analytics.setCollectionEnabled(true)
        analytics.screen(AnalyticsScreen.TODAY)
        analytics.flush()
        assertTrue(logger.calls.any { it.message.contains("screen") })
        assertTrue(logger.calls.any { it.message == "flush" })
    }
}

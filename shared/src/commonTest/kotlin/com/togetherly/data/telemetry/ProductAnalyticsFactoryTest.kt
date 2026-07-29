package com.togetherly.data.telemetry

import com.togetherly.app.application.AppConfiguration
import com.togetherly.app.foundation.PlatformInfoProvider
import com.togetherly.app.foundation.VersionInfoProvider
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.logging.FakeAppLogger
import com.togetherly.core.telemetry.NoOpProductAnalytics
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Instant

private val NOW = Instant.parse("2026-07-28T09:00:00Z")

class ProductAnalyticsFactoryTest {

    @Test
    fun blankProjectKeyFallsBackToNoOp() {
        val logger = FakeAppLogger()

        val analytics = createProductAnalytics(
            projectKey = "",
            host = null,
            debug = false,
            logger = logger,
            adapter = { fail("A missing project key must never construct a real PostHog adapter") },
            commonProperties = { fail("A missing project key must never construct common properties") },
        )

        assertIs<NoOpProductAnalytics>(analytics)
    }

    @Test
    fun nullProjectKeyFallsBackToNoOp() {
        val analytics = createProductAnalytics(
            projectKey = null,
            host = null,
            debug = false,
            logger = FakeAppLogger(),
            adapter = { fail("A missing project key must never construct a real PostHog adapter") },
            commonProperties = { fail("A missing project key must never construct common properties") },
        )

        assertIs<NoOpProductAnalytics>(analytics)
    }

    @Test
    fun missingProjectKeyInDebugLogsAClearWarning() {
        val logger = FakeAppLogger()

        createProductAnalytics(
            projectKey = null,
            host = null,
            debug = true,
            logger = logger,
            adapter = { fail("unreachable") },
            commonProperties = { fail("unreachable") },
        )

        assertTrue(logger.calls.any { it.level == "warn" && it.message.contains("PostHog", ignoreCase = false) })
    }

    @Test
    fun missingProjectKeyInReleaseStaysSilent() {
        val logger = FakeAppLogger()

        createProductAnalytics(
            projectKey = null,
            host = null,
            debug = false,
            logger = logger,
            adapter = { fail("unreachable") },
            commonProperties = { fail("unreachable") },
        )

        assertTrue(logger.calls.isEmpty())
    }

    @Test
    fun presentProjectKeyConstructsTheRealPostHogAdapter() {
        val adapter = FakePostHogSdkAdapter()
        var commonPropertiesConstructed = false

        val analytics = createProductAnalytics(
            projectKey = "phc_test_project_key",
            host = "https://eu.i.posthog.com",
            debug = true,
            logger = FakeAppLogger(),
            adapter = { adapter },
            commonProperties = {
                commonPropertiesConstructed = true
                PostHogCommonProperties(
                    appConfiguration = AppConfiguration("Togetherly", debug = true),
                    versionInfoProvider = object : VersionInfoProvider {
                        override fun versionName() = "1.0"
                        override fun buildNumber() = "1"
                    },
                    platformInfoProvider = object : PlatformInfoProvider {
                        override fun platformName() = "Android 34"
                    },
                    entitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)),
                    dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
                )
            },
        )

        assertIs<PostHogProductAnalytics>(analytics)
        assertTrue(commonPropertiesConstructed)
        assertTrue(adapter.setupCalls.isNotEmpty(), "A present project key must call adapter.setup exactly once")
    }
}

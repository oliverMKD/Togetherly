package com.togetherly.data.telemetry

import com.togetherly.app.application.AppConfiguration
import com.togetherly.app.foundation.PlatformInfoProvider
import com.togetherly.app.foundation.VersionInfoProvider
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
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

private val NOW = Instant.parse("2026-07-28T09:00:00Z")

private class FakeVersionInfoProvider(
    private val versionName: String = "1.0",
    private val buildNumber: String = "1",
) : VersionInfoProvider {
    override fun versionName(): String = versionName
    override fun buildNumber(): String = buildNumber
}

private class FakePlatformInfoProvider(private val name: String) : PlatformInfoProvider {
    override fun platformName(): String = name
}

/** Proves the exact five safe properties every event/screen view carries — see this app's own forbidden-content list in `docs/telemetry.md`. */
@OptIn(ExperimentalCoroutinesApi::class)
class PostHogCommonPropertiesTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun properties(
        appConfiguration: AppConfiguration = AppConfiguration(applicationName = "Togetherly", debug = true),
        versionInfoProvider: VersionInfoProvider = FakeVersionInfoProvider(),
        platformInfoProvider: PlatformInfoProvider = FakePlatformInfoProvider("Android 34"),
        entitlementRepository: FakeEntitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)),
    ) = PostHogCommonProperties(
        appConfiguration = appConfiguration,
        versionInfoProvider = versionInfoProvider,
        platformInfoProvider = platformInfoProvider,
        entitlementRepository = entitlementRepository,
        dispatchers = TestAppDispatchers(testDispatcher),
    )

    @Test
    fun exposesExactlyTheFiveSafeKeysAndNothingElse() {
        val current = properties().current()

        assertEquals(setOf("app_version", "build_number", "platform", "environment", "access_state"), current.keys)
    }

    @Test
    fun neverExposesAnyForbiddenKey() {
        val current = properties().current()
        val forbidden = setOf(
            "family_profile_id", "revenuecat_app_user_id", "device_model", "advertising_id",
            "ip", "ip_address", "locale",
        )

        assertTrue(forbidden.none { it in current })
    }

    @Test
    fun mapsPlatformNameToALowercaseAndroidLabel() {
        val current = properties(platformInfoProvider = FakePlatformInfoProvider("Android 34")).current()

        assertEquals("android", current["platform"])
    }

    @Test
    fun mapsPlatformNameToALowercaseIosLabel() {
        val current = properties(platformInfoProvider = FakePlatformInfoProvider("iOS 18.2")).current()

        assertEquals("ios", current["platform"])
    }

    @Test
    fun mapsDebugConfigurationToDebugEnvironment() {
        val current = properties(appConfiguration = AppConfiguration(applicationName = "Togetherly", debug = true)).current()

        assertEquals("debug", current["environment"])
    }

    @Test
    fun mapsReleaseConfigurationToReleaseEnvironment() {
        val current = properties(appConfiguration = AppConfiguration(applicationName = "Togetherly", debug = false)).current()

        assertEquals("release", current["environment"])
    }

    @Test
    fun accessStateStartsUnknownBeforeStart() {
        val current = properties().current()

        assertEquals(ACCESS_STATE_UNKNOWN, current["access_state"])
    }

    @Test
    fun accessStateBecomesFreeAfterStartForAFreeFamily() = runTest {
        val entitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val properties = properties(entitlementRepository = entitlementRepository)

        properties.start()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ACCESS_STATE_FREE, properties.current()["access_state"])
    }

    @Test
    fun accessStateBecomesFamilyPlusAfterStartForAPremiumFamily() = runTest {
        val entitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.lifetime(), emptySet(), NOW))
        val properties = properties(entitlementRepository = entitlementRepository)

        properties.start()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ACCESS_STATE_FAMILY_PLUS, properties.current()["access_state"])
    }

    @Test
    fun accessStateIsOnlyEverOneOfTheThreeBroadValues() = runTest {
        val entitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.lifetime(), emptySet(), NOW))
        val properties = properties(entitlementRepository = entitlementRepository)
        properties.start()
        testDispatcher.scheduler.advanceUntilIdle()

        val accessState = properties.current()["access_state"]

        assertTrue(accessState in setOf(ACCESS_STATE_FREE, ACCESS_STATE_FAMILY_PLUS, ACCESS_STATE_UNKNOWN))
    }
}

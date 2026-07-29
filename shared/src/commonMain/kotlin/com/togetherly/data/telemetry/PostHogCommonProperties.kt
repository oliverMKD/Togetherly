package com.togetherly.data.telemetry

import com.togetherly.app.application.AppConfiguration
import com.togetherly.app.foundation.PlatformInfoProvider
import com.togetherly.app.foundation.VersionInfoProvider
import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.result.DataResult
import com.togetherly.domain.purchase.repository.EntitlementRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile

internal const val ACCESS_STATE_FREE = "free"
internal const val ACCESS_STATE_FAMILY_PLUS = "family_plus"
internal const val ACCESS_STATE_UNKNOWN = "unknown"

/**
 * The exact five safe, low-cardinality properties attached to every event/screen view this app
 * sends — never a family profile id, a RevenueCat App User ID, an exact device model, an
 * advertising id, an IP address, or a fine-grained locale. `app_version`/`build_number`/`platform`/
 * `environment` never change during a process lifetime, so they're computed once; `access_state`
 * can (a family can purchase/restore/lose Family Plus mid-session), so it's kept current via
 * [EntitlementRepository.observeAccess] on this class's own long-lived [scope] — the same
 * "seed a safe default, update async" shape [com.togetherly.data.purchase.RevenueCatEntitlementRepository]
 * already uses elsewhere. `access_state` only ever takes one of exactly three values — never an
 * exact entitlement id, expiry date, or billing period.
 */
internal class PostHogCommonProperties(
    appConfiguration: AppConfiguration,
    versionInfoProvider: VersionInfoProvider,
    platformInfoProvider: PlatformInfoProvider,
    private val entitlementRepository: EntitlementRepository,
    dispatchers: AppDispatchers,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    private val staticProperties: Map<String, Any> = mapOf(
        "app_version" to versionInfoProvider.versionName(),
        "build_number" to versionInfoProvider.buildNumber(),
        "platform" to platformInfoProvider.platformName().toPlatformLabel(),
        "environment" to if (appConfiguration.debug) "debug" else "release",
    )

    @Volatile
    private var accessState: String = ACCESS_STATE_UNKNOWN

    fun start() {
        scope.launch {
            entitlementRepository.observeAccess().collect { result ->
                accessState = when (result) {
                    is DataResult.Success -> if (result.value.familyAccess.isPlus) ACCESS_STATE_FAMILY_PLUS else ACCESS_STATE_FREE
                    is DataResult.Error -> ACCESS_STATE_UNKNOWN
                }
            }
        }
    }

    fun current(): Map<String, Any> = staticProperties + ("access_state" to accessState)
}

private fun String.toPlatformLabel(): String = if (startsWith("Android", ignoreCase = true)) "android" else "ios"

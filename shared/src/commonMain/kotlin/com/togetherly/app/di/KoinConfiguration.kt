package com.togetherly.app.di

import com.togetherly.app.application.AppConfiguration
import com.togetherly.core.logging.AppLogger
import com.togetherly.core.telemetry.TelemetryCoordinator
import com.togetherly.data.purchase.RevenueCatAnalyticsLinker
import com.togetherly.data.purchase.RevenueCatConfigurator
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.Module

private const val TAG = "KoinConfiguration"

/**
 * [RevenueCatConfigurator.configure] runs here, synchronously, right after `startKoin`'s modules
 * are wired but before this function returns to its caller (`TogetherlyApplication.onCreate`/
 * `initializeTogetherlyIos`) — before any Composable, `ViewModel`, or premium-state read can
 * possibly run. This is the only call site for it; nothing else configures RevenueCat, and this
 * function itself is only ever invoked once per process by each platform's own app entry point.
 *
 * [com.togetherly.data.purchase.RevenueCatApiKeyProvider] must come from [additionalModules] (each
 * platform's own entry point supplies it — see that interface's own KDoc); `getOrNull` here is a
 * defensive fallback only, so a missing/miswired key-provider binding degrades to
 * [com.togetherly.domain.purchase.PurchaseStartupState.NotConfigured] rather than crashing the
 * entire app at startup — RevenueCat initialization must never be a hard dependency for Togetherly
 * to launch.
 *
 * [TelemetryCoordinator.start] runs the same way, right after — also `getOrNull` (a missing binding
 * degrades to no telemetry rather than a crash) and additionally wrapped in [runCatching], since
 * unlike [RevenueCatConfigurator.configure] this one has an explicit "handles initialization
 * failure without affecting app startup" requirement (see that class's own KDoc) that must hold
 * even for a failure this function's own caller could never see coming.
 */
fun initKoin(
    appConfiguration: AppConfiguration,
    additionalModules: List<Module> = emptyList(),
): KoinApplication {
    val application = startKoin {
        if (appConfiguration.debug) {
            printLogger(Level.DEBUG)
        }
        modules(appModules(appConfiguration) + additionalModules)
    }

    application.koin.getOrNull<RevenueCatConfigurator>()?.configure(debug = appConfiguration.debug)

    runCatching {
        application.koin.getOrNull<TelemetryCoordinator>()?.start()
    }.onFailure {
        application.koin.getOrNull<AppLogger>()?.warn(TAG, "Telemetry coordinator failed to start", it)
    }

    runCatching {
        application.koin.getOrNull<RevenueCatAnalyticsLinker>()?.start()
    }.onFailure {
        application.koin.getOrNull<AppLogger>()?.warn(TAG, "RevenueCat analytics linker failed to start", it)
    }

    return application
}

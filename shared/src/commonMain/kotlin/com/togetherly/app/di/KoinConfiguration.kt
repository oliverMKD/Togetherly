package com.togetherly.app.di

import com.togetherly.app.application.AppConfiguration
import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.logging.AppLogger
import com.togetherly.core.telemetry.TelemetryCoordinator
import com.togetherly.data.purchase.RevenueCatAnalyticsLinker
import com.togetherly.data.purchase.RevenueCatConfigurator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
 * Telemetry providers and the RevenueCat analytics bridge are resolved and started on the default
 * dispatcher after the required purchase setup. Provider SDK setup can involve disk access and
 * native SDK work, so it must neither extend the platform entry point's critical path nor make a
 * provider failure affect app startup.
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

    // getOrNull, not get: a missing/miswired AppDispatchers binding must degrade (fall back to the
    // plain kotlinx.coroutines default dispatcher) exactly like every other optional lookup in this
    // function, not crash startup before telemetry/analytics even get a chance to degrade gracefully.
    val defaultDispatcher = application.koin.getOrNull<AppDispatchers>()?.default ?: Dispatchers.Default
    val startupScope = CoroutineScope(SupervisorJob() + defaultDispatcher)
    launchTelemetryStartup(
        scope = startupScope,
        startTelemetry = { application.koin.getOrNull<TelemetryCoordinator>()?.start() },
        startRevenueCatAnalyticsLinker = { application.koin.getOrNull<RevenueCatAnalyticsLinker>()?.start() },
        logFailure = { message, throwable -> application.koin.getOrNull<AppLogger>()?.warn(TAG, message, throwable) },
    )

    return application
}

/** Launches optional startup integrations without putting their resolution or setup on the caller's critical path. */
internal fun launchTelemetryStartup(
    scope: CoroutineScope,
    startTelemetry: () -> Unit,
    startRevenueCatAnalyticsLinker: () -> Unit,
    logFailure: (String, Throwable) -> Unit,
): Job = scope.launch {
    runCatching(startTelemetry).onFailure { logFailure("Telemetry coordinator failed to start", it) }
    runCatching(startRevenueCatAnalyticsLinker).onFailure {
        logFailure("RevenueCat analytics linker failed to start", it)
    }
}

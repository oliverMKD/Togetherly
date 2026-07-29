package com.togetherly.app.di

import com.togetherly.app.application.AppConfiguration
import com.togetherly.app.foundation.VersionInfoProvider
import com.togetherly.core.telemetry.DuplicateSignalDetector
import com.togetherly.core.telemetry.NoOpProductAnalytics
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.core.telemetry.ProductAnalytics
import com.togetherly.core.telemetry.TelemetryCoordinator
import com.togetherly.core.telemetry.TelemetryDebugRecorder
import com.togetherly.data.purchase.RevenueCatAnalyticsLinker
import com.togetherly.data.telemetry.DebugRecordingOperationalDiagnostics
import com.togetherly.data.telemetry.DebugRecordingProductAnalytics
import com.togetherly.data.telemetry.PostHogApiKeyProvider
import com.togetherly.data.telemetry.PostHogCommonProperties
import com.togetherly.data.telemetry.PostHogSdkAdapter
import com.togetherly.data.telemetry.RealPostHogSdkAdapter
import com.togetherly.data.telemetry.RealSentrySdkAdapter
import com.togetherly.data.telemetry.SentryDsnProvider
import com.togetherly.data.telemetry.SentrySdkAdapter
import com.togetherly.data.telemetry.createOperationalDiagnostics
import com.togetherly.data.telemetry.createProductAnalytics
import org.koin.dsl.module

/**
 * [ProductAnalytics] (Step 14.2) resolves to the real PostHog-backed [PostHogProductAnalytics]
 * whenever a project key is configured, on **both** debug and release builds —
 * [com.togetherly.data.telemetry.RealPostHogSdkAdapter]'s own `PostHogConfig.debug` already gives
 * debug builds the SDK's own debug logging, so [com.togetherly.core.telemetry.DebugProductAnalytics]
 * (still fully available, still fully tested) is no longer the default binding — see
 * `docs/analytics-setup.md`.
 *
 * [OperationalDiagnostics] (Step 14.5) resolves to the real Sentry-backed
 * [com.togetherly.data.telemetry.SentryOperationalDiagnostics] whenever a DSN is configured, on
 * **both** debug and release builds, the same "real provider on every build type, its own `debug`
 * flag gives debug builds the SDK's own debug logging" shape [ProductAnalytics] already
 * establishes — see `docs/sentry-setup.md`. [com.togetherly.core.telemetry.DebugOperationalDiagnostics]
 * (still fully available, still fully tested) is no longer the default binding either.
 *
 * A missing/blank [PostHogApiKeyProvider.projectKey]/[SentryDsnProvider.dsn] falls back to
 * [NoOpProductAnalytics]/[com.togetherly.core.telemetry.NoOpOperationalDiagnostics] on **every**
 * build type — "missing configuration falls back to no-op," never a build failure, so the app
 * remains fully usable without either key configured. A debug build additionally logs a clear
 * warning ("debug build reports missing configuration clearly"); a release build stays silent, the
 * same asymmetry [RevenueCatConfigurator] already establishes for a missing RevenueCat key.
 *
 * [TelemetryCoordinator] is the one binding here that isn't provider selection — see its own KDoc
 * for the startup contract [KoinConfiguration.initKoin] depends on. [RevenueCatAnalyticsLinker]
 * (Step 14.4) is a second, independent consent-driven starter with the same shape — it associates
 * PostHog's anonymous distinct id with RevenueCat's own `$posthogUserId` subscriber attribute, never
 * touching entitlement access; see `docs/revenuecat-posthog-integration.md`. [PostHogSdkAdapter]/
 * [SentrySdkAdapter] are both bound as their own singletons (not constructed inline inside their
 * respective factory lambdas) specifically so [RevenueCatAnalyticsLinker] and
 * [PostHogProductAnalytics] share exactly one PostHog adapter instance, and so Sentry's own adapter
 * is available for the debug-only manual test action (`docs/sentry-setup.md`) without a second
 * `Sentry.init` ever running.
 *
 * [TelemetryDebugRecorder]/[DuplicateSignalDetector] (Step 14.6) are always bound — cheap, and
 * always empty in a release build since nothing ever writes to them there (see below) — but
 * [DebugRecordingProductAnalytics]/[DebugRecordingOperationalDiagnostics] only ever wrap the real
 * `single<ProductAnalytics>`/`single<OperationalDiagnostics>` instance when
 * [AppConfiguration.debug] is `true`. A release build's [ProductAnalytics]/[OperationalDiagnostics]
 * binding is exactly what [createProductAnalytics]/[createOperationalDiagnostics] itself returns,
 * completely undecorated — see `docs/debug-telemetry.md`.
 */
val telemetryModule = module {
    single<PostHogSdkAdapter> { RealPostHogSdkAdapter(context = get()) }

    single { TelemetryDebugRecorder(clock = get()) }
    single { DuplicateSignalDetector(clock = get(), logger = get()) }

    single<ProductAnalytics> {
        val configuration = get<AppConfiguration>()
        val keyProvider = get<PostHogApiKeyProvider>()

        val real = createProductAnalytics(
            projectKey = keyProvider.projectKey(),
            host = keyProvider.host(),
            debug = configuration.debug,
            logger = get(),
            adapter = { get<PostHogSdkAdapter>() },
            commonProperties = {
                PostHogCommonProperties(
                    appConfiguration = configuration,
                    versionInfoProvider = get(),
                    platformInfoProvider = get(),
                    entitlementRepository = get(),
                    dispatchers = get(),
                )
            },
        )
        if (configuration.debug) DebugRecordingProductAnalytics(real, get(), get()) else real
    }

    single {
        RevenueCatAnalyticsLinker(
            consentRepository = get(),
            revenueCatDataSource = get(),
            postHogSdkAdapter = get(),
            logger = get(),
            dispatchers = get(),
        )
    }

    single<SentrySdkAdapter> { RealSentrySdkAdapter() }

    single<OperationalDiagnostics> {
        val configuration = get<AppConfiguration>()
        val dsnProvider = get<SentryDsnProvider>()

        val real = createOperationalDiagnostics(
            dsn = dsnProvider.dsn(),
            release = {
                val versionInfoProvider = get<VersionInfoProvider>()
                "togetherly@${versionInfoProvider.versionName()}+${versionInfoProvider.buildNumber()}"
            },
            environment = if (configuration.debug) "debug" else "release",
            debug = configuration.debug,
            logger = get(),
            adapter = { get<SentrySdkAdapter>() },
        )
        if (configuration.debug) DebugRecordingOperationalDiagnostics(real, get(), get()) else real
    }

    single {
        TelemetryCoordinator(
            consentRepository = get(),
            analytics = get(),
            diagnostics = get(),
            logger = get(),
            dispatchers = get(),
        )
    }
}

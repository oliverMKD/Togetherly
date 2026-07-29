package com.togetherly.data.telemetry

/**
 * Supplies this platform's PostHog **project** (public) key and ingestion host — never a private
 * dashboard/personal API key, and never a value this interface's own implementation should read
 * from committed Kotlin source. Each platform's app-level bootstrap
 * ([com.togetherly.app.di.initKoin]'s caller — `TogetherlyApplication` on Android,
 * `initializeTogetherlyIos` on iOS) supplies a real implementation via `additionalModules`,
 * reading from that platform's own local, gitignored configuration — the exact same shape
 * [com.togetherly.data.purchase.RevenueCatApiKeyProvider] already established; see
 * `docs/analytics-setup.md` for exactly where each value goes.
 *
 * [projectKey] returning `null`/blank means "not configured yet" — the telemetry DI wiring decides
 * what that means (falls back to [com.togetherly.core.telemetry.NoOpProductAnalytics]), never this
 * interface. [host] returning `null`/blank means "use the default" (PostHog Cloud EU — see
 * `RealPostHogSdkAdapter`'s own KDoc for why EU is this app's default, not PostHog's own US
 * default).
 */
interface PostHogApiKeyProvider {
    fun projectKey(): String?
    fun host(): String?
}

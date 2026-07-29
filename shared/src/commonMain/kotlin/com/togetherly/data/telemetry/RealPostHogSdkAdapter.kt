package com.togetherly.data.telemetry

import com.posthog.kmp.PersonProfiles
import com.posthog.kmp.PostHog
import com.posthog.kmp.PostHogConfig
import com.posthog.kmp.PostHogContext

/**
 * The one place `PostHog.setup` is ever called — from [PostHogProductAnalytics]'s own `init`,
 * itself resolved exactly once from [com.togetherly.app.di.KoinConfiguration.initKoin] (via
 * [com.togetherly.core.telemetry.TelemetryCoordinator.start]), the same call-site shape
 * [com.togetherly.data.purchase.RevenueCatConfigurator.configure] already establishes.
 *
 * Every [PostHogConfig] choice here is deliberate:
 * - `host` defaults to [PostHogConfig.HOST_EU] — Togetherly's intended production environment is
 *   PostHog Cloud **EU**, not PostHog's own US-cloud default — whenever
 *   [com.togetherly.data.telemetry.PostHogApiKeyProvider.host] isn't configured.
 * - `captureApplicationLifecycleEvents = false`, `captureScreenViews = false`,
 *   `captureDeepLinks = false`, `autocapture = false` — no automatic capture of any kind; every
 *   event and screen view this app ever sends goes through [PostHogProductAnalytics.capture]/
 *   `.screen` explicitly, never a background SDK hook deciding what happened on its own.
 * - `sendFeatureFlagEvent = false`, `preloadFeatureFlags = false` — Togetherly doesn't use
 *   PostHog feature flags at all; this avoids the extra network round trip `preloadFeatureFlags`
 *   would otherwise make at every `setup` call, consent or not.
 * - `optOut = true` — every setup starts opted out, regardless of consent state, so a family whose
 *   consent is [com.togetherly.domain.telemetry.ConsentDecision.NotAsked]/[com.togetherly.domain.telemetry.ConsentDecision.Denied]
 *   never has an event leave the device even if `setup` itself has already run.
 *   [com.togetherly.core.telemetry.TelemetryCoordinator] is what calls `optIn()`/`optOut()`
 *   afterward, reactively, as consent actually changes — this is the SDK's own documented seam for
 *   "the SDK permits delayed initialization" (see `docs/analytics-setup.md`).
 * - `personProfiles = PersonProfiles.NEVER` — no person profiles are ever created; Togetherly has
 *   no account system and must never build one implicitly through analytics.
 * - `sessionRecording = null` — session replay explicitly stays off. Togetherly displays private
 *   family memories, photos, and notes; session replay must never exist as a code path here.
 * - No advertising identifier is ever collected — this config exposes no such option to enable,
 *   and this app requests no App Tracking Transparency/`NSUserTrackingUsageDescription` permission
 *   on iOS and touches no `AdvertisingIdClient`-style API on Android anywhere in this codebase.
 */
internal class RealPostHogSdkAdapter(
    private val context: PostHogContext,
) : PostHogSdkAdapter {

    override fun setup(projectKey: String, host: String?, debug: Boolean) {
        PostHog.setup(
            PostHogConfig(
                apiKey = projectKey,
                host = host?.takeUnless { it.isBlank() } ?: PostHogConfig.HOST_EU,
                debug = debug,
                captureApplicationLifecycleEvents = false,
                captureScreenViews = false,
                captureDeepLinks = false,
                sendFeatureFlagEvent = false,
                preloadFeatureFlags = false,
                optOut = true,
                personProfiles = PersonProfiles.NEVER,
                sessionRecording = null,
                autocapture = false,
            ),
            context,
        )
    }

    override fun capture(event: String, properties: Map<String, Any>) {
        PostHog.capture(event, properties)
    }

    override fun screen(screenName: String, properties: Map<String, Any>) {
        PostHog.screen(screenName, properties)
    }

    override fun optIn() {
        PostHog.optIn()
    }

    override fun optOut() {
        PostHog.optOut()
    }

    /**
     * Resets PostHog's own anonymous local identity (a fresh anonymous ID going forward) — see
     * `docs/analytics-setup.md` for why this is the SDK-supported "reset anonymous local
     * analytics identity where appropriate" mechanism, and its documented limits around any
     * already-queued-but-unflushed events.
     */
    override fun reset() {
        PostHog.reset()
    }

    override fun flush() {
        PostHog.flush()
    }

    override fun anonymousId(): String? = PostHog.getAnonymousId()
}

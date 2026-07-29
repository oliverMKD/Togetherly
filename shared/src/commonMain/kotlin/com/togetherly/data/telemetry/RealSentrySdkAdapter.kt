package com.togetherly.data.telemetry

import com.togetherly.core.telemetry.TelemetryPrivacyValidator
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb

private const val MAX_BREADCRUMBS = 30

/**
 * The one place `Sentry.init`/`captureException`/`addBreadcrumb`/`configureScope`/`close` are
 * ever called — from [SentryOperationalDiagnostics]'s own `init`, itself resolved exactly once
 * from [com.togetherly.app.di.KoinConfiguration.initKoin] (via
 * [com.togetherly.core.telemetry.TelemetryCoordinator.start]), the same call-site shape
 * [com.togetherly.data.purchase.RevenueCatConfigurator.configure]/[RealPostHogSdkAdapter] already
 * establish.
 *
 * Every [io.sentry.kotlin.multiplatform.SentryOptions] choice here is deliberate:
 * - `sendDefaultPii = false` — never collect IP address, request headers, or other
 *   default-personal-data Sentry can optionally attach.
 * - `attachScreenshot = false` / `attachViewHierarchy = false` — never a screenshot or a rendered
 *   view-hierarchy dump; both could trivially expose a family's private memories, photos, or notes
 *   visible on screen at the moment of a crash.
 * - `maxBreadcrumbs` is capped well below the SDK's own 100-breadcrumb default — bounds how much
 *   trail context (and therefore how much text) any single report can carry.
 * - `beforeBreadcrumb` re-validates every breadcrumb's own message (including ones this app never
 *   explicitly added — Sentry's native Android/iOS layers can generate their own automatic
 *   lifecycle/navigation breadcrumbs this KMP wrapper doesn't expose a dedicated opt-out toggle
 *   for) through the same [TelemetryPrivacyValidator.isSafeText] heuristic
 *   [DebugOperationalDiagnostics] already uses — an unsafe breadcrumb is dropped entirely rather
 *   than redacted in place, since a breadcrumb (unlike a tag) has no other useful content once its
 *   own message is removed.
 * - Session Replay is never enabled — the KMP SDK's own [io.sentry.kotlin.multiplatform.SentryOptions]
 *   surface exposes no session-replay option to opt into in the first place, so there is nothing to
 *   explicitly disable.
 * - No advertising identifier, device identifier beyond what the native SDKs' own crash reports
 *   already require for grouping, or personal data is ever requested by this configuration.
 */
internal class RealSentrySdkAdapter : SentrySdkAdapter {

    private val validator = TelemetryPrivacyValidator.default()

    override fun setup(dsn: String, release: String, environment: String, debug: Boolean) {
        Sentry.init { options ->
            options.dsn = dsn
            options.release = release
            options.environment = environment
            options.debug = debug
            options.sendDefaultPii = false
            options.attachScreenshot = false
            options.attachViewHierarchy = false
            options.maxBreadcrumbs = MAX_BREADCRUMBS
            options.beforeBreadcrumb = { breadcrumb ->
                val message = breadcrumb.message
                if (message != null && !validator.isSafeText(message)) null else breadcrumb
            }
        }
    }

    /** [tags] are applied via Sentry's own per-event scope callback — never a persistent mutation of the shared scope (see this interface's own KDoc). */
    override fun captureException(throwable: Throwable, tags: Map<String, String>) {
        Sentry.captureException(throwable) { scope ->
            tags.forEach { (key, value) -> scope.setTag(key, value) }
        }
    }

    override fun addBreadcrumb(message: String, category: String?) {
        Sentry.addBreadcrumb(Breadcrumb(message = message, category = category))
    }

    override fun clearScope() {
        Sentry.configureScope { scope -> scope.clear() }
    }

    override fun close() {
        Sentry.close()
    }
}

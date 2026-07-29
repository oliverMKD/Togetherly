package com.togetherly.core.telemetry

/**
 * A provider-neutral snapshot of "why is this provider behaving the way it is right now" — built
 * for Step 14.6's debug telemetry screen, never surfaced to a family. Every [ProductAnalytics]/
 * [OperationalDiagnostics] implementation reports its own status; nothing outside `core.telemetry`/
 * `data.telemetry` needs to know *why* a status holds, only which of these four buckets applies.
 *
 * - [MISSING]: no key/DSN was ever configured — the bound instance is a no-op
 *   ([NoOpProductAnalytics]/[NoOpOperationalDiagnostics]) by the factory's own design (see
 *   `ProductAnalyticsFactory.kt`/`SentryDiagnosticsFactory.kt`), not a failure.
 * - [INITIALIZATION_FAILED]: a key/DSN was present, but the underlying SDK's own `setup` call
 *   threw — the real provider instance still exists but is permanently inert for this process (see
 *   [PostHogProductAnalytics]/[SentryOperationalDiagnostics]'s own `setupSucceeded` gate).
 * - [DISABLED_BY_CONSENT]: setup succeeded, but [ProductAnalytics.setCollectionEnabled]/
 *   [OperationalDiagnostics.setCollectionEnabled] is currently `false` — [TelemetryCoordinator]
 *   hasn't (yet) been told this consent category is granted.
 * - [CONFIGURED]: setup succeeded and collection is currently enabled — this provider would
 *   actually send something right now.
 */
enum class ProviderConfigurationStatus {
    CONFIGURED,
    MISSING,
    DISABLED_BY_CONSENT,
    INITIALIZATION_FAILED,
}

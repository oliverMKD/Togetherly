# Debug Telemetry Tooling (Step 14.6)

Documents the debug-only tooling that makes analytics, consent, and diagnostics verifiable during
development, built on top of `docs/telemetry.md`'s consent model and `docs/analytics-setup.md`/
`docs/sentry-setup.md`'s provider setup. This file only documents what's actually implemented
today — see [Not yet implemented](#not-yet-implemented) at the bottom for the rest of Step 14.6.

## Reaching the debug screen

Settings → About → **"Open debug telemetry tools"** (`AboutScreen`'s
`AboutAction.OpenDebugTelemetryClicked`). The button itself is gated by
`AboutUiState.showDebugTelemetryAction`, which `AboutViewModel` only ever sets to `true` when
`AppConfiguration.debug` is `true` — a release build's `AboutScreen` never renders it, and the
destination it navigates to (`RootDestination.DebugTelemetry`, handled by
`DebugTelemetryRoute`/`DebugTelemetryScreen`/`DebugTelemetryViewModel`) has no other entry point
anywhere in the app.

## What the screen shows

All state comes from `DebugTelemetryUiState` (`feature/debug/model/DebugTelemetryUiState.kt`):

- **Consent**: `analyticsConsent`/`diagnosticsConsent` — the current `ConsentDecision`
  (`NotAsked`/`Granted`/`Denied`) from `TelemetryConsentRepository.observeConsent()`, collected
  continuously.
- **Provider status**: `postHogStatus`/`sentryStatus` — a `ProviderConfigurationStatus`
  (`CONFIGURED`/`MISSING`/`DISABLED_BY_CONSENT`/`INITIALIZATION_FAILED`), read from
  `ProductAnalytics.configurationStatus()`/`OperationalDiagnostics.configurationStatus()`. See
  `core/telemetry/ProviderConfigurationStatus.kt` for exactly what each value means and which
  provider state maps to it. `revenueCatStatus` is the same enum, derived from
  `RevenueCatConfigurator.state` (`PurchaseStartupState`) — `Ready` → `CONFIGURED`, `Failed` →
  `INITIALIZATION_FAILED`, `NotConfigured`/`Initializing` → `MISSING` (RevenueCat has no consent
  gate, so `DISABLED_BY_CONSENT` never applies to it).
- **Access**: `accessSummary` — a short string ("Free", "Family Plus (subscription)", etc.) derived
  from `EntitlementRepository.observeAccess()`, collected continuously.
- **Last sanitized analytics events / last safe breadcrumbs / last provider error category**:
  `recentEvents`/`recentBreadcrumbs`/`recentProviderErrors`, read from `TelemetryDebugRecorder` —
  see [In-memory event/breadcrumb/error inspector](#in-memory-eventbreadcrumberror-inspector) below.

None of these fields ever carry a complete SDK key, a full DSN, a RevenueCat App User ID, a
PostHog distinct id, a receipt/purchase token, or family/memory content — see
`DebugTelemetryUiState`'s own KDoc.

`ProviderConfigurationStatus`/`TelemetryDebugRecorder`'s three histories have no reactive stream of
their own (plain synchronous reads); `DebugTelemetryViewModel.refreshSnapshot()` re-pulls all of
them on construction, after every action that could plausibly change one, and whenever
`DebugTelemetryAction.RefreshClicked` is dispatched.

## Actions

All six live in `DebugTelemetryAction`:

| Action | Effect |
|---|---|
| `RefreshClicked` | Re-pulls provider status + the three debug histories (see above). |
| `FlushClicked` | Calls `ProductAnalytics.flush()`. |
| `ClearHistoryClicked` | Calls `TelemetryDebugRecorder.clear()`, then refreshes — "Clear local debug history." |
| `SendTestEventClicked` | Calls `ProductAnalytics.capture(DebugTestEvent)` — a real, schema-registered, zero-property event (`core/telemetry/AnalyticsEvent.kt`, name `debug_test_event`) that exists solely for this action, "the one event this app ever deliberately sends as a manual, end-to-end pipeline check." |
| `SendTestExceptionClicked` | Calls `OperationalDiagnostics.captureHandledException` with a fixed, private, already-safe synthetic exception (`DebugTestException`), mirroring `AboutViewModel`'s own existing `TestDiagnosticException` action from Step 14.5. |

## In-memory event/breadcrumb/error inspector

`core/telemetry/TelemetryDebugRecorder.kt` holds three independently bounded histories (20 entries
each, oldest evicted first — `core/telemetry/BoundedHistory.kt`, a thread-safe compare-and-swap
loop over an immutable list). Nothing writes to it except two decorators, both only ever
constructed in a debug build (`app/di/TelemetryModule.kt`):

- `data/telemetry/DebugRecordingProductAnalytics.kt` wraps the real `ProductAnalytics` — every
  `capture()` call is forwarded to the real delegate completely unchanged first (never alters
  whether or what a real provider receives), and *additionally*, only while its own
  `setCollectionEnabled(true)` has been called (i.e. only after analytics consent is granted —
  nothing before that, even though this history never leaves the device either, mirroring
  `DebugProductAnalytics`'s own established "no capture before consent, even locally" rule from
  Step 14.2), records the event's name and its already-validated, already-sanitized properties
  (`TelemetryPrivacyValidator.validateEvent`'s `Accepted` result) into the recorder.
- `data/telemetry/DebugRecordingOperationalDiagnostics.kt` is the diagnostics-side counterpart: it
  records only an exception's class name (never its message) into the recorder, and a breadcrumb
  message only after it survives `DiagnosticSanitizer.sanitizeBreadcrumbMessage`, on the same
  "only while collection is enabled" gate.

Both decorators also feed `core/telemetry/DuplicateSignalDetector.kt`, unconditionally (this one
check is never gated by consent, since it only logs a developer-facing warning via `AppLogger` — it
never retains any event content). It compares each `capture`/`captureHandledException` call's
"operation id" (an event name, or `"<exception class>:<operation/feature tag>"`) against that same
id's own last-seen timestamp; a repeat within 500ms logs a warning suggesting a Compose
recomposition or a repeated collector error — it never suppresses or deduplicates the actual call.

## Not yet implemented

Step 14.6 also specifies build-time validation and manual-testing documentation that do not exist
yet — do not assume any of the following are wired up:

- No automated check exists yet that production code never references debug telemetry UI, that
  feature code never depends directly on PostHog/Sentry types, that event names are unique or
  snake_case, or that every production event appears in `docs/analytics-event-taxonomy.md`. No
  generated event catalogue exists.
- No dedicated offline-behavior test suite exists yet (the underlying architecture is offline-first
  by construction — Room-backed consent storage, no network dependency for consent changes — but
  this hasn't been captured as its own explicit test suite).
- No manual test checklist doc exists yet for the fresh-install / consent-combination / offline /
  missing-key scenarios Step 14.6 calls for.

These remain open follow-up work, not something this file should describe as done.

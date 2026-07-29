# Sentry Operational Diagnostics Setup (Step 14.5)

This documents the Sentry integration built on top of Step 14.1's provider-neutral telemetry
architecture (see [telemetry.md](telemetry.md) for the consent model, privacy allowlist, and
`OperationalDiagnostics` contract itself — this file is operational setup for the real Sentry
provider, not a repeat of that design doc). It mirrors [analytics-setup.md](analytics-setup.md)'s
own shape closely — `OperationalDiagnostics` (crash/error diagnostics) and `ProductAnalytics`
(product events) are Togetherly's two independent, provider-neutral telemetry surfaces, each with
its own consent toggle, its own no-op fallback, and its own real provider (Sentry vs. PostHog).

## 1. Creating a Sentry project

1. Sign up / log in at [sentry.io](https://sentry.io) and create a new project for the Kotlin
   Multiplatform SDK (or a generic project — the SDK works the same either way).
2. Project Settings → **Client Keys (DSN)** — this is the value this app needs. A DSN is public
   client configuration (it only tells the SDK where to send events), not a secret.
3. **Never** place a Sentry **auth token** (Settings → Auth Tokens, used only for uploading
   ProGuard/R8 mapping files and iOS dSYMs — see [Release tooling](#release-tooling) below)
   anywhere in this app. A DSN and an auth token are different kinds of credential with different
   blast radii if leaked: a leaked DSN lets someone send junk events to your project; a leaked auth
   token can read/write project data. The app only ever needs the DSN.

## 2. DSN — where it goes

Same shape as [revenuecat-setup.md](revenuecat-setup.md)/[analytics-setup.md](analytics-setup.md)'s
own key management, mirrored exactly: the real DSN is never hardcoded in shared Kotlin source and
never committed to version control.

**Android**: place the DSN in `local.properties` (already gitignored):
```
sentry.dsn=
```
See `local.properties.example` for the tracked template. `androidApp/build.gradle.kts` reads it
into `BuildConfig.SENTRY_DSN`, and `TogetherlyApplication` supplies it to the shared module via
Koin (`SentryDsnProvider`).

**iOS**: place the DSN in `iosApp/Configuration/Sentry.local.xcconfig` (already gitignored):
```
SENTRY_DSN=
```
See `Sentry.local.xcconfig.example` for the tracked template (ships with no placeholder DSN, the
same "no safe-to-commit value" shape PostHog's own example file uses).  `Config.xcconfig` includes
this file if present; `Info.plist` exposes the value via `$(SENTRY_DSN)`, and
`TogetherlyIosInitializer` reads it from `NSBundle.mainBundle` at startup.

**If the DSN is missing or blank**, on **either** build type: `OperationalDiagnostics` falls back
to `NoOpOperationalDiagnostics` — the app remains fully usable, nothing crashes, nothing is
reported. A **debug** build additionally logs a clear warning (tag `SentryDiagnosticsFactory`) so a
missing local DSN is never silently invisible during development; a **release** build stays
silent — the same asymmetry `ProductAnalyticsFactory` already establishes for a missing PostHog
key.

The DSN itself is never written to any log.

## 3. What is and isn't configured

Configured in `RealSentrySdkAdapter` (see that class's own KDoc for the full reasoning per
option):

| Setting | Value | Why |
|---|---|---|
| `sendDefaultPii` | `false` | No personal data collection — Togetherly has no account system. |
| `attachScreenshot` | `false` | No screenshot attachments — this app displays private family memories, photos, and notes. |
| `attachViewHierarchy` | `false` | No view-hierarchy attachments, for the same reason. |
| `maxBreadcrumbs` | `30` | Bounded breadcrumb history — limits both memory and the amount of app-generated context in any one report. |
| `beforeBreadcrumb` | drops any breadcrumb whose message fails `TelemetryPrivacyValidator.isSafeText` | A second, independent check on every breadcrumb — including any the native SDK might generate on its own — not just the ones this app's own `addBreadcrumb` call sites construct. |
| `debug` | `AppConfiguration.debug` | Native SDK debug logging only in debug builds. |

**Session replay is never enabled.** The Sentry KMP SDK's `SentryOptions` surface exposes no
session-replay option to opt into at all — there is nothing to turn off because there is nothing
this integration could turn on in the first place.

**No automatic HTTP request/response body or header capture.** The Sentry KMP SDK (unlike some
platform-native Sentry SDKs) has no such automatic-instrumentation option exposed to enable, and
this app's own `RealSentrySdkAdapter` never adds one.

**Togetherly memory content is never attached.** No photo, voice recording, or completion note is
ever passed to Sentry — `DiagnosticContext`'s allowed-key allowlist (see
[Allowed diagnostic context](#allowed-diagnostic-context) below) has no slot for one, and
`DiagnosticSanitizer` strips file paths from whatever *is* sent.

## 4. Consent behavior

Unchanged from [telemetry.md](telemetry.md#telemetrycoordinator) — `TelemetryCoordinator` is the
only caller of `OperationalDiagnostics.setCollectionEnabled`/`.clearContext`, reacting to
`TelemetryConsentRepository.observeConsent()`. What's specific to `SentryOperationalDiagnostics`:

- **`NotAsked`/`Denied`**: `setCollectionEnabled(false)`. `SentryOperationalDiagnostics` keeps its
  own independent `collectionEnabled` flag and never calls `adapter.captureException`/
  `.addBreadcrumb` while disabled — no pre-consent call is ever queued for later replay. "When
  consent becomes granted, enable future reporting — do not manufacture reports for earlier
  failures."
- **`Granted`**: `setCollectionEnabled(true)`. Only failures captured *after* this point are ever
  reported.
- **Revoked** (`Granted` → anything else): `setCollectionEnabled(false)` (stops all future
  reporting immediately) followed by `clearContext()` → `Sentry.configureScope { it.clear() }`. See
  [SDK limitations](#sdk-limitations-queue-clearing-on-revocation) below for what this does and
  does not guarantee about already-queued events.
- **Local data deletion resets diagnostics consent** the same way it resets analytics consent (see
  `telemetry.md`) — a family choosing "delete all my data" returns diagnostics to `NotAsked`, not
  merely `Denied`, so a future reinstall/reset starts from the same first-run consent prompt rather
  than a silently-carried-forward decision.
- **RevenueCat is never affected.** `SentryOperationalDiagnostics` has no dependency on
  `EntitlementRepository` or RevenueCat's own identity at all — these are structurally unrelated
  subsystems, unlike `RevenueCatAnalyticsLinker`'s deliberate PostHog↔RevenueCat identity link (see
  `revenuecat-posthog-integration.md`).

Crash reporting never affects application behavior: every `SentryOperationalDiagnostics` method
runs inside `runCatching`, and a failed `SentrySdkAdapter.setup` call leaves the instance
permanently, safely inert for the rest of the process rather than retrying or crashing anything
else.

## SDK limitations: queue clearing on revocation

The Sentry KMP SDK's public API exposes `Sentry.configureScope { it.clear() }` for clearing tags/
breadcrumbs/context carried on the *current* scope going forward, but **no explicit "purge
already-queued, not-yet-sent envelopes on disk" method**. `Sentry.close()` does the opposite of
discarding — it flushes and attempts to *send* whatever is pending — so it is deliberately never
called on revocation. Given `SentryOperationalDiagnostics` never calls
`adapter.captureException`/`.addBreadcrumb` at all while its own `collectionEnabled` flag is
`false`, the realistic exposure window is limited to: a report captured in the brief moment between
a family granting consent and a near-simultaneous revocation, if the native SDK's own internal
envelope batch hadn't sent yet. This integration cannot fully close that window given the SDK's
current public API — it is disclosed here rather than silently assumed away, the same shape
[analytics-setup.md](analytics-setup.md#sdk-limitations-queue-clearing-on-opt-out) discloses for
PostHog's own opt-out queue.

## Allowed diagnostic context

`DiagnosticSanitizer` and every capture-boundary call site are constrained to this fixed key
vocabulary (see `core/telemetry/DiagnosticContext.kt` and each call site's own `DiagnosticContext`
construction):

`feature`, `operation`, `result_category`, `platform`, `app_version`, `build_number`,
`access_state`, `quest_category`.

**Never included**: quest/pack ids (unless genuinely needed for a specific future debugging need —
not used by any current capture site), family profile identifiers, memory identifiers, database
rows, file paths, user-entered text, search queries, notification contents, purchase tokens,
RevenueCat customer information, or raw database contents. `DiagnosticSanitizer.sanitizeTags`
additionally strips file paths and URL query parameters from whatever string values *are* sent, and
drops (rather than redacts) any tag value that still fails `TelemetryPrivacyValidator.isSafeText`
after stripping.

**Exception messages are not sanitized** — see `DiagnosticSanitizer`'s own KDoc for the full
reasoning: Kotlin's common `Throwable` has no portable `stackTrace` property, so wrapping a caught
exception in a "sanitized" replacement would only ever capture a stack trace pointing at the
wrapper's own construction site, not the real bug location — worthless for debugging, and a direct
violation of "do not weaken stack traces so much that reports become useless." Sentry's own
`SentryException.value` (the message field) is also immutable, so a `beforeSend`-style rewrite
after capture isn't mechanically possible either. The original `Throwable` is always passed through
to `Sentry.captureException` unmodified, preserving its real stack trace and its own natural
`.message`. This is why every capture-boundary call site is expected to pass a deliberately-authored
exception (a `private object ... : Exception("...")`, or the caught exception from a narrow,
already-understood failure path) rather than blindly forwarding an arbitrary throwable whose
message might contain something unsafe — **call-site discipline**, not mechanical sanitization, is
what keeps exception messages safe.

## Capture boundaries

Instrumented (all 9 from the spec):

| Boundary | Location |
|---|---|
| Catalogue parsing failure | `content/loader/DefaultQuestCatalogueLoader.kt` |
| Database migration failure | `app/di/DatabaseModule.kt` (`TogetherlyDatabase` singleton) |
| Persistence failure | `data/RoomRepositorySupport.kt` (`runCatchingStorage`/`catchStorageReadErrors`, shared by every Room-backed repository/cleaner) |
| Private media storage failure | `data/media/AndroidPrivateMediaStorage.kt` / `IosPrivateMediaStorage.kt` |
| Reminder scheduling failure | `core/notification/ReminderScheduler.android.kt` / `.ios.kt` |
| RevenueCat initialization failure | `data/purchase/RevenueCatConfigurator.kt` |
| Offering loading failure | `data/purchase/DefaultRevenueCatDataSource.kt` (`getOfferingPackages`) |
| Unexpected purchase mapping failure | `data/purchase/DefaultRevenueCatDataSource.kt` (offering→`PurchasePackage` mapping) |
| Navigation restoration failure | `navigation/state/BootstrapViewModel.kt` — Togetherly's own analog: Compose/Navigation's own state restoration is framework-internal, not app-authored code, so Bootstrap's cold-start "which destination does this app resume into" decision is the closest genuine call site. |

**Never reported** (expected user outcomes, confirmed at each site not to reach a capture call):
purchase cancelled/pending (`DefaultRevenueCatDataSource.purchase`'s `PurchasesTransactionException`
branch is a separate `catch` clause from the one that captures), no purchases to restore (a
`RestoreResult.Success` path, never an exception), notification permission denied (handled entirely
outside `ReminderScheduler` — see `NotificationPermissionStatusProvider`/`ReminderViewModel`, fully
decoupled from scheduling), user dismissed paywall, empty search results, user abandoned a quest.

## Release tooling

**Current state: Android release builds do not yet enable R8/ProGuard minification**
(`isMinifyEnabled = false` in `androidApp/build.gradle.kts`) — there is no obfuscated stack trace
to de-obfuscate yet, so no mapping file upload is wired into the build. The steps below are the
documented plan for when minification is turned on, not something already running.

### Android — ProGuard/R8 mapping upload (when minification is enabled)

1. Add the [Sentry Android Gradle plugin](https://docs.sentry.io/platforms/android/configuration/gradle/)
   to `androidApp/build.gradle.kts` (verify current version/compatibility against this project's
   Android Gradle Plugin/Kotlin versions the same way the Sentry KMP plugin itself was verified —
   see this step's own commit history for that process).
2. The plugin uploads the R8 mapping file automatically on a release build, keyed by
   `release`/`environment` — matching the exact `release = "togetherly@${versionName}+${buildNumber}"`
   string `TelemetryModule.kt` already passes to `Sentry.init`, so uploaded mappings line up with
   real crash reports without any extra configuration.
3. Requires `SENTRY_AUTH_TOKEN`, `SENTRY_ORG`, `SENTRY_PROJECT` as **CI-only environment
   variables** (see [Required CI environment variables](#required-ci-environment-variables) below)
   — never placed in `local.properties`, `build.gradle.kts`, or any other tracked file.

### iOS — dSYM upload (when needed)

1. Use the official [Sentry Xcode build integration](https://docs.sentry.io/platforms/apple/dsym/) —
   either the `sentry-cli` `Bundle And Upload Debug Symbols` Run Script build phase, or
   [Fastlane's `sentry` Fastlane plugin](https://docs.sentry.io/platforms/apple/dsym/#fastlane) if
   this project adopts Fastlane for release automation.
2. Same `release`/`environment` string as Android — `IosVersionInfoProvider` reads
   `CFBundleShortVersionString`/`CFBundleVersion` from the compiled Info.plist, so the uploaded
   dSYM's own build identification matches what `Sentry.init` reports at runtime automatically.
3. Requires `SENTRY_AUTH_TOKEN`, `SENTRY_ORG`, `SENTRY_PROJECT` as **CI-only environment
   variables**, same as Android — never placed in the Xcode project file, an xcconfig, or any other
   tracked file.

### Required CI environment variables

Document only — not present in any tracked file, and this project currently has no CI pipeline
configuration (`.github/workflows` or equivalent) to wire them into:

| Variable | Purpose | Scope |
|---|---|---|
| `SENTRY_AUTH_TOKEN` | Authenticates mapping/dSYM upload to the Sentry API | CI/server only — never in the app, never in `local.properties`/xcconfig |
| `SENTRY_ORG` | Sentry organization slug | CI/server only |
| `SENTRY_PROJECT` | Sentry project slug | CI/server only |

These three are unrelated to `SENTRY_DSN` (a client-safe value baked into the app itself via
`local.properties`/`Sentry.local.xcconfig` — see [DSN — where it goes](#2-dsn--where-it-goes)
above) — a CI pipeline needs both the DSN (to build a working app) and these three (to upload debug
symbols for that build), but only the auth token/org/project trio is sensitive enough to require
CI-only secret storage.

## Debug-only manual test action

`AboutViewModel`/`AboutScreen` (Settings → About) render a **"Send test diagnostic event"** button
only when `AppConfiguration.debug` is `true` — `AboutUiState.showDiagnosticsTestAction` mirrors the
existing `showEnvironmentLabel` debug-only gate exactly. Tapping it calls
`OperationalDiagnostics.captureHandledException` with a fixed, already-safe synthetic exception
(`TestDiagnosticException`, a private singleton with a static message — never anything derived from
live app state) and `DiagnosticContext(mapOf("feature" to "diagnostics", "operation" to
"manual_test_capture"))`. This is never reachable on a release build: a release `AppConfiguration`
never sets `showDiagnosticsTestAction` to `true`, so `AboutScreen` never renders the button at
all — there is no production-accessible crash/test-report button, satisfying that requirement
directly rather than merely disabling one.

To verify manually during development:
1. Fill in `local.properties`/`Sentry.local.xcconfig` with a real DSN pointed at a project you're
   comfortable seeing test events in (a dedicated dev/staging Sentry project is recommended over
   your eventual production one).
2. Launch a debug build. Since there is still no consent-granting UI (only
   `TelemetryConsentRepository` itself exists — same limitation `analytics-setup.md` documents for
   PostHog), diagnostics consent must be granted manually first: temporarily call
   `TelemetryConsentRepository.updateDiagnosticsConsent(ConsentDecision.Granted)` from a debug-only
   code path or a debugger/breakpoint.
3. Navigate to Settings → About, tap "Send test diagnostic event."
4. Confirm the event appears in the Sentry dashboard's issue stream for that project, tagged
   `exception_type = TestDiagnosticException` plus the two context tags above, with the app's real
   `release`/`environment` attached — and confirm no family profile id, memory id, file path, or any
   other disallowed value appears anywhere on the event.

## Known limitations of this step

- No consent-granting UI exists yet — `TelemetryConsentRepository` is fully wired and tested, but a
  parent-facing Privacy-settings toggle to actually call `updateDiagnosticsConsent` is not part of
  this step (same limitation `analytics-setup.md` documents for PostHog's own analytics consent).
- Android release builds do not yet enable R8/ProGuard minification — see
  [Release tooling](#release-tooling) above.
- The Sentry Android Gradle plugin (for mapping upload) and an iOS dSYM-upload build phase are
  documented here as the plan, not yet added to the build — adding them requires the same
  compatibility-verification diligence the Sentry KMP plugin itself received before being wired in.
- iOS integration was verified via `:shared:compileKotlinIosSimulatorArm64` and
  `:shared:linkDebugFrameworkIosSimulatorArm64` only, plus `xcodebuild -resolvePackageDependencies`
  confirming the manually-added Sentry Cocoa SPM package resolves — no simulator/device runtime
  verification was possible in this environment.

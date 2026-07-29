# PostHog Analytics Setup (Step 14.2)

This documents the PostHog integration built on top of Step 14.1's provider-neutral telemetry
architecture (see [telemetry.md](telemetry.md) for the consent model, privacy allowlist, and
`ProductAnalytics` contract itself — this file is operational setup for the real PostHog provider,
not a repeat of that design doc).

## 1. Creating a PostHog Cloud EU project

1. Sign up / log in at [posthog.com](https://posthog.com) and create a new project.
2. When choosing a region, select **EU** — Togetherly's data boundary is PostHog Cloud EU
   (`https://eu.i.posthog.com`), not PostHog's own US-cloud default. If a project was accidentally
   created on US cloud, create a new one on EU cloud instead of trying to migrate region after the
   fact.
3. Project settings → **"Project API Key"** is the value this app needs. This is a public,
   client-safe **project token** (it starts with `phc_`) — not a personal/private API key. **Never**
   place a personal API key (found under your account's own API key settings, used for PostHog's
   server-side REST/query APIs) anywhere in this app; the mobile SDK only ever needs the project
   token.

## 2. API keys — where they go

Same shape as [revenuecat-setup.md](revenuecat-setup.md)'s own key management, mirrored exactly:
the real token is never hardcoded in shared Kotlin source and never committed to version control.

**Android**: place the token in `local.properties` (already gitignored):
```
posthog.projectKey=your_project_token_here
posthog.host=
```
See `local.properties.example` for the tracked template. Leave `posthog.host` blank to default to
PostHog Cloud EU. `androidApp/build.gradle.kts` reads both into
`BuildConfig.POSTHOG_PROJECT_KEY`/`BuildConfig.POSTHOG_HOST`, and `TogetherlyApplication` supplies
them to the shared module via Koin (`PostHogApiKeyProvider`).

**iOS**: place the token in `iosApp/Configuration/PostHog.local.xcconfig` (already gitignored):
```
POSTHOG_PROJECT_KEY=your_project_token_here
POSTHOG_HOST=
```
See `PostHog.local.xcconfig.example` for the tracked template — unlike RevenueCat's own example
file, this one ships with **no** placeholder token value, since PostHog has no "test store"-style
safe-to-commit key. `Config.xcconfig` includes this file if present; `Info.plist` exposes both
values via `$(POSTHOG_PROJECT_KEY)`/`$(POSTHOG_HOST)`, and `TogetherlyIosInitializer` reads them
from `NSBundle.mainBundle` at startup.

**If the token is missing or blank**, on **either** build type: `ProductAnalytics` falls back to
`NoOpProductAnalytics` — the app remains fully usable, nothing crashes, nothing is collected. A
**debug** build additionally logs a clear warning (tag `ProductAnalyticsFactory`) so a missing
local key is never silently invisible during development; a **release** build stays silent. This
is a deliberate asymmetry from `RevenueCatConfigurator`'s own missing-key handling (which throws in
debug) — a missing analytics key should never block a developer from running the app at all, since
analytics is never a feature-gating concern the way Family Plus purchases are.

The token itself (complete or partial) is never written to any log.

## 3. EU ingestion host

`RealPostHogSdkAdapter.setup` always resolves the ingestion host with EU as the default:
```kotlin
host = host?.takeUnless { it.isBlank() } ?: PostHogConfig.HOST_EU  // "https://eu.i.posthog.com"
```
`PostHogApiKeyProvider.host()` only needs to be set at all if a project deliberately uses a
self-hosted or non-default PostHog instance; leaving it blank always means "PostHog Cloud EU."

## 4. What is and isn't configured

Configured in `RealPostHogSdkAdapter` (see that class's own KDoc for the full reasoning per
option):

| Setting | Value | Why |
|---|---|---|
| `personProfiles` | `NEVER` | No person profiles — Togetherly has no account system. |
| `sessionRecording` | `null` | Session replay explicitly off — this app displays private family memories, photos, and notes. |
| `autocapture` | `false` | No automatic event capture. |
| `captureScreenViews` | `false` | No automatic screen capture — screens are sent manually via `ProductAnalytics.screen`. |
| `captureApplicationLifecycleEvents` | `false` | No automatic app-open/background events. |
| `captureDeepLinks` | `false` | No automatic deep-link capture. |
| `sendFeatureFlagEvent` / `preloadFeatureFlags` | `false` | Togetherly doesn't use PostHog feature flags at all — avoids an extra network round trip at every `setup`, consent or not. |
| `optOut` | `true` (always, at setup) | Every setup starts opted out regardless of consent state — see [Consent behavior](#consent-behavior). |

**No advertising identifier is ever collected.** `PostHogConfig` exposes no such option to enable,
and this app requests no App Tracking Transparency (`NSUserTrackingUsageDescription`) permission on
iOS and calls no `AdvertisingIdClient`-style API on Android anywhere in this codebase (confirmed by
grep — zero matches).

**identify/alias/group are never called.** Togetherly has no account system; every install is
anonymous-only, forever. `ProductAnalytics`'s own contract has no `identify` method at all (see
[telemetry.md](telemetry.md)) — there is no code path that could call it.

## 5. Disabling IP collection (required dashboard action)

**This is a PostHog project setting, not something this app's code can configure.** In the PostHog
dashboard: Project settings → look for the IP address collection / "Discard client IP data" option
(PostHog's own settings naming may vary by dashboard version) and enable discarding captured IP
data for this project. Do this for every PostHog Cloud EU project this app is ever pointed at,
including any staging/test project — it is not a one-time account-wide setting.

## 6. Consent behavior

Unchanged from [telemetry.md](telemetry.md#telemetrycoordinator) — `TelemetryCoordinator` is the
only caller of `ProductAnalytics.setCollectionEnabled`/`.reset`, reacting to
`TelemetryConsentRepository.observeConsent()`. What's new in this step is how `PostHogProductAnalytics`
itself implements those calls:

- **`NotAsked`/`Denied`**: `setCollectionEnabled(false)` → `PostHogSdkAdapter.optOut()`. Every
  `setup` call also starts with `optOut = true` baked into `PostHogConfig`, so even the moment
  `PostHog.setup()` itself runs (which must happen once, early, to have an SDK instance to opt in
  to later), no event can leave the device before that — the SDK's own opt-out flag is honored from
  the very first line of configuration. `PostHogProductAnalytics` also keeps its own independent
  `collectionEnabled` flag and never even calls `adapter.capture`/`.screen`/`.flush` while
  disabled — two independent layers, not just trusting the SDK's own (very early, 0.x) opt-out
  enforcement alone.
- **`Granted`**: `setCollectionEnabled(true)` → `PostHogSdkAdapter.optIn()`. No pre-consent event is
  ever replayed — this app never queues a `capture`/`screen` call made while disabled; the call
  never reaches the adapter at all, so there is nothing to replay later, and no synthetic history
  is ever fabricated for previous activity.
- **Revoked** (`Granted` → anything else): `setCollectionEnabled(false)` → `optOut()`, followed by
  `reset()` → `PostHog.reset()` (a fresh anonymous ID going forward). See
  [SDK limitations](#sdk-limitations-queue-clearing-on-opt-out) below for what this does and does
  not guarantee about already-queued events.
- **RevenueCat is never affected.** `PostHogProductAnalytics` has no dependency on
  `EntitlementRepository`'s *write* surface (only reads `observeAccess()` for the `access_state`
  common property) and no dependency on RevenueCat's own identity at all — these are structurally
  unrelated subsystems.

## SDK limitations: queue clearing on opt-out

`posthog-kmp` 0.2.0 (a `0.x`, pre-1.0 SDK — "API stability pending" per PostHog's own
documentation) exposes `optOut()`/`optIn()`/`reset()`/`flush()`, but **no explicit "clear the
pending unflushed queue" method**. `optOut()`'s own documented contract is "no events will be
captured or sent" going forward — it is not documented to retroactively purge whatever the native
SDK may have already batched internally before the opt-out call landed. Given this app's own
`PostHogProductAnalytics` never calls `adapter.capture`/`.screen` at all while its own
`collectionEnabled` flag is `false`, the realistic exposure window is limited to: an event captured
in the brief moment between a family granting consent and a near-simultaneous revocation, if the
native SDK's own internal batch hadn't flushed yet. This integration cannot fully close that window
given the SDK's current public API — it is disclosed here rather than silently assumed away.
`reset()` (called on every revocation) at least ensures any such straggler event, if the native SDK
does still send it, is associated with a distinct-id this app has already abandoned rather than the
family's ongoing anonymous identity.

## 7. Testing debug events

1. Fill in `local.properties`/`PostHog.local.xcconfig` with a real PostHog Cloud EU project token
   pointed at a project you're comfortable seeing test events in (a dedicated dev/staging PostHog
   project is recommended over your eventual production one).
2. Launch a debug build. `RealPostHogSdkAdapter.setup` runs with `debug = true` —
   `PostHogConfig.debug` enables the underlying native SDK's own debug logging (Android
   Logcat/Xcode console), separate from this app's own `AppLogger` tag `PostHogProductAnalytics`
   (setup/capture/reject/failure messages) and `ProductAnalyticsFactory` (missing-config warning).
3. Since there is still no consent-granting UI (only `TelemetryConsentRepository` itself exists),
   nothing actually opts in during normal use; to manually verify the pipeline end-to-end during
   development, a developer can temporarily call
   `TelemetryConsentRepository.updateAnalyticsConsent(ConsentDecision.Granted)` from a debug-only
   code path (or a debugger/breakpoint), then use the app normally — every feature ViewModel now
   calls `ProductAnalytics.capture`/`.screen` at real product moments (see
   [analytics-event-taxonomy.md](analytics-event-taxonomy.md) for the full list), so no manual
   `capture` call is needed once consent is granted.
4. Confirm the event appears in the PostHog dashboard's live events view for that project, with
   exactly the five common properties (`app_version`, `build_number`, `platform`, `environment`,
   `access_state`) plus the event's own registered properties — nothing else.

## 8. Production verification

Before shipping a build pointed at a real production PostHog Cloud EU project:

1. Confirm IP discarding is enabled for that specific project (see
   [Disabling IP collection](#5-disabling-ip-collection-required-dashboard-action) — a setting per
   project, easy to forget when creating a fresh production project separate from a dev one).
2. Confirm session replay is off in the dashboard's own project settings too, as a second
   confirmation beyond this app's own `sessionRecording = null` — belt and suspenders, since a
   project-level dashboard toggle could theoretically be flipped independently of this app's code.
3. Confirm autocapture/heatmaps are off in the dashboard's own project settings for the same
   reason.
4. With consent still `NotAsked` (a fresh install, before any future consent UI exists), confirm no
   events appear in the dashboard at all.
5. Once a future step adds a real consent-granting UI: grant analytics consent, trigger a real
   in-app action once feature code is instrumented, and confirm exactly one event appears with only
   the five documented safe properties — never a family profile id, RevenueCat App User ID, exact
   device model, advertising id, IP (should already read as discarded), or a fine-grained locale.
6. Revoke consent and confirm no further events appear afterward, and that a fresh
   `getAnonymousId()`-equivalent identity is used if consent is later re-granted (a new anonymous
   distinct ID after `reset()`).

## Known limitations of this step

- No consent-granting UI exists yet — `TelemetryConsentRepository` is fully wired and tested, but a
  parent-facing Privacy-settings toggle to actually call `updateAnalyticsConsent`/
  `updateDiagnosticsConsent` is not part of this step.
- `posthog-kmp` 0.2.0 is a very early (`0.x`) release; its public API may change in a future
  version in ways that require revisiting `RealPostHogSdkAdapter`. `PostHogSdkAdapter`'s own narrow
  interface exists specifically to contain that blast radius to one file.
- iOS integration was verified via `:shared:compileKotlinIosSimulatorArm64` and
  `:shared:linkDebugFrameworkIosSimulatorArm64` only (the published KMP artifact already bundles
  PostHog's iOS Swift package at PostHog's own publish time, so no additional Xcode-side SPM setup
  was needed) — no simulator/device runtime verification was possible in this environment.

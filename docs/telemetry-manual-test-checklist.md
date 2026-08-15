# Telemetry Manual Test Checklist (Step 14.6)

Manual QA scenarios for consent, analytics, and diagnostics behavior — complements the automated
suite (`shared/src/commonTest/kotlin/com/togetherly/core/telemetry/`,
`shared/src/commonTest/kotlin/com/togetherly/data/telemetry/`) rather than duplicating it. Run this
against a **debug build** unless a step says otherwise — the debug telemetry screen this checklist
relies on only exists when `AppConfiguration.debug` is `true` (see `docs/debug-telemetry.md`).

## Before you start: what's actually reachable today

There is currently **no consent-toggle UI** anywhere in the app — `feature/family/presentation/PrivacyScreen.kt`
is purely informational (its own `PrivacyAction` sealed interface has exactly one case,
`BackClicked`). `TelemetryConsentRepository.updateAnalyticsConsent`/`updateDiagnosticsConsent` are
only ever called from the repository's own definition and from tests — no feature screen calls
either one. This means the only telemetry-consent state a QA tester can currently reach through the
UI is **`NotAsked` for both categories** (which this app treats identically to "denied" —
collection disabled). This is expected, not a bug: this checklist documents it explicitly rather
than describing toggle steps that don't exist yet (see §3 for what to do once that UI ships).

## 1. Fresh install

1. Clear app data (or uninstall/reinstall) so this is a genuine fresh install.
2. Confirm no local telemetry keys are configured — the repo's own tracked defaults
   (`local.properties.example`, the three `*.local.xcconfig.example` files) ship with every key
   blank; don't copy real values in for this pass. See `docs/configuration.md`.
3. Launch the app and complete onboarding.
   - **Expected**: every step completes with no crash and no network dependency (see §2).
4. Family tab → About → **"Open debug telemetry tools"** (debug builds only).
   - **Expected**: `analyticsConsent` = `NotAsked`, `diagnosticsConsent` = `NotAsked`,
     `postHogStatus`/`sentryStatus` = `MISSING` (no key configured), `accessSummary` = "Free".

## 2. Offline / missing-key scenarios

1. With no PostHog/Sentry key configured (the fresh-install default), confirm the app starts
   normally with no crash. A debug build logs a warning for the missing key (see
   `docs/configuration.md`); a release-configured build falls back silently — check your build's
   own log output matches which one you're running.
2. Turn on Airplane Mode (or otherwise fully disconnect network) **before** launching the app.
   - Complete onboarding, browse Explore, start and complete a quest, save a memory with a note,
     view Journey, open Reminders.
   - **Expected**: every one of these succeeds with no error, no hung spinner, no crash — nothing
     in this app's core flows has a network dependency (see README's "Offline & privacy" section).
3. Still offline, open the debug telemetry screen and tap **"Send test event"**, then
   **"Send test exception"**, then **"Flush"**.
   - **Expected**: no crash. The recent-events/recent-errors lists update immediately regardless of
     whether a real provider is configured or reachable — that list is a local, in-memory record
     (`TelemetryDebugRecorder`), never a confirmation that anything left the device. "Flush" is
     always safe to tap offline: a real provider queues locally and retries later; this in-memory
     debug view doesn't wait on that.

## 3. Consent-state scenarios

1. Confirm the fresh-install state from §1 step 4 (`NotAsked`/`NotAsked`) — this is the only
   consent state currently reachable through any UI in the app.
2. Family tab → Privacy: confirm it's informational only (no toggle) — this is the current,
   correct implementation, not something to file as a bug.
3. Family tab → Data management → "Delete all local data": confirm telemetry consent (visible via
   the debug telemetry screen before/after) remains `NotAsked`/`NotAsked` afterward — deleting all
   local data resets consent the same way a family manually revoking would, so this should be a
   no-op from an already-`NotAsked` state, never a regression to some other value.
4. **Once a consent-toggle UI ships**, extend this checklist with manual passes for: grant
   analytics only, grant diagnostics only, grant both, deny both, revoke
   analytics after granting both, revoke diagnostics after granting both, reset via delete-all-data
   after granting. Until then, all of these combinations are covered only by the automated suite —
   see `TelemetryCoordinatorTest`, `DefaultTelemetryConsentRepositoryTest`,
   `TelemetryOfflineBehaviorTest`, and `TelemetryConsentOfflineBehaviorTest`.

## 4. Debug telemetry screen walkthrough

Family tab → About → "Open debug telemetry tools" (debug builds only — confirm the button itself
is **absent** on a release-configured build; `AboutViewModel` gates it on `AppConfiguration.debug`).

| Action | Expected result |
|---|---|
| Refresh | Re-pulls consent, provider status, and the three debug histories; no crash regardless of network state. |
| Flush | Calls `ProductAnalytics.flush()`; never crashes, including with no provider configured or offline. |
| Clear history | Empties the three in-memory histories (events/breadcrumbs/errors) shown on screen; a subsequent Refresh confirms they're empty. |
| Send test event | Fires `debug_test_event` through the real pipeline; appears in "recent events" immediately if analytics collection happens to be enabled, otherwise is silently dropped by `TelemetryCoordinator`'s own consent gate — either way, no crash. |
| Send test exception | Calls `captureHandledException` with a fixed synthetic exception; appears in "recent provider errors" under the same consent gating as above. |

Confirm none of the on-screen fields ever show a complete SDK key, a full DSN, a RevenueCat App
User ID, a PostHog distinct id, a receipt/purchase token, or any family/memory content — see
`docs/debug-telemetry.md`'s own description of `DebugTelemetryUiState`.

## 5. Regression watch

Re-run after any change to `TelemetryCoordinator`, the consent repository/cache, the debug
decorators, or `TelemetryEventRegistry`:

- [ ] Fresh install still completes onboarding fully offline (§2).
- [ ] Debug telemetry screen still renders with no crash on both a configured and an unconfigured
      build.
- [ ] `./gradlew :shared:testAndroidHostTest` is green (covers the automated half of every scenario
      above except the truly manual "look at the screen" steps).

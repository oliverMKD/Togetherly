# Telemetry architecture (Step 14.1)

Step 14.1 established provider-neutral consent, contracts, and privacy-allowlist architecture
before any real analytics/diagnostics SDK existed. **Step 14.2 added the real PostHog KMP SDK**
behind `ProductAnalytics` — see [analytics-setup.md](analytics-setup.md) for that integration's own
setup/operational details; this file remains the architecture reference (consent model, contracts,
validator) both steps share. **Sentry/`OperationalDiagnostics` still has no real SDK** — that
binding still resolves to a debug-only, log-printing stand-in or a true no-op (see
[Debug and no-op implementations](#debug-and-no-op-implementations)); nothing in this app has ever
transmitted diagnostics data.

## Consent model

`TelemetryConsent` (`domain/telemetry/TelemetryConsent.kt`) pairs two fully independent
`ConsentDecision`s — `analytics` and `diagnostics`. `ConsentDecision` is `NotAsked` / `Granted` /
`Denied`, never a `Boolean`: `NotAsked` and `Denied` are different facts ("we haven't asked" versus
"the family said no") and must never be conflated. Both default to `NotAsked`, which this app also
treats as "collection disabled" — nothing is ever collected unless a decision is explicitly
`Granted`.

Granting one category never grants or implies the other. There is no UI in this step to change
consent (`feature/family/presentation/PrivacyScreen.kt` still shows only the static Step 13.5
summary) — this step is the architecture a future consent-toggle screen will call into, via
`TelemetryConsentRepository`.

### Why this isn't `PrivacyPreferences`

`domain/family/PrivacyPreferences.diagnosticsEnabled` (Step 13.1) was an inert placeholder Boolean
with nothing reading it. It cannot represent `NotAsked` distinctly from `Denied`, and it's scoped
to `FamilyProfile`'s own lifecycle (a per-family Room row that doesn't exist before onboarding).
Telemetry consent needs to be readable from app startup, independent of whether onboarding has
happened yet — so it's its own repository, persisted separately (see
[Persistence](#persistence)). `PrivacyPreferences.diagnosticsEnabled` remains exactly as inert as
before; its own KDoc now points here.

## Provider-neutral contracts

All in `core/telemetry/` (the same "platform/infra capability boundary" package
`core/notification/ReminderScheduler.kt` and `core/media/AppSettingsLauncher.kt` already
establish):

- **`ProductAnalytics`** — `capture(event)`, `screen(screen)`, `flush()`, `setCollectionEnabled(enabled)`, `reset()`.
- **`OperationalDiagnostics`** — `captureHandledException(throwable, context)`, `addBreadcrumb(breadcrumb)`, `setCollectionEnabled(enabled)`, `clearContext()`.

Neither interface mentions PostHog, Sentry, or any other vendor. Neither has an `identify(userId)`
method — granting consent must never itself identify an otherwise-anonymous family (see
[RevenueCat and analytics: two separate identities](#revenuecat-and-analytics-two-separate-identities)).

`TelemetryConsentRepository` (`domain/telemetry/repository/`) is the single source of truth for
consent: `observeConsent(): Flow<TelemetryConsent>`, `updateAnalyticsConsent`/`updateDiagnosticsConsent(decision)`,
and `resetConsent()` (used by local data deletion — see below). No method returns `DataResult` —
this is local, always-available key-value state, the same simplification
`data/purchase/EntitlementCache`'s own `load`/`save` already make; an implementation must still
never let a storage exception reach a caller (see `DefaultTelemetryConsentRepository`'s own KDoc).

## Typed events, not strings

`AnalyticsEvent` (`core/telemetry/AnalyticsEvent.kt`) is a `sealed interface` — every event Togetherly
can ever send is declared in that one file (a Kotlin sealed hierarchy requires it), so the full
vocabulary is always reviewable in one place. `AnalyticsValue` is a closed four-variant type
(`Text`/`Number`/`Decimal`/`BooleanValue`) — no domain model, platform object, list, or nested
structure can reach a provider; the type system enforces this, not a runtime check.

Step 14.1 shipped three illustrative events only, to prove the framework compiled and validated end
to end — none were wired into feature code. **Step 14.3 replaced that placeholder vocabulary with
the real, deliberate taxonomy actually instrumented across every feature** — see
[analytics-event-taxonomy.md](analytics-event-taxonomy.md) for the full event list, per-event
trigger/purpose/properties, and the real `ProductAnalytics.capture`/`.screen` call sites in each
ViewModel.

## Privacy allowlist

`TelemetryPrivacyValidator` (`core/telemetry/TelemetryPrivacyValidator.kt`) is the gate every event
and every diagnostic free-text value passes through before a provider ever sees it:

- **Allowlist first.** `TelemetryEventRegistry` maps each registered event name to its own set of
  allowed property names. An event not in the registry is rejected outright; a registered event
  with even one property name outside its own schema is rejected outright. This is the primary
  defense — "prefer allowlisting over trying to detect every possible type of personal data."
- **A small forbidden-key list** (`email`, `phone`, `child_name`, `note`, `photo`, `receipt`,
  `purchase_token`, `location`, ...) is checked before the allowlist, as defense in depth against a
  future schema being misconfigured.
- **Text-value heuristics** (`isSafeText`, also reused directly by the diagnostics debug
  implementation for breadcrumb/context values, which have no per-key allowlist): rejects blank,
  over 200 characters, email-like (`x@y.z` shape), URL-like (`://` or `www.`), file-path-like
  (contains `/` or `\`), or newline-containing text.
- **Never logs a rejected value** — a `Rejected` result carries only a short, fixed technical
  reason string (`"unregistered property for event"`, never the value itself). Both debug
  implementations only ever log that reason.

## Debug and no-op implementations

Selected in `app/di/TelemetryModule.kt`:

| Build | `ProductAnalytics` (Step 14.2) | `OperationalDiagnostics` (still Step 14.1 — no Sentry SDK yet) |
|---|---|---|
| Missing key (either build type) | `NoOpProductAnalytics` — does nothing, unconditionally | — |
| Release, key present | `PostHogProductAnalytics` (real PostHog) | `NoOpOperationalDiagnostics` — does nothing, unconditionally |
| Debug, key present | `PostHogProductAnalytics` (real PostHog, `PostHogConfig.debug = true`) | `DebugOperationalDiagnostics` — prints a safe, validated, structured line via `AppLogger` |

`ProductAnalytics` no longer resolves to `DebugProductAnalytics` by default — a real PostHog
project (even a dev/staging one) now gives debug builds a real event stream to inspect, via the
native SDK's own debug logging. `DebugProductAnalytics` remains fully implemented and fully tested
(`DebugProductAnalyticsTest`) as an available, SDK-free `ProductAnalytics` implementation, just not
the default binding — see [analytics-setup.md](analytics-setup.md) for exactly how `PostHogProductAnalytics`
is selected and configured.

`DebugOperationalDiagnostics` (still the default for debug builds, no Sentry SDK integrated yet)
starts with collection disabled and only ever acts once `setCollectionEnabled(true)` has been
called — nothing is printed, not even a rejection warning, before that. It runs every value through
the same `TelemetryPrivacyValidator` a real provider implementation would use, so what a developer
sees printed is exactly what would have been sent.

A future step swaps `NoOp*`/`Debug*` for a real Sentry-backed `OperationalDiagnostics` implementation
behind the same interface — nothing else in the app (the coordinator, the consent repository, the
validator, `KoinConfiguration`) needs to change when that happens.

## `TelemetryCoordinator`

The one place consent turns into provider state (`core/telemetry/TelemetryCoordinator.kt`) — no
feature code calls `setCollectionEnabled` itself. `start()` is called exactly once, from
`app/di/KoinConfiguration.initKoin`, the same call-site shape `RevenueCatConfigurator.configure`
already establishes (synchronously, right after Koin's modules are wired, before any
`@Composable`/`ViewModel` can run).

- Both providers are forced disabled *before* the coordinator ever subscribes to
  `observeConsent()` — collection is off the instant this object exists.
- Every subsequent consent emission drives `analytics`/`diagnostics` independently.
- A transition *out of* `Granted` (revoking, or resetting to `NotAsked`) calls the provider's own
  `reset()`/`clearContext()` — "revocation must stop future collection promptly" and "resets
  provider state when consent is revoked" both fall out of this one rule.
- Every provider call is wrapped in `runCatching` and logged on failure, never rethrown — a
  misbehaving provider can never break app startup or stop the coordinator from reacting to future
  consent changes. `KoinConfiguration.initKoin` wraps the `start()` call itself in `runCatching`
  too, as a second layer.

## Persistence

`TelemetryConsentCache` (`data/telemetry/TelemetryConsentCache.kt`) reuses `DatabaseMetadataDao`
(the same small Room key-value table `data/purchase/EntitlementCache` already uses) under its own
key (`telemetry_consent`) — no new Room table, no migration. `DefaultTelemetryConsentRepository`
seeds in-memory state to `TelemetryConsent.default()` immediately and reconciles from the cache
asynchronously (the same "safe default now, real state shortly after" shape
`RevenueCatEntitlementRepository` already uses for entitlement state).

## Local data deletion resets consent

`DeleteAllLocalData` (Step 13.7) calls `TelemetryConsentRepository.resetConsent()` as one of its
best-effort steps (after the database wipe has already succeeded — see that class's own KDoc on
ordering). This resets both decisions to `NotAsked` and deletes the persisted consent row.
`TelemetryCoordinator`, already subscribed to `observeConsent()` for the whole app process, reacts
to that transition exactly like a family manually revoking consent — disabling both providers and
resetting their state. `DeleteAllLocalData` itself never touches `ProductAnalytics`/
`OperationalDiagnostics` directly.

**This does not erase analytics or diagnostics data already transmitted before deletion**, if any
was ever sent while consent was granted. Only local, on-device state — the consent record and
in-memory provider state — is Togetherly's own to erase. A provider's own server-side record (once
a real provider exists) is that provider's data, governed by its own retention/deletion tooling,
not by anything this app does locally. Never claim otherwise in UI copy.

## RevenueCat and analytics: two separate identities

RevenueCat's own anonymous, on-device, SDK-managed identity (see
`data/purchase/RevenueCatConfigurator`'s own KDoc: no `appUserId` is ever passed, `logIn`/`logOut`
are never called) is entirely separate from anything `ProductAnalytics` does. This step's own
`ProductAnalytics` interface has no `identify` method at all — granting analytics consent must
never itself identify an otherwise-anonymous family, and must never read or forward RevenueCat's
identifier. RevenueCat public/secret keys must never appear in an event property (see the
forbidden-key list above) or in any diagnostic context.

## What Togetherly may and may not send

Never: family name, parent name, child name, exact age or birth date, family profile free text,
memory notes, photos, voice recordings, media file names or paths, search queries, quest reaction
text, notification text, a store receipt or purchase token, RevenueCat public/secret keys, a
PostHog/Sentry key in an event property, precise location, contact information, or advertising
identifiers.

May: content-catalogue identifiers (predefined quest/pack IDs — they identify Togetherly content,
never a family member) and broad, predefined categories (`quest_category`, `duration_bucket`,
`energy_level`, `access_required`, `paywall_context`, `package_type`, and the rest of
[analytics-event-taxonomy.md](analytics-event-taxonomy.md)'s own common-properties table) — never an
exact participant age, and never a property combination rich enough to fingerprint one specific
family.

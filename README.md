# Togetherly

[![CI](https://github.com/oliverMKD/Togetherly/actions/workflows/ci.yml/badge.svg)](https://github.com/oliverMKD/Togetherly/actions/workflows/ci.yml)
[![Apple CI](https://github.com/oliverMKD/Togetherly/actions/workflows/apple-ci.yml/badge.svg)](https://github.com/oliverMKD/Togetherly/actions/workflows/apple-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/docs/multiplatform.html)

Togetherly helps families put the phone down and spend meaningful time together through small
cooperative quests.

## The problem

Family time increasingly competes with individual screen time, and most "family" apps solve this
by asking a family to hand over their data to a cloud account instead. Togetherly takes a
different approach: no login, no account, no cloud sync. A family's activity, preferences, and
private memories live only on their own device.

## Product concept

Every day, Togetherly offers one short **quest** — a small, concrete, shared activity (talk, make
something, move, be kind, discover something, be silly, or revisit a memory) sized to the family's
own stated duration, energy level, and location preferences. A family completes it together, then
can optionally capture a private **memory**: a short note, a photo, or a voice recording, kept only
on-device. Over time, completed quests build a **Journey** — a lightweight timeline of what a
family has done together.

## Main features

- **Daily quest** — one recommended activity per day, chosen from the bundled quest catalogue
  against the family's own preferences (category, duration, energy, location); can be swapped for
  a fresh recommendation.
- **Explore** — search and filter the full quest catalogue directly, save quests for later.
- **Quest Mode** — a focused, timer-optional screen for actually doing a quest together.
- **Completion memories** — an optional note, photo, or voice recording per completed quest,
  stored in each platform's own private app storage, never uploaded anywhere.
- **Journey** — a derived timeline/constellation of completed quests and their memories.
- **Reminders** — local, opt-in daily notifications on the family's own chosen days/time.
- **Family profile & preferences** — age bands, interests, duration/energy/location preferences,
  reminder settings, and privacy controls, all stored locally.
- **Local data deletion** — a family can delete memories, quest history, or everything, entirely
  from the device, at any time.
- **Family Plus** — an optional subscription unlocking additional quest content (see below).

## Architecture overview

Kotlin Multiplatform + Compose Multiplatform, targeting **Android and iOS only** (no Desktop/JVM
target), built around a single `shared` module with strict domain/data/presentation boundaries:
`domain` (pure model, repository interfaces, use cases) never depends on `content` or `data`;
`content` (the bundled quest catalogue) and `data` (Room persistence, RevenueCat, PostHog, Sentry)
each implement a `domain` repository interface and are only ever resolved through it; `feature`
(presentation) depends only on `domain`. See [docs/architecture.md](docs/architecture.md) for the
full layer map, module dependency diagram, and conventions.

## Technology stack

- **Kotlin Multiplatform** + **Compose Multiplatform** (shared UI, Android + iOS)
- **Koin** for dependency injection
- **Room** (KMP) for local persistence — the only datastore; no backend, no cloud database
- **Kotlinx** Coroutines, Serialization, DateTime, Collections-Immutable
- **AndroidX Navigation Compose** (type-safe destinations)
- **RevenueCat** KMP SDK — the optional Family Plus subscription
- **PostHog** KMP SDK — optional, consent-gated product analytics
- **Sentry** KMP SDK — optional, consent-gated crash/error diagnostics

## Module overview

- **`shared`** — everything: domain model, use cases, the local content pipeline, persistence, DI
  wiring, and the shared Compose Multiplatform UI. `commonMain` holds platform-independent code;
  `androidMain`/`iosMain` hold the small `expect`/`actual` platform seams (dispatchers, ID
  generation, platform-specific media/notification/purchase wiring).
- **`androidApp`** — the Android application shell.
- **`iosApp`** — the Xcode project hosting the iOS entry point, consuming `shared` as a framework.

## Offline-first and privacy

There is no account, no login, and no cloud sync anywhere in this app — every family's quest
history, preferences, and memories exist only in that device's own local Room database and private
filesystem. Product analytics and crash diagnostics are both off by default, provider-neutral
(the app never hardcodes a vendor into feature code), consent-gated per category, and fall back to
a full no-op if never configured — the app is completely usable with neither ever turned on. See
[docs/privacy.md](docs/privacy.md), [docs/private-media.md](docs/private-media.md),
[docs/local-data-deletion.md](docs/local-data-deletion.md), and
[docs/telemetry.md](docs/telemetry.md).

## RevenueCat integration

RevenueCat is the one purchase boundary in the app (`data.purchase` — see
[docs/architecture.md](docs/architecture.md#purchase--revenuecat-boundary)): every family is
identified only by RevenueCat's own anonymous app-user id, never a Togetherly account, and no
RevenueCat type ever leaks past `data.purchase` into domain or feature code. See
[docs/revenuecat-setup.md](docs/revenuecat-setup.md) for API-key management and how to test
purchases without real money, and
[docs/revenuecat-posthog-integration.md](docs/revenuecat-posthog-integration.md) for how
subscription events are (optionally, consent-gated) linked to product analytics.

### Free vs. Family Plus

The full daily-quest/Explore/Journey/memory experience works without ever subscribing. Family Plus
is an optional subscription that unlocks additional premium quest content in the catalogue —
everything else in the app behaves identically on both tiers.

## Local development

- **Requirements**: JDK 17+ (via the Gradle toolchain), Android Studio or IntelliJ IDEA for
  Android, Xcode for iOS.
- **Configuration**: copy `local.properties.example` → `local.properties` and
  `iosApp/Configuration/*.local.xcconfig.example` → the matching `*.local.xcconfig` files. Every
  value is optional — the app runs fully in free mode with analytics/diagnostics disabled if none
  are configured. See [docs/configuration.md](docs/configuration.md) for what each value is, where
  it comes from, and how it's classified (client configuration vs. a secret that must never be
  committed), and [docs/revenuecat-setup.md](docs/revenuecat-setup.md) /
  [docs/analytics-setup.md](docs/analytics-setup.md) / [docs/sentry-setup.md](docs/sentry-setup.md)
  for full per-integration setup.

### Build

```
./gradlew :androidApp:assembleDebug   # Android debug build
```
For iOS, open [`iosApp`](iosApp) in Xcode and run it from there.

### Test

```
./gradlew :shared:testAndroidHostTest   # common + Android unit tests
./gradlew :shared:iosSimulatorArm64Test # iOS unit tests
./gradlew allTests                      # every target
```

## Current project status

Togetherly is under active development and is **not yet published** to the App Store or Google
Play. The core product flows described above — onboarding, daily quest, Explore, Quest Mode,
completion memories, Journey, reminders, family preferences, local data deletion, Family Plus
purchases, and consent-gated analytics/diagnostics — are implemented and tested on both platforms.
Pull requests are verified by CI (Android + Apple targets) — see [docs/ci.md](docs/ci.md). Release
signing and store listings are not yet set up.

## Roadmap

- Finish the debug-only telemetry verification tooling described in
  [docs/debug-telemetry.md](docs/debug-telemetry.md).
- Prepare Android/iOS release signing and store listings.

## Shipaton

Togetherly is being built as an entry for RevenueCat's Shipaton — a Family Plus subscription via
RevenueCat is a core part of that.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). This project also has a [Code of Conduct](CODE_OF_CONDUCT.md)
and a [security policy](SECURITY.md).

## License

MIT — see [LICENSE](LICENSE).

## Further documentation

- [docs/architecture.md](docs/architecture.md) — module layout, core conventions, presentation
  conventions, dependency injection, application startup behavior.
- [docs/design-system.md](docs/design-system.md) — the shared theme/token structure, the component
  library and its rules, and how to add a new component.
- [docs/navigation.md](docs/navigation.md) — the navigation library decision, the destination
  model, Bootstrap's routing logic, back-stack shape, and how to add a new top-level destination.
- [docs/content-system.md](docs/content-system.md) — the bundled quest catalogue: schema, how to
  add a quest or pack, ID/access conventions, the content-safety checklist, and catalogue
  versioning.
- [docs/persistence.md](docs/persistence.md) — the Room-backed local database: tables and
  ownership, transactional boundaries, full family-data deletion, media metadata versus media
  files, migration policy, and corruption policy.
- [docs/onboarding-qa.md](docs/onboarding-qa.md) — manual QA checklist for the bootstrap →
  onboarding → Main flow.

The `docs/` directory has further files on individual features (Today, Explore, Journey, Quest
Mode, the recommendation engine, reminders) not listed above.

# Togetherly performance audit

Audited on 2026-07-29 against the production-readiness baseline in
`docs/release-readiness.md`. This is a code-path audit with deterministic verification; it is not a
device benchmark. Wall-clock startup, frame timing, and memory numbers require release builds on
representative physical Android and iOS devices and remain follow-up work.

## Results

| Area | Status | Evidence and outcome |
|---|---|---|
| Application startup | Improved | Koin module wiring and the required RevenueCat configuration remain synchronous. Optional PostHog/Sentry coordinator and RevenueCat analytics-linker resolution previously ran before `initKoin` returned; they now launch on `AppDispatchers.default`. A deterministic scheduler test proves the caller returns before either task begins. |
| Room initialization | Ready | The Room singleton remains lazy. Both platform builders set Room's query coroutine context to `AppDispatchers.io`; no database query or builder was moved onto the UI thread. |
| Bundled catalogue and JSON | Ready | The resource is loaded by `BundledQuestRepository` on `AppDispatchers.io`. `DefaultQuestCatalogueLoader` protects loading with a `Mutex` and caches only successful validation. Existing tests prove 20 concurrent callers perform one resource read/parse and that a successful load is not reparsed. No additional cache was added because the existing cache already has the correct invalidation rule: process lifetime for an immutable bundled resource, with failures retryable. |
| Today recommendation | Improved | Before this change, every eligible candidate scanned dismissals, filtered all completions (twice), found a maximum, and sorted the complete history to obtain the same two categories. History is now indexed once per request into dismissed IDs, most-recent completion by quest, completed IDs, and the latest two categories. Per-candidate history checks are set/map lookups. This changes the repeated history work from approximately `O(quests × history + quests × history log history)` to `O(history log history + quests)` while preserving the existing deterministic policy tests. |
| Explore search/filtering | Ready | Search updates remain debounced at 300 ms after the initial immediate query and use `distinctUntilChanged`. Filtering remains in the ViewModel/use-case pipeline. The current bundled catalogue is 45 quests and 6 packs, so no unbounded or invalidation-free result cache was justified. |
| Lazy-list rendering | Improved | Explore previously put pack/search quest cards in eager `Column.forEach` blocks inside a vertically scrolling screen, allowing a search to compose all 51 catalogue entities at once. It now uses one `LazyColumn`; every pack and quest has a stable, type-prefixed ID key and a content type. The featured horizontal list remains lazy and also has stable keys. |
| Image/media loading | Ready | Media reads, writes, copies, normalization, and deletion remain wrapped in `AppDispatchers.io`. Catalogue artwork references are resource-backed and no duplicate image decoder or UI-thread file access was found. |
| Flow collection | Ready | Screen state uses `collectAsStateWithLifecycle`; one-off event collection is tied to `LaunchedEffect(viewModel)`. Repository/database flows are transformed in ViewModels and no composition-created duplicate collectors were found in the audited screens. |
| Compose recomposition | Ready | Catalogue loading/parsing is repository-owned rather than called from a Composable. Explore derived results remain state produced by its ViewModel; the UI now receives stable keyed lazy items. No expensive derived catalogue operation was found directly in composition. |
| RevenueCat | Ready | `RevenueCatConfigurator.configure` still has exactly one production call site, in `initKoin`, before premium state can be read. It remains synchronous because purchase state depends on it; the optional analytics bridge is deferred. |
| PostHog/Sentry | Improved | Their Koin providers can initialize native SDK adapters. Provider resolution and coordinator start are now off the entry-point critical path. Each optional startup integration is failure-isolated, so one exception neither delays startup nor prevents the other integration from starting. |

## Measurements and concrete evidence

- Bundled data size: 45 quests and 6 packs, validated by the existing catalogue content tests.
- Catalogue parse count: existing loader tests assert one read/parse after a successful load and one
  read/parse across 20 concurrent calls.
- Explore eager composition upper bound before: 6 pack cards plus 45 quest cards for a broad search.
  After: only the visible `LazyColumn` window and Compose's normal prefetch window are composed.
- Recommendation repeated work before: for `Q` surviving quests and `H` completion records, the
  policy performed `Q` full-history sorts plus multiple `Q × H` scans. After: one history sort,
  one completion pass, one dismissal pass, and constant-time per-quest history lookups.
- Startup scheduling: `KoinConfigurationTest.telemetryStartupIsDeferredOffTheCallingThread` uses a
  paused deterministic dispatcher and asserts neither telemetry task starts on the caller; both
  start only after the dispatcher advances.

These are structural and deterministic measurements, not claims about milliseconds or frames.

## Remaining work

1. Capture Android Macrobenchmark cold/warm startup (`timeToInitialDisplay` and
   `timeToFullDisplay`) from a signed release/profileable build on representative low- and
   mid-range physical devices.
2. Capture iOS launch and first-interaction traces with Instruments on a physical supported device;
   separate Kotlin/Native initialization, Koin, Room, and native telemetry SDK time.
3. Add Compose Macrobenchmark scrolling/search scenarios for Explore and record frame-duration
   percentiles and jank before changing list prefetch or image policy.
4. Add production observability for catalogue load/validation duration and Today recommendation
   duration using privacy-safe local tracing first; do not add a cache unless measurements justify
   it and an invalidation owner is defined.
5. Revisit telemetry startup only if device traces show ViewModel access racing provider startup.
   Any further change should retain consent gating and the one-provider-instance Koin bindings.

## Verification commands and results

| Command | Result |
|---|---|
| `./gradlew :shared:testAndroidHostTest` | Passed: 1,306 tests, 0 skipped, 0 failures, 0 errors. This includes the catalogue and new startup scheduling tests. |
| `./gradlew :shared:compileAndroidMain :shared:compileKotlinIosSimulatorArm64` | Passed for Android and the arm64 iOS simulator target. |
| `./gradlew :androidApp:assembleDebug :androidApp:compileReleaseKotlin` | Passed; debug APK assembled and release Kotlin compiled without requiring signing. |
| `./gradlew :androidApp:lintDebug` | Passed; report written to `androidApp/build/reports/lint-results-debug.html`. |
| `./gradlew :shared:allTests` | Android host tests passed, then the iOS executable suite was blocked after 1,297 tests by the known `IosReminderSchedulerTest.schedulingNeverThrowsAndSucceeds` process abort described below. |
| `git diff --check` | Passed. No executable formatter is configured in the repository. |

The `allTests` iOS failure is external to these changes: a command-line Kotlin/Native test
executable has no application bundle, so `UNUserNotificationCenter.currentNotificationCenter`
aborts with `bundleProxyForCurrentProcess is nil`. The iOS production source compiled successfully;
the failing notification path was not changed in this step.

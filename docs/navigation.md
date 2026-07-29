# Navigation

Togetherly's navigation is shared Compose Multiplatform code in `com.togetherly.navigation`
(`shared/src/commonMain`) — there is no per-platform navigation implementation. This document
covers the library decision, the destination model, Bootstrap's routing logic, the Main shell, and
how to extend any of them.

## Library decision

**`org.jetbrains.androidx.navigation:navigation-compose` (stable, currently `2.9.2`)** —
JetBrains' Kotlin-Multiplatform-published build of AndroidX Navigation Compose, with genuine iOS
support and type-safe `@Serializable` routes (the same route-as-object API AndroidX Navigation
Compose shipped in `2.8.0`).

This was chosen over **Navigation 3**. Both a Google-native, Android-only artifact
(`androidx.navigation3`) and a genuinely multiplatform JetBrains build
(`org.jetbrains.androidx.navigation3`) exist and are stable, but the multiplatform Navigation 3
build requires implementing polymorphic serialization for destination keys by hand on non-JVM
targets (`rememberNavBackStack()` with a `SerializersModule`) since Kotlin/Native can't use
reflection-based serialization the way the JVM/Android path does — real, non-trivial extra work
this project has no matching need for yet. `navigation-compose` gives the same type-safe route
ergonomics, is more mature, and needs no iOS-specific serialization work. Revisit Navigation 3 if a
concrete need for its adaptive/multi-pane primitives shows up later.

`kotlinx-serialization-json` (already a project dependency) backs the `@Serializable` routes; no
extra dependency was needed for that part.

## Destination model

Three files in `com.togetherly.navigation.destination`, each a sealed interface of
`@Serializable data object`/`data class` route types — never a string route, never a domain object
passed as a navigation argument:

- **`RootDestination`** — `Bootstrap`, `Onboarding`, `Main`, `QuestDetail(questId: QuestId)`,
  `QuestMode(completionId: CompletionId)`, `CompletionCelebration(completionId: CompletionId)`
  (Step 9.6). The root graph's only destinations.
- **`MainDestination`** — `Today`, `Explore`, `Journey`, `Family`. Main's four bottom-nav tabs,
  each currently a flat destination (no nested sub-graph).

`QuestDetail`/`QuestMode` (Step 8.6) are the first *parameterized* destinations, and the first
proof that a screen inside a `MainDestination` tab can reach a destination above the tab bar:
`TodayEvent.OpenQuestDetail` climbs out through a plain callback (`MainShell(onOpenQuestDetail = ...)`)
to `TogetherlyNavHost`, which pushes `RootDestination.QuestDetail` onto the *root* back stack —
never a destination added to `MainShell`'s own nested tab `NavHost`. Both carry only a typed ID
(`QuestId`/`CompletionId`, both `@Serializable` value classes) — never a domain model
(`FamilyQuest`/`ActiveQuestSession`); each screen resolves its own data fresh from the repository.
`QuestMode` renders with no bottom navigation for free, simply by being a root-level destination
composed outside `MainShell`'s `TogetherlyScreen` — the "full-screen future content" seam this
document used to describe as open is exactly what `QuestMode` now fills.

`CompletionCelebration` (Step 9.6) carries only a `completionId`, never the
`QuestCompletion`/`FamilyQuest` themselves — the celebration screen resolves both fresh from their
repositories. `QuestMode` navigates to it with `popUpTo(RootDestination.Main) { inclusive = false }`,
stripping `QuestDetail`/`QuestMode` off the back stack in the same step so Back from the celebration
can never reopen a session that has already been completed and cleared — see
[quest-mode.md](quest-mode.md#completion-transaction).

Onboarding has **no destination type of its own** — it's a single `RootDestination.Onboarding`
entry hosting one internal state machine
([`OnboardingUiState.step`](../shared/src/commonMain/kotlin/com/togetherly/feature/onboarding/model/OnboardingUiState.kt)),
not one `NavDestination` per form step. See that file's own KDoc, and
`feature.onboarding.navigation.OnboardingStepFlow` for the (non-`NavController`) step-order logic.

## Bootstrap: the family profile is the source of truth

[`BootstrapViewModel`](../shared/src/commonMain/kotlin/com/togetherly/navigation/state/BootstrapViewModel.kt)
decides `RootDestination.Onboarding` vs. `RootDestination.Main` by calling
`FamilyRepository.observeProfile()` directly — **never** a stored "has onboarded" boolean. A
`null` profile means onboarding hasn't created one yet; a non-null profile means it has. This is
deliberate: a family profile deleted outside the app's own flow (a fresh install restoring a
backup, a support-driven data reset) is reflected immediately, with nothing to fall out of sync.

`observeProfile()`'s `Flow` terminates after emitting an error (see `RoomFamilyRepository`'s own
`catch` operator) rather than recovering on its own — `BootstrapViewModel.retry()` re-subscribes a
fresh call via an internal `retrySignal`, not by assuming the existing `Flow` will resume.

`BootstrapScreen` renders the same branded loading UI for `Loading` and for the (single-frame)
instant between the state resolving and navigation actually firing — this is what keeps onboarding
or Main from ever flashing underneath Bootstrap.

## Back-stack shape

- `RootDestination.Bootstrap` is **replaced** (`popUpTo(Bootstrap) { inclusive = true }`), never
  pushed onto — there is no route back to it once a decision is made.
- Completing onboarding (`OnboardingEvent.FamilyCreated`, fired only after
  `CreateFamilyProfile` has actually committed the write) replaces `Onboarding` with `Main`
  (`popUpTo(Onboarding) { inclusive = true }`) — Back from Main cannot return to onboarding.
- Onboarding's own internal Back (its top-bar icon, on the first internal step) fires
  `OnboardingEvent.NavigateBack`, which the host turns into a plain `navController.popBackStack()`.
  Since Bootstrap is already gone, this leaves nothing — the platform's own exit/dismiss behavior
  takes over with no extra handling.
- `QuestMode`'s top-bar close ("✕") shows an exit confirmation dialog before navigating back; the
  system back gesture/button is not intercepted there and pops the stack directly instead — a
  known, audited asymmetry with no data-safety impact (the active session is untouched either way).
  See [quest-mode.md](quest-mode.md#exit-and-abandon-behavior).
- Each `MainDestination` tab keeps its own back stack across switches
  (`popUpTo(startDestination) { saveState = true }` / `restoreState = true`, the standard
  Navigation Compose bottom-nav recipe) — see `MainShell`. Re-selecting the already-active tab is a
  no-op (`launchSingleTop`) rather than popping to some "root," since every tab is flat today; a
  future nested flow within a tab is exactly where "return to root on reselect" would be added, at
  that same call site.

## Main shell

[`MainShell`](../shared/src/commonMain/kotlin/com/togetherly/navigation/shell/MainShell.kt) owns
its own nested `NavController`/`NavHost` for the four tabs and renders
[`TogetherlyBottomNavigationBar`](design-system.md) — bottom navigation exists **only** inside this
shell; neither Bootstrap nor Onboarding wrap their content in it. `MainTab` is the one place that
maps a type-safe `MainDestination` route onto the design system's own navigation-agnostic
`TogetherlyDestination` identity, exactly as that component's own KDoc anticipates.

## Parameterized destinations and their ViewModels

A destination that carries an argument (`QuestDetail(questId)`, `QuestMode(completionId)`) reads it
at the `composable<T>` boundary via `backStackEntry.toRoute<T>()`, then passes it straight into the
screen's `Route` composable — never into a `SavedStateHandle` read inside the `ViewModel`. The
`ViewModel` itself takes that ID as a plain constructor parameter, resolved through Koin's
`parametersOf` at the call site:

```kotlin
// Koin module
factory { params -> QuestDetailViewModel(params.get(), get(), get(), get(), get(), get()) }

// Route composable
viewModel: QuestDetailViewModel = koinViewModel(key = questId.value) { parametersOf(questId) }
```

The `key = questId.value` matters: without a distinct key, Koin/Compose would reuse the same
`QuestDetailViewModel` instance across two different quest IDs viewed in the same composition
lifetime (e.g. Back then opening a different quest while the first instance is still scoped) —
keying by the ID itself guarantees a fresh instance per distinct quest.

## Adding a new top-level destination

1. Add a `@Serializable data object`/`data class` to the relevant sealed interface in
   `navigation.destination` (`RootDestination` for a new root-level flow, `MainDestination` for a
   new bottom-nav tab). If it carries an argument, that argument must be a typed ID — never a
   domain object.
2. Register it with `composable<YourDestination> { ... }` in `TogetherlyNavHost` (root) or
   `MainShell`'s inner `NavHost` (a tab). For a parameterized destination, read the argument via
   `backStackEntry.toRoute<YourDestination>()`.
3. If it needs a `ViewModel`, add a `factory { ... }` to `presentationModule` (`app.di`) — plain
   `factory { YourViewModel(get()) }` if parameterless, or `factory { params -> YourViewModel(params.get(), get(), ...) }`
   if it needs a nav-arg ID (see above) — and resolve it with `koinViewModel()` at the route
   composable. Never construct a `ViewModel` directly, and never resolve one inside a small reusable
   component.
4. If it's a new bottom-nav tab, add the matching `TogetherlyDestination` case and extend
   `MainShell`'s `MAIN_TABS` list with its label resource and icon.
5. Never pass a domain object as a route argument — pass a typed ID or nothing.

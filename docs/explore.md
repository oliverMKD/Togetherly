# Explore, Packs, Saved Quests and Family Plus Previews

Step 12 built Togetherly's Explore tab, filters, saved quests, pack details, and the premium
preview/start-authorization rules that gate Family Plus content everywhere in the app. This
document describes that system: architecture, content rules, filter/search behavior, saved-quest
sync, premium preview rules, and how to test it.

## Architecture

```
domain.explore.usecase
  ObserveExploreCatalogueUseCase   quests + packs, as one Flow, no filtering
  SearchQuestsUseCase              pure title/summary/category-name text search
  FilterQuestsUseCase              pure AND-combination across 6 dimensions
  GetQuestPackUseCase              one pack + its resolved member quests
  ObserveSavedQuestIdsUseCase      Set<QuestId> only, for O(1) grid lookups
  ObserveSavedQuestsUseCase        full FamilyQuest objects, for the Saved screen
  ToggleSavedQuestUseCase          resolves current state, flips it
  EvaluateQuestAccessUseCase       true = accessible, via QuestAccessPolicy
  EvaluatePackAccessUseCase        same, for a whole pack
```

Each use case does exactly one thing; ViewModels compose them. None of them import a RevenueCat
type — access is always evaluated through the provider-neutral `QuestAccessPolicy` against an
`AccessSnapshot` read from `EntitlementRepository`.

### Screens and their ViewModels

| Screen | ViewModel | Reached from |
|---|---|---|
| Explore home | `ExploreViewModel` | `MainShell`'s Explore tab |
| Explore filters | `ExploreFiltersViewModel` | Explore's Filters button → `RootDestination.ExploreFilters` |
| Saved quests | `SavedViewModel` | Explore's Saved action → `RootDestination.Saved` |
| Pack Details | `PackDetailsViewModel` | Explore pack card / Saved / search → `RootDestination.PackDetails(packId)` |
| Quest Details | `QuestDetailViewModel` | Today, Explore, Pack Details, Saved, or a deep link → `RootDestination.QuestDetail(questId, source)` |

`QuestDetail` is a single screen reused by every entry point — there is no separate
"premium preview" screen. `QuestDetailUiState.Content.locked` (and the mapper's `locked` parameter)
is what switches it between full detail and preview rendering; see
[Premium preview rules](#premium-preview-rules) below.

### The committed-filters problem, and `ExploreFilterStore`

`RootDestination.ExploreFilters` is a real, separate nav destination (not an in-VM sheet like
Today's own filters), so its result has to flow *back* to Explore's already-running
`ExploreViewModel` instance. `ExploreFilterStore` (a Koin `single`) is the shared, in-memory source
of truth both ViewModels read: `ExploreViewModel` observes `store.filters` as part of its own
combine pipeline; `ExploreFiltersViewModel` reads the current value once as its draft's starting
point and calls `store.commit(draft)` on Apply. Explore's own quick category chip writes to the
same store directly (no separate "apply" step for that one dimension), so the chip and the filter
sheet's own Category group can never disagree.

Explore's own `ExploreViewModel` instance survives navigating to Filters/Pack Details/Saved and
back — Navigation Compose keeps a backstack entry's `ViewModelStore` alive as long as the entry
isn't popped, so this is "for free," not something Explore had to build.

## Packs and quest counts

Six packs, unchanged since Step 12.2:

| Pack | Access | Quests |
|---|---|---|
| Quick Wins | Free | 11 |
| Everyday Together | Free | 10 |
| Creative Sparks | Family Plus | 6 |
| Calm & Connected | Family Plus | 6 |
| Move Together | Family Plus | 6 |
| Weekend Adventures | Family Plus | 6 |

45 quests total: 21 free, 24 Family Plus.

## Free versus Family Plus content rules

- A free pack must only ever contain free quests (`CONTRADICTORY_STATE` validation error
  otherwise) — see [content-system.md](content-system.md).
- Premium packs and premium quests are **never excluded** from Explore results, search, or Pack
  Details' own quest list — `locked` is a presentational flag, not a filter. A free family sees
  every pack and every quest; only what they can *start* is gated.
- Filtering by `QuestAccessFilter.PREMIUM` narrows results to declared-premium content but never
  opens a paywall by itself — selecting a filter is never a purchase trigger.
- Saving, reading a preview, and browsing are always available to free families regardless of
  access level.

## Search and filter behavior

- Search text is debounced 300ms (`ExploreViewModel.debouncedSearchQuery`) but the *text field
  display* updates on every keystroke immediately — only the actual quest/pack recompute waits for
  the debounce, so typing never feels laggy.
- The very first search-flow emission is never debounced (`merge(take(1), drop(1).debounce(...))`)
  — otherwise the initial catalogue load would stall behind an artificial 300ms wait nobody's
  typing caused.
- Filters (`ExploreFilters`: category, duration, energy, location, age band, access) all combine
  with search and each other via AND, entirely through `FilterQuestsUseCase`/`SearchQuestsUseCase`
  — never hand-rolled matching logic in a ViewModel or composable.
- Packs are matched by a local title/description text search while a query is active (no domain
  use case does pack search — packs are too few, and too varied in category, for that to need its
  own use case) but are **never** filtered by the 6-dimension `ExploreFilters` — most packs mix
  categories on purpose.
- The Explore Filters screen holds an isolated **draft** (`ExploreFiltersUiState.draft`) that only
  reaches `ExploreFilterStore` on Apply; Cancel and Clear All never touch the store directly (Clear
  All resets the draft; Apply is the only commit path).
- `ExploreFilters.activeCount` is what the "Filters (N)" badge on Explore's home screen counts —
  every one of the six dimensions, category included.

## Saved-quest behavior

- `SavedQuestRepository` is the single source of truth. Explore, Pack Details, Quest Details, and
  Saved all observe it (`ObserveSavedQuestIdsUseCase` for grids, `ObserveSavedQuestsUseCase` for
  the full Saved list) — no screen copies saved state into its own local model.
- A premium quest can be saved by a free family; saving never requires or checks access.
- Unsaving updates every screen reactively and immediately — there is no explicit refresh anywhere.
- The Saved screen's empty state: "No saved quests yet — Save activities you would like to try
  together later."

## Premium preview rules

A locked quest's `QuestDetailUi` is mapped with `locked = true`, which makes
`instructions`/`hints` **genuinely empty lists** — not merely hidden by the screen. This is
deliberate: a rendering bug can only fail *closed* (nothing to accidentally reveal, including
through the accessibility tree or a UI test's `printToString()`) if the real content was never
mapped into the model at all.

A locked preview still shows: title, summary, duration, energy, location, age suitability,
materials ("at a high level" — the existing plain material-name list, nothing more detailed exists
to trim), pack title, the safety note (safety information costs nothing to reveal and can matter
before a purchase decision), a "Family Plus" badge, and an explicit locked-content section (never a
fake blur — a real `Column` with real text, so screen readers and font scaling both work normally)
with the copy: "This quest is part of Family Plus. Unlock the full activity and more ways to spend
meaningful time together."

## Start authorization rules

Every route into Quest Mode is a distinct nav destination pushed on top of the *same*
`QuestDetail` screen — Today, Explore, Pack Details, Saved, and a deep link all funnel through
`QuestDetailViewModel.onAction(StartClicked)`. That single choke point is protected two ways:

1. **`PrepareQuestStartUseCase`** — called first. Returns `Allowed(quest)` /
   `RequiresFamilyPlus(questId)` / `NotFound`. On `RequiresFamilyPlus`, `QuestDetailViewModel`
   emits the same `OpenPaywall(questId)` event `UnlockClicked` uses — no session is created, no
   navigation to Quest Mode happens.
2. **`StartQuest`** — the actual session-creation boundary — re-checks access itself, returning
   `AppError.Validation(ValidationError.PREMIUM_ACCESS_REQUIRED)` if the quest is premium and
   inaccessible. This is defense-in-depth: even if a future caller forgets to call
   `PrepareQuestStartUseCase` first, or a UI flag is stale, the mutation itself refuses. "Do not
   rely only on a disabled button" — the disabled/hidden Start button is a UX nicety, never the
   actual authorization.

A `content.locked` guard in `onStartClicked` is a fast client-side check (skip the round trip for
the common case) — it is not what makes this safe; the two checks above are.

## Paywall and return-navigation behavior

`RootDestination.FamilyPlusPaywall(context, questId?, packId?)` is one destination used from every
paywall trigger. `context` picks the intro copy/analytics label (`PaywallContext.PREMIUM_QUEST`,
`PREMIUM_PACK`, `PREMIUM_REROLL`, `FAMILY_PLUS_MANAGEMENT`); purchases always use the current
RevenueCat offering regardless of why the paywall opened. `questId`/`packId` are "return to"
markers, not read by the paywall screen itself.

Child-facing triggers (a locked quest's Unlock action, a locked pack's Unlock action, Today's
reroll limit) are wrapped in `TogetherlyParentalGateDialog` before navigating
(`TogetherlyNavHost`'s `pendingGatedNavigation`); a parent-facing entry (Family Plus management)
navigates directly, per that screen's own established rule.

After a purchase, closing the paywall is a plain `popBackStack()` — no result passing. Reveal is
Flow-driven: `QuestDetailViewModel`/`PackDetailsViewModel`/`SavedViewModel` each keep a live
collector on `EntitlementRepository.observeAccess()` for the screen's whole lifetime (the
ViewModel is paused, not destroyed, while the paywall is on top of it — same backstack-retention
reasoning as the filter store above), so by the time the user pops back, locked content is already
unlocked. **Purchasing never auto-starts a quest** — the reactive collector only ever updates
displayed state, never calls `StartQuest`.

If the paywall is dismissed without purchasing, the same pop-back returns to the same screen with
its state exactly as it was — nothing was mutated by opening the paywall.

## Entitlement expiration

If Family Plus expires while a screen observing `EntitlementRepository.observeAccess()` is open,
that screen's `locked` state flips back to `true` reactively, and `StartQuest`'s own re-check means
a *new* premium session cannot be created afterward. An **already-active** Quest Mode session is
never affected — `QuestMode`/`StartQuest`/`CompleteQuest` never re-check entitlements once a
session exists, so a session started while premium was active can finish offline, and its
completion/memory are never deleted or hidden by a later entitlement change. This is a deliberate
product rule: access gates *starting*, never a session or memory already in progress or completed.

## How to test Explore with fake premium states

Every ViewModel test in `feature/explore`, `feature/saved`, `feature/packdetails`, and
`feature/questdetail` builds its own fixture from `FakeQuestRepository` +
`FakeSavedQuestRepository` + `FakeEntitlementRepository(AccessSnapshot(...))` + the real
`QuestAccessPolicy` — never a mock of `QuestAccessPolicy` itself, since that policy's actual
comparison logic is exactly what's under test. To simulate:

- **A free family**: `FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), now))`.
- **An active Family Plus family**: `FakeEntitlementRepository(AccessSnapshot(FamilyAccess.lifetime(), setOf(EntitlementId("family_plus")), now))`.
- **An entitlement change while a screen is open**: call `entitlementRepository.setAccess(...)`
  after `onScreenStarted()`, then `advanceUntilIdle()` — every reactive collector described above
  picks it up without any explicit refresh call.

## How to test with RevenueCat Test Store

Not covered by this step — Step 11's own `docs/revenuecat-setup.md` documents Test Store
configuration and sandbox purchase flow; nothing about Explore/Pack Details/premium previews
changes that setup. The paywall screen itself (`FamilyPlusPaywallRoute`/`FamilyPlusPaywallViewModel`)
owns all real RevenueCat SDK interaction — Explore, Pack Details, Saved, and Quest Details never
call RevenueCat directly, only `EntitlementRepository`.

## Known limitations

- Explore's "All packs"/"Suggested quests" lists are plain `Column { forEach }`, not
  `LazyColumn` — acceptable at the current catalogue size (45 quests, 6 packs, a 12-item suggested
  cap) but would need revisiting if the catalogue grows substantially.
- An Explore catalogue that is genuinely empty (not merely search/filter-empty) has no dedicated
  empty state — purely theoretical today since the bundled catalogue always has content, but would
  need a real state if a future remote/dynamic catalogue could legitimately ship empty.
- No Compose UI (`androidDeviceTest`) tests exist yet for the Explore Filters, Saved, or Pack
  Details *screens* specifically (ViewModel behavior for all three is fully unit-tested); Explore's
  own home screen and Quest Detail do have UI tests. No device/emulator was available in this
  environment to execute any `androidDeviceTest` suite, so all UI-test claims in this document
  cover compilation, not execution.

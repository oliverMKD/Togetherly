# Product event taxonomy (Steps 14.3–14.4)

Steps 14.1 (provider-neutral architecture) and 14.2 (real PostHog SDK) shipped no feature
instrumentation — every `ProductAnalytics.capture`/`.screen` call site was either absent or
illustrative only. Step 14.3 defined the actual, deliberately small event vocabulary and wired it
into Togetherly's real feature ViewModels. **Step 14.4 added `premium_content_unlocked`, the
`source_screen`/`offering_identifier`/`available_package_types` paywall properties, reshaped the
purchase/restore result vocabulary into granular store-level outcomes, and defined the Shipaton
measurement funnels** (see [Core funnels](#core-funnels) below). See [telemetry.md](telemetry.md)
for the consent model, privacy validator, and provider contracts this taxonomy runs on top of,
[analytics-setup.md](analytics-setup.md) for the PostHog provider itself, and
[revenuecat-posthog-integration.md](revenuecat-posthog-integration.md) for the RevenueCat↔PostHog
identity link and dashboard-side revenue measurement this file's own purchase/restore events feed
into — this file only documents *what* is sent and *why*, never *how* the pipe works.

Every event below is a typed `AnalyticsEvent` subtype declared in
`core/telemetry/AnalyticsEvent.kt`, registered with its own property allowlist in
`core/telemetry/TelemetryEventSchema.kt`. An event or property not listed in this document does not
exist in the registry either — `TelemetryPrivacyValidator` rejects anything else outright, so this
document and the code can never silently drift apart without a test failing
(`TelemetryPrivacyValidatorTest`, plus a dedicated schema/property test per event where one exists).

## Never capture

Restated from the governing spec, and enforced by `TelemetryPrivacyValidator`'s allowlist plus its
forbidden-key/text heuristics as defense in depth — never, in any event's properties:

- Search query text
- Family name, parent name, child name, or any other onboarding profile answer
- Memory note text or reaction text
- Voice or photo content, or a media file reference/path — only a boolean "has this kind of media"
  indicator
- Notification contents
- Error messages or exception text (only a broad outcome enum, e.g. `PurchaseOutcomeResult.NETWORK_ERROR`)
- Screen coordinates, scroll position, or any per-tap/per-frame signal
- The exact reminder time or which specific days are selected — only `reminder_enabled` and a day
  *count*
- A completion id, memory id, or any other local database row id — only bundled catalogue
  identifiers (`quest_id`, `pack_id`) ever leave the device
- A family profile id, a raw purchase token, receipt, transaction ID, order ID, or the RevenueCat
  `CustomerInfo` object itself
- RevenueCat's own App User ID as a normal event property — it is never read into any event's
  `properties()` map anywhere in this codebase; the one place a PostHog identifier and RevenueCat
  meet is the dedicated `$posthogUserId` subscriber-attribute link described in
  [revenuecat-posthog-integration.md](revenuecat-posthog-integration.md), never a per-event property
- An advertising identifier of any kind — this app collects none, and adds no advertising
  attribution SDK

## Common properties

Allowed, predefined-vocabulary properties any event may draw from (never a family-specific free-text
value):

| Property | Meaning | Example values |
|---|---|---|
| `screen` | Implicit — carried by `ProductAnalytics.screen(AnalyticsScreen)` calls, not an event property | `today`, `explore`, `journey`, `family`, `onboarding`, `quest_mode`, `completion`, `reminder` |
| `source` | Where a quest was opened from | `today`, `explore`, `pack`, `saved`, `journey` |
| `quest_id` / `pack_id` | Bundled Togetherly catalogue identifier | `quest-042`, `pack-family-night` |
| `quest_category` | Broad content category | `talk`, `create`, `move`, `kindness`, `discover`, `silly`, `memories` |
| `duration_bucket` | Predefined length bucket | `five_minutes`, `ten_minutes`, `twenty_minutes`, `thirty_plus_minutes` |
| `energy_level` | Predefined energy level | `calm`, `moderate`, `active` |
| `access_required` / `access_filter` | Free vs. premium, as a label — never a price | `free`, `premium` (plus `all` for `access_filter`) |
| `access_state` | Common property (see `PostHogCommonProperties`), the family's *current* plan | `free`, `family_plus`, `unknown` |
| `is_saved` | New saved/unsaved state after a toggle | `true`, `false` |
| `has_note` / `has_photo` / `has_voice` | Whether a memory includes that kind of content | `true`, `false` |
| `paywall_context` | Why the paywall opened | `premium_reroll`, `premium_quest`, `premium_pack`, `family_plus_management` |
| `package_type` | Broad subscription package shape | `monthly`, `annual`, `lifetime` |
| `result` | Outcome enum for a purchase/restore attempt | see each event's own allowed values below |
| `reminder_enabled` / `selected_day_count` | Broad reminder preference shape | `true`/`false`, `0`–`7` |
| `location` | Predefined quest location | `indoor`, `outdoor`, `either` |
| `onboarding_step` | Destination onboarding step | `welcome`, `family_name`, `age_bands`, `interests`, `preferences`, `reminder`, `review` |
| `used_timer` / `used_phone_down` | Whether Quest Mode's timer/phone-down mode was actually used | `true`, `false` |
| `source_screen` | Which screen a paywall/purchase/restore event originated from | `paywall`, `family_plus_management` |
| `offering_identifier` | The RevenueCat dashboard offering identifier the shown packages came from | `default` |
| `available_package_types` | Every package type on offer at capture time, comma-joined and sorted — never a price | `"annual,monthly"` |

`available_package_types` is the one property whose value is a joined string rather than a single
token — `AnalyticsValue` has no list variant (see `core/telemetry/AnalyticsValue.kt`), and the set of
possible joined strings is still small and fully predictable (at most the 3 known package types),
never free text.

`quest_id`/`pack_id` are always bundled catalogue identifiers — never a completion id, a memory id,
or any other local database row id.

## Screens (`ProductAnalytics.screen`)

Property-free "viewed" moments, fired once per meaningful navigation entry via each ViewModel's own
`onScreenStarted()` guard (never repeated by recomposition, flow re-collection, or process
restoration). A screen entry that needs a property is a typed event instead (see below) — the
`screen()` call itself carries no properties.

| Screen | Fired from | Trigger |
|---|---|---|
| `today` | `TodayViewModel.onScreenStarted` | Today tab entered |
| `explore` | `ExploreViewModel.onScreenStarted` | Explore tab entered |
| `journey` | `JourneyViewModel.onScreenStarted` | Journey tab entered |
| `onboarding` | `OnboardingViewModel.onScreenStarted` | Onboarding flow entered (once, regardless of which internal step is first shown) |
| `quest_mode` | `QuestModeViewModel.onScreenStarted` | Quest Mode entered after a session starts |
| `completion` | `CompletionCelebrationViewModel.onScreenStarted` | The post-quest celebration screen is shown |
| `reminder` | `ReminderViewModel.onScreenStarted` | The reminder settings screen entered |

`family` is declared in `AnalyticsScreen` but not currently fired by any ViewModel — reserved for a
future Family settings landing screen if one becomes worth tracking; adding the call site is a
one-line change against an already-registered value.

Two further `AnalyticsScreen` values (`paywall`, `family_plus_management`) exist only as the
`source_screen` property value on paywall/purchase/restore events (see [Paywall /
purchases](#paywall--purchases)) — neither is ever passed to `ProductAnalytics.screen()` itself,
since `PaywallPresented`'s own typed properties (`paywall_context`, `offering_identifier`, …)
already fully represent that screen-entry moment; a bare property-free `.screen(PAYWALL)` call
alongside it would be redundant.

---

## Onboarding

### `onboarding_step_viewed`

- **Trigger**: The onboarding flow lands on a new step — the initial `WELCOME` step (via
  `onScreenStarted`) and every subsequent `ContinueClicked`/`BackClicked`/`SkipNameClicked`
  transition that actually changes the current step.
- **Purpose**: Understand where families slow down or drop off inside the multi-step flow.
- **Properties**: `onboarding_step`.
- **Allowed values**: `welcome`, `family_name`, `age_bands`, `interests`, `preferences`, `reminder`, `review`.
- **Forbidden**: The family name, selected age bands, interests, or any other answer given on that step.
- **Funnel**: Onboarding (step 1 of 2, repeated per step reached).

### `onboarding_completed`

- **Trigger**: `CreateFamilyProfile` succeeds — the first family profile is actually persisted, not
  merely when Create is tapped.
- **Purpose**: The onboarding funnel's conversion event.
- **Properties**: none.
- **Forbidden**: The family name, age bands, interests, preferences, or reminder choice.
- **Funnel**: Onboarding (final step). No `onboarding_abandoned`/exit event exists — see
  [Known gaps](#known-gaps).

---

## Today

### `daily_quest_revealed`

- **Trigger**: The family taps to reveal the mystery quest card (`TodayAction.RevealClicked`) and a
  quest was actually showing.
- **Purpose**: Measure engagement with the daily mystery-quest mechanic.
- **Properties**: `quest_category`, `duration_bucket`, `energy_level`, `access_required`.
- **Forbidden**: `quest_id` (deliberately omitted — this event measures the *mechanic*, not which
  specific quest, avoiding a redundant `quest_viewed`-shaped event for the same moment).
- **Funnel**: Today → Daily quest.

### `daily_quest_rerolled`

- **Trigger**: `RerollDailyQuest` succeeds and a new quest has actually been persisted as today's
  selection — never on the confirmation dialog opening.
- **Purpose**: Measure how often families use their free reroll allowance.
- **Properties**: `quest_category`, `duration_bucket`, `energy_level`, `access_required` (of the
  *new* quest).
- **Funnel**: Today → Daily quest → Reroll.

### `daily_quest_reroll_blocked`

- **Trigger**: A reroll attempt is blocked by the free-tier allowance — both when
  `TodayAction.RerollClicked` finds zero rerolls remaining (skips the confirmation dialog entirely)
  and when a confirmed reroll's own allowance check rejects it server-side. Both call sites fire the
  same event; see `TodayViewModel`'s own KDoc.
- **Purpose**: Size the premium-reroll paywall opportunity.
- **Properties**: none.
- **Funnel**: Today → Daily quest → Reroll → Premium allowance funnel.

---

## Quest detail / quest lifecycle

These four events are shared across every navigation path into Quest Detail — Today, Explore, a
pack, the Saved list, or (reserved) Journey — distinguished only by `source`. Reusing one event
family keeps the taxonomy small instead of duplicating `today_quest_viewed`/`explore_quest_viewed`/etc.

### `quest_viewed`

- **Trigger**: Quest Detail finishes loading a quest successfully (`QuestDetailViewModel.load`
  success branch) — never on navigation *intent* alone.
- **Purpose**: Core content-discovery funnel entry point; `source` attributes which surface drove it.
- **Properties**: `quest_id`, `source`, `quest_category`, `access_required`.
- **Allowed `source` values**: `today`, `explore`, `pack`, `saved`, `journey`.
- **Funnel**: Quest discovery (`quest_viewed` → `quest_started` → `quest_completed`/`quest_abandoned`).

### `quest_saved`

- **Trigger**: `SetQuestSaved`/`ToggleSavedQuestUseCase` succeeds — fired from Quest Detail, Explore's
  quick-save button, and Pack Details' quick-save button alike (all three reuse the same event).
- **Purpose**: Measure save/bookmark engagement independent of the quest-open funnel.
- **Properties**: `quest_id`, `is_saved` (the new state after the toggle).
- **Funnel**: Quest discovery (side branch, not gated on `quest_viewed`).

### `quest_started`

- **Trigger**: `StartQuest`/`ReplaceActiveQuestSession` succeeds — an active Quest Mode session has
  actually been created, never merely when Start is tapped (a locked-content or active-session-conflict
  outcome never reaches this event).
- **Purpose**: Quest discovery funnel's conversion step.
- **Properties**: `quest_id`, `source`, `quest_category`, `access_required`.
- **Funnel**: Quest discovery.

### `quest_completed`

- **Trigger**: `CompleteQuest` succeeds inside Quest Mode.
- **Purpose**: The core product outcome — a family actually finished a quest together.
- **Properties**: `quest_id`, `quest_category`, `duration_bucket`, `energy_level`, `access_required`,
  `used_timer`, `used_phone_down`.
- **`used_timer` semantics**: `true` only when the quest had a timer *and* completion happened after
  it finished (`QuestTimerUi.Finished` at the moment Complete is tapped) — never the exact elapsed
  seconds.
- **Funnel**: Quest discovery → Completion/Memory.

### `quest_abandoned`

- **Trigger**: `AbandonQuest` succeeds from Quest Mode's exit-confirmation flow.
- **Purpose**: The quest-discovery funnel's other terminal state, distinct from a silent app close.
- **Properties**: `quest_id`, `used_phone_down`.
- **Forbidden**: How far through the quest the family got, or how long the session ran.
- **Funnel**: Quest discovery (terminal, non-conversion branch).

---

## Completion and Journey

### `memory_saved`

- **Trigger**: `SaveCompletionMemory` succeeds — metadata and any staged media are actually
  committed, never on a keystroke or a photo/voice picker opening.
- **Purpose**: Measure how often quest completions turn into a saved memory, and roughly what kind.
- **Properties**: `has_note`, `has_photo`, `has_voice` — booleans only.
- **Forbidden**: The note text, the reaction choices, the photo, the voice recording, or any media
  file reference.
- **Funnel**: Completion/Memory (follows `quest_completed`).

### `memory_deleted`

- **Trigger**: `DeleteCompletion` succeeds from the Journey screen's delete-confirmation flow.
- **Purpose**: Understand memory-retention behavior (are memories being cleaned up, and how often).
- **Properties**: none.
- **Forbidden**: The completion id, or anything about what the deleted memory contained.
- **Funnel**: Journey.

### `memory_opened`

- **Trigger**: A voice memory's playback actually starts successfully
  (`JourneyViewModel.onPlayVoiceClicked`, non-error branch).
- **Purpose**: The closest real per-entry engagement signal Journey has — note and photo content is
  already fully visible inline in the list (no separate "open" gesture exists for those), so this
  event is scoped to voice playback only. See [Known gaps](#known-gaps).
- **Properties**: none.
- **Forbidden**: The completion id or media reference.
- **Funnel**: Journey.

---

## Explore

### `explore_searched`

- **Trigger**: Edge-triggered — fired the moment the debounced search stream transitions from
  inactive to active (a non-blank query settles), never once per keystroke and never again while the
  same search stays active. Clearing the search and typing a new query fires it again.
- **Purpose**: A boolean signal that search is used at all, without ever seeing what was searched for.
- **Properties**: none.
- **Forbidden**: The query text itself, in any form.
- **Funnel**: Explore → Search.

### `explore_filtered`

- **Trigger**: A filter change actually commits — both the quick category chip
  (`ExploreAction.CategorySelected`/`CategoryCleared`) and the full filter sheet's Apply button
  (`ExploreFiltersAction.ApplyClicked`) fire this, since both write through the same committed
  `ExploreFilterStore`.
- **Purpose**: Understand which filter dimensions families actually use.
- **Properties**: `quest_category`, `duration_bucket`, `energy_level`, `location`, `access_filter` —
  each omitted from the payload when unset, never sent as a null/empty placeholder.
- **Allowed `access_filter` values**: `all`, `free`, `premium`.
- **Forbidden**: Age band (deliberately excluded from this event even though it's a filter dimension
  in the UI — it's closer to a child-identity signal than the other broad categories).
- **Funnel**: Explore → Filter.

### `pack_viewed`

- **Trigger**: Pack Details finishes loading a pack successfully.
- **Purpose**: Content-discovery funnel entry point for bundled packs.
- **Properties**: `pack_id`, `access_required`.
- **Funnel**: Explore → Pack discovery.

### `premium_content_viewed`

- **Trigger**: Fired alongside `quest_viewed`/`pack_viewed` whenever the loaded quest or pack is
  locked (premium, not yet accessible) — from Quest Detail and Pack Details, whichever content type
  applies. Exactly one of `quest_id`/`pack_id` is present.
- **Purpose**: The paywall funnel's top-of-funnel signal — how often families encounter locked
  content before ever opening the paywall.
- **Properties**: exactly one of `quest_id` / `pack_id`.
- **Funnel**: Paywall (entry point, precedes `paywall_presented`).

### `premium_content_unlocked`

- **Trigger**: While Quest Detail or Pack Details is open and was showing locked content, the live
  entitlement collector both screens already run (`observeAccess()`) sees access flip from locked to
  unlocked — a purchase or restore completing (from this screen's own paywall, or from anywhere
  else in the app) while the family is still looking at the content that was locked. Never fired for
  content that was never locked in the first place.
- **Purpose**: The Family Plus funnel's conversion event — the moment previously-inaccessible
  content actually becomes usable, distinct from `purchase_result(success)` itself (see [Family Plus
  funnel](#family-plus-funnel) for why these are kept separate).
- **Properties**: exactly one of `quest_id` / `pack_id`, mirroring `premium_content_viewed`.
- **Funnel**: Family Plus (terminal, conversion).

`quest_saved` (reused) also fires from Explore's and Pack Details' own quick-save buttons — see
[Quest detail / quest lifecycle](#quest-detail--quest-lifecycle) above.

---

## Paywall / purchases

Every paywall-lifecycle event (`paywall_presented`, `paywall_dismissed`, `purchase_started`,
`purchase_result`) carries the same five paywall-context properties: `paywall_context`,
`source_screen`, `access_state` (a common property, always present — see [Common
properties](#common-properties)), `offering_identifier`, and `available_package_types`.
`offering_identifier`/`available_package_types` are omitted/empty only when packages genuinely
haven't finished loading yet at capture time (possible on `paywall_presented`, which fires
synchronously before the async package load resolves) — never guessed. `restore_started`/
`restore_result` carry `source_screen` only — a restore has no package selection or offering to
attach the other three to.

### `paywall_presented`

- **Trigger**: `FamilyPlusPaywallViewModel.onScreenStarted` — the paywall screen finishes its first
  meaningful entry.
- **Purpose**: Paywall funnel's entry point, attributed by why it was opened.
- **Properties**: `paywall_context`, `source_screen`, `offering_identifier`, `available_package_types`.
- **Allowed `paywall_context` values**: `premium_reroll`, `premium_quest`, `premium_pack`, `family_plus_management`.
- **Allowed `source_screen` value**: `paywall` (always, for this event).
- **Funnel**: Paywall.

### `paywall_dismissed`

- **Trigger**: `FamilyPlusPaywallAction.CloseClicked` — the explicit close/X action. A system back
  gesture that bypasses the ViewModel entirely is not captured — see [Known gaps](#known-gaps).
- **Purpose**: The paywall funnel's non-conversion terminal state.
- **Properties**: `paywall_context`, `source_screen`, `offering_identifier`, `available_package_types`.
- **Funnel**: Paywall (terminal, non-conversion branch).

### `purchase_started`

- **Trigger**: `FamilyPlusPaywallAction.PurchaseClicked`, once a package selection resolves to a
  known `package_type` (never fired if that resolution somehow fails — the purchase attempt itself
  is never blocked by this).
- **Purpose**: Purchase funnel's entry point.
- **Properties**: `paywall_context`, `source_screen`, `package_type`, `offering_identifier`, `available_package_types`.
- **Allowed `package_type` values**: `monthly`, `annual`, `lifetime`.
- **Forbidden**: Price, product ID, receipt, or purchase token.
- **Funnel**: Paywall → Purchase.

### `purchase_result`

- **Trigger**: `PurchaseFamilyPlus` resolves — every branch of `PurchaseResult`
  (`Success`/`Cancelled`/`Pending`/`Failure`) fires exactly one of these, never zero and never more
  than one per attempt.
- **Purpose**: Purchase funnel's conversion/drop-off event. **This is a product-intent signal only —
  never the authoritative revenue metric.** See
  [revenuecat-posthog-integration.md](revenuecat-posthog-integration.md) for why RevenueCat's own
  server-verified subscription lifecycle events are the source of truth for actual revenue, and how
  to avoid double-counting a purchase between the two.
- **Properties**: `paywall_context`, `source_screen`, `result`, `package_type` (omitted only if it
  couldn't be resolved before the outcome, e.g. an immediate failure), `offering_identifier`,
  `available_package_types`.
- **Allowed `result` values**: `success`, `cancelled`, `pending`, `already_owned`,
  `store_unavailable`, `product_unavailable`, `network_error`, `unknown_error`. `Failure`'s own
  `PurchaseError` is bucketed into one of the five granular failure values via
  `PurchaseError.toPurchaseOutcomeResult()` — `ConfigurationProblem`/`PurchaseNotAllowed` both fall
  back to `unknown_error`, since neither has its own dedicated bucket in this vocabulary.
- **Forbidden**: The underlying `PurchaseError`'s own message, a receipt, a purchase token, a
  transaction ID, or the RevenueCat `CustomerInfo` object.
- **Funnel**: Paywall → Purchase (terminal).

### `restore_started`

- **Trigger**: `RestoreClicked`, from either the paywall (`FamilyPlusPaywallViewModel`) or the
  Family Plus management screen (`FamilyPlusManagementViewModel`).
- **Purpose**: Restore funnel's entry point, attributed by which screen it started from.
- **Properties**: `source_screen`.
- **Allowed values**: `paywall`, `family_plus_management`.
- **Funnel**: Restore.

### `restore_result`

- **Trigger**: `RestoreFamilyPlus` resolves, from either surface above.
- **Purpose**: Restore funnel's conversion/drop-off event. `no_purchases` is deliberately distinct
  from `success` — a restore that completed without error but found nothing to restore is not the
  same product outcome as one that actually reinstated Family Plus. This is a product-intent signal
  only, same caveat as `purchase_result` above.
- **Properties**: `source_screen`, `result`.
- **Allowed `result` values**: `success`, `no_purchases`, `already_owned`, `store_unavailable`,
  `product_unavailable`, `network_error`, `unknown_error` — the same granular `PurchaseError`
  bucketing `purchase_result` uses (via `PurchaseError.toRestoreOutcomeResult()`), minus
  `cancelled`/`pending` (a restore has neither) plus `no_purchases` (a restore-specific outcome
  `purchase_result` has no equivalent for).
- **Funnel**: Restore (terminal).

---

## Family settings

### `reminder_preference_changed`

- **Trigger**: `UpdateReminderPreference` succeeds *and* the saved preference actually differs from
  what was loaded (`ReminderUiState.hasUnsavedChanges` at save time) — a Save with no real change
  never fires this.
- **Purpose**: Track meaningful reminder-preference engagement using broad categories only.
- **Properties**: `reminder_enabled`, `selected_day_count`.
- **Forbidden**: The exact reminder time, or which specific days were selected — only a count.
- **Funnel**: Family settings (standalone, not part of a multi-step funnel).

---

## Core funnels

These are PostHog-side funnel *definitions* built from events already documented above — no new
event exists solely for funnel purposes. Configure each as a PostHog Funnels insight using the exact
event sequence given; where a step name below differs from a literal event name, the mapping is
called out explicitly.

### Activation funnel

```text
onboarding (screen) → onboarding_completed → daily_quest_revealed → quest_started → quest_completed → memory_saved
```

The funnel's first step is the `onboarding` screen view, not a literal `onboarding_started` event —
`ProductAnalytics.screen(AnalyticsScreen.ONBOARDING)` already fires exactly once per onboarding
entry (see [Screens](#screens-productanalyticsscreen)), and PostHog's own `$screen` events are valid
funnel steps; adding a redundant custom event for the same moment would violate this taxonomy's own
"avoid redundant events" rule.

- **Primary activation milestone**: `quest_completed` — first quest completed. This is the funnel
  step every other activation metric should be measured against.
- **Stronger emotional-retention milestone**: `memory_saved` — first memory saved. Track this
  separately from the primary milestone; not every activated family saves a memory on their first
  quest, and that gap is itself a meaningful signal.

### Explore engagement funnel

```text
explore (screen) → pack_viewed or quest_viewed → quest_saved → quest_started → quest_completed
```

`pack_viewed`/`quest_viewed` is an "or" branch in PostHog's funnel UI (two parallel step-2
definitions merging back into the same step 3) — a family can enter this funnel through either a
pack or a direct quest view from Explore. Filter `quest_viewed`/`quest_started`/`quest_completed` by
`source = explore` when building this funnel, since those three events are shared across every
quest-open surface (see [Quest detail / quest lifecycle](#quest-detail--quest-lifecycle)).

### Family Plus funnel

```text
premium_content_viewed or daily_quest_reroll_blocked → paywall_presented → purchase_started → purchase_result (result = success) → premium_content_unlocked
```

`daily_quest_reroll_blocked` is this funnel's mapping for "premium reroll blocked" — there is no
separately-named `premium_reroll_blocked` event; the one `daily_quest_reroll_blocked` event already
covers both call sites that can block a reroll (see that event's own entry above). Filter
`purchase_result` to `result = success` as the funnel's third step. `premium_content_unlocked` is
the funnel's true conversion step, not `purchase_result(success)` itself — see that event's own
entry for why they're kept distinct (a restore, or a purchase completed from a *different* paywall
context, can also unlock content a family is currently looking at).

### Retention signals

Measure engagement recurrence using these four events as PostHog Trend/Retention insight inputs,
grouped by day:

- `daily_quest_revealed`
- `quest_completed`
- `journey_viewed` — mapped to the `journey` screen view (`ProductAnalytics.screen(AnalyticsScreen.JOURNEY)`), not a separate custom event; see [Screens](#screens-productanalyticsscreen).
- `reminder_opened` — mapped to the `reminder` screen view (`ProductAnalytics.screen(AnalyticsScreen.REMINDER)`), same reasoning.

**Retention itself is never calculated inside the app.** No code in this codebase computes a
retention rate, cohort, or streak from these events — that arithmetic belongs entirely to PostHog's
own Retention insight (Product analytics → Retention), configured with one of the four events above
as the "performed event" and a repeat of the same (or a different) event as the "returning event",
grouped by day/week as appropriate. This app only ever emits the raw timestamped events; PostHog
owns every retention calculation downstream of that.

## RevenueCat customer attributes

Three low-risk, predefined attributes are written to RevenueCat for paywall targeting —
`onboarding_completed`, `first_quest_completed`, `preferred_duration_bucket` — gated on the same
analytics consent as every event in this document. See
[revenuecat-posthog-integration.md](revenuecat-posthog-integration.md#revenuecat-customer-attributes)
for the full reviewed justification (required/predefined/non-sensitive/low-cardinality/consent/
targeting-need) behind each of the three, and for the separate RevenueCat↔PostHog anonymous identity
link this app maintains.

---

## Known gaps

Documented deliberately, matching this step's own "where reliably detectable" allowance rather than
fabricating a weak signal:

- **No `onboarding_abandoned`/exit event.** Nothing in `OnboardingViewModel` distinguishes "the
  family closed the app mid-flow" from "the family will resume later" — the flow is a single
  in-memory state machine with no persisted draft, so there is no reliable boundary to hook an exit
  event into without adding infrastructure purely for telemetry. `onboarding_step_viewed`'s own
  step-by-step trail is the best available signal for where a flow stalled.
- **No `paywall_dismissed` from a system back gesture** that bypasses `FamilyPlusPaywallAction.CloseClicked`
  entirely — only the explicit close action is instrumented.
- **`memory_opened` covers voice playback only**, not note or photo content, which is already fully
  visible inline in the Journey list with no separate "open" gesture to instrument.
- **The `saved` screen (the Saved-quests list) has no `AnalyticsScreen` entry and is not
  instrumented** — it wasn't named in this step's own coverage requirements, and adding a screen
  purely to track tab visits without a specific product question to answer would run against "do not
  track every tap."
- **No advertising attribution of any kind was added in Step 14.4**, by explicit instruction — no
  attribution SDK, no ad network click ID, no install-referrer capture, and none of RevenueCat's own
  attribution-related reserved attributes (`$mediaSource`, `$campaign`, `$adGroup`, `$ad`,
  `$keyword`, `$creative`, `$appleAdsCampaignId`, `$appleAdsAdGroupId`, `$appleAdsKeywordId`) are
  ever set by this codebase.
- **`offering_identifier`/`available_package_types` can be absent on `paywall_presented`** —
  captured synchronously in `onScreenStarted()`, before the async package load resolves. They are
  reliably present on `purchase_started`/`purchase_result`, since neither can fire until a package
  selection has already resolved.

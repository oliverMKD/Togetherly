# Today, Quest Detail, and Quest Mode

`com.togetherly.feature.today`, `com.togetherly.feature.questdetail`, and
`com.togetherly.feature.questmode` (all `shared/src/commonMain`) implement the daily-quest
experience: Today's Mystery/Reveal card, contextual filters, the free reroll, quest detail, and the
Start Quest transition into a placeholder Quest Mode.

## Today

### UI-state model

`TodayUiState` is one immutable snapshot: `greeting`/`familyName`, `content: TodayContentState`
(Loading / Mystery / Revealed / Error — a single sealed type, never separate Booleans), `filters:
TodayFilterState` (draft), `filtersVisible`, `rerollsRemaining`, `rerollConfirmationVisible`,
`isApplyingFilters`/`isRerolling` (duplicate-action guards), and `transientError`. It holds nothing
platform-shaped — no `NavController`, `Context`, `SnackbarHostState`, domain repository, or mutable
collection.

`QuestCardUi` is what a Composable actually renders — never `FamilyQuest`. `title`/`summary` are
plain strings (already-resolved bundled content); every other label (`durationLabel`,
`locationLabel`, `preparationLabel`, `energyLabel`, `materialSummary`) is a `UiText.Resource`, and
`category` is `QuestCategoryUi`, a UI-only enum mirroring `QuestCategory` — category color/label
resolution stays entirely in the UI layer (`QuestCategoryUiColor.kt`, `QuestCardUiMapper.kt`).

### Reveal-state behavior

Reveal is presentation-only, never persisted: `TodayContentState.Mystery` vs. `Revealed` lives only
in `TodayUiState.content`. A quest starts `Mystery` on every fresh `ScreenStarted`; `RevealClicked`
flips it to `Revealed`, and that flip survives configuration change (ordinary `ViewModel` state) but
resets to `Mystery` on process death (a fresh process re-resolves the same persisted `DailyQuest`).
A successful reroll or filter-apply also returns the *new* quest to `Mystery` — reveal never
carries over to a replacement quest. None of this is completion or session state; those remain
Quest Detail's own explicit Start flow.

### Actions and events

`TodayAction` covers screen lifecycle, reveal, filters (open/dismiss/per-field-changed/clear/apply),
reroll (click/confirm/cancel), save, start, and transient-error dismissal — one `onAction` entry
point, no direct `ViewModel` method calls from a screen. `TodayEvent` is one-off effects only:
`OpenQuestDetail(questId)` and `RerollLimitReached`. There is deliberately no `StartQuest` event —
Today's primary action opens quest detail, and quest detail owns Start (see below).

### Filter draft/apply behavior

Filter-chip changes (`DurationFilterChanged`, etc.) only ever mutate `TodayUiState.filters` —
never the persisted daily quest. `FiltersClicked` snapshots the current filters as a baseline;
`FiltersDismissed` restores that baseline, discarding whatever changed while the sheet was open.
Only `ApplyFiltersClicked` calls `SelectDailyQuestForContext`, which itself is a no-op (returns the
current quest, no policy call, no write) if the applied context is unchanged from what's already
persisted. `ClearFiltersClicked` resets every group to `null`. The filter sheet is a shared Material3
`ModalBottomSheet` (stable cross-platform in this project's Compose Multiplatform version) — not a
separate full-screen destination.

### Free reroll rule

`RerollClicked` never calls the domain layer directly — it opens a confirmation dialog
(`rerollConfirmationVisible`), and only `RerollConfirmed` actually calls `RerollDailyQuest`. When
`rerollsRemaining == 0`, there's nothing to confirm, so `RerollClicked` emits `TodayEvent.RerollLimitReached`
immediately instead. `RerollAllowance`/`RerollAllowancePolicy` (`domain.daily`) give one free reroll
per local calendar day (`DefaultRerollAllowancePolicy`); `maximum = null` means unlimited
(Family Plus). **Premium is deferred**: every production call site hardcodes `hasFamilyPlus = false`
— there is no fake `EntitlementRepository` and no paywall anywhere in this codebase yet.

### ViewModel dependencies

`TodayViewModel` injects `GetOrSelectDailyQuest`, `SelectDailyQuestForContext`, `RerollDailyQuest`,
`SetQuestSaved`, `SavedQuestRepository` (to know whether the *currently resolved* quest is saved —
none of the daily-selection use cases return that), `FamilyRepository`, `AppClock`, and
`AppTimeZoneProvider` (the one seam for `TimeZone.currentSystemDefault()`, so tests can supply a
fixed zone). `ScreenStarted` is idempotent via a private flag — recomposition or a re-entered Route
never re-triggers the family observer or the initial load a second time.

## Quest Detail and Start Quest

`RootDestination.QuestDetail(questId)` is a root-level destination (pushed above `MainShell`'s tab
bar via `TodayEvent.OpenQuestDetail` → `MainShell(onOpenQuestDetail)` → `TogetherlyNavHost`), never
nested inside a tab. `QuestDetailUiState` is Loading / `Content(quest, isSaved, isStarting,
startError, activeSessionConflict)` / Error. The screen shows full ordered instructions, materials,
hints, and the safety note — everything Today's own card deliberately omits — plus a save toggle and
a sticky Start button.

**Start persistence sequence**: `StartClicked` calls the existing `StartQuest` use case (already
atomic — check-and-write inside one `QuestSessionTransaction`). On success, `QuestDetailEvent.NavigatedToQuestMode(completionId)`
fires only after the session has actually been persisted — never before. On
`ValidationError.ACTIVE_SESSION_CONFLICT`, the screen shows a confirmation instead of silently
replacing anything.

**Active-session conflict**: confirming calls a new, separate use case,
`ReplaceActiveQuestSession`, backed by a new `QuestSessionTransaction.replaceActiveSession` method
(atomic, unconditional overwrite) — never a hidden "force" branch inside `StartQuest` itself (see
that use case's own KDoc). Cancelling leaves the existing session completely untouched. Note: the
task's third option, "Continue previous quest," is not implemented — `QuestDetailViewModel` doesn't
currently learn the *previous* session's own identity when a conflict is detected, only that one
exists, so "Cancel" plays both roles (dismiss the dialog, keep the existing session). A future pass
can split this once the conflict state carries the previous session's own completion ID.

## Quest Mode (placeholder)

`RootDestination.QuestMode(completionId)` is a deliberately minimal full-screen placeholder: it
loads the active session, verifies its `completionId` matches the route argument, loads the
corresponding quest, and shows its title plus neutral "Your quest is ready." copy. It never mutates
the active session in any way — closing it (`onClose` → `popBackStack()`) leaves whatever
`CompletionRepository` already has untouched; a session is only ever cleared by the (not yet built)
`CompleteQuest` flow. No timer, phone-down mode, or completion UI exists yet.

## Tests and previews

Today: `TodayViewModelTest` (20+ cases — load, mystery/reveal, retry, filter draft/apply/dismiss/clear,
reroll confirm/cancel/success/failure/limit, save, start event, duplicate-ScreenStarted, no-raw-error
exposure), `TodayErrorMapperTest`, `TodayScreenTest` (instrumented — hidden-title-never-in-semantics,
reveal/save/start/retry actions, filter sheet and reroll dialog interactions, reduced motion, large
text, no duplicate bottom nav). Quest Detail: `QuestDetailViewModelTest`, `QuestDetailUiMapperTest`,
`QuestDetailScreenTest` (instrumented). Quest Mode: `QuestModeViewModelTest`. Previews exist for every
major state (loading/mystery/revealed/saved/materials/error/filters/reroll/light/dark/large font/
reduced motion) across all three features, built on shared fixture functions
(`com.togetherly.feature.today.preview`) rather than duplicating sample data per preview.

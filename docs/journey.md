# Journey (Stars, Constellation, Timeline)

Journey (`domain.journey`, `feature.journey`) turns completed quests into a derived "constellation"
of stars plus a private chronological memory timeline (Steps 10.5–10.6). This document covers why
there is no `journey_star` database table, how star position is derived deterministically, the
constellation's collision handling, milestones, and the timeline screen's own behavior. For the
memory content a Journey entry displays, see [memory-flow.md](memory-flow.md); for the private
photo/voice storage it reads from, see [private-media.md](private-media.md).

## No `journey_star` table — a derived read model

Every completed quest produces exactly one `JourneyStar`, whether or not it has a note, reactions,
a photo, or a voice memory — enrichment never gates existence. Stars are computed fresh from
`JourneyEntry` (a `QuestCompletion` joined to its resolved `FamilyQuest`, Step 10.5's own contract
via `JourneyRepository`) rather than stored:

- Star identity **is** completion identity — a `journey_star` table would just be a driftable
  second copy of that same identity.
- Deleting a completion (`DeleteCompletion`, Step 10.7 fix — see [private-media.md](private-media.md))
  naturally removes its derived star with no separate cleanup step.
- Journey can always be rebuilt deterministically from `CompletionRepository`/`QuestRepository`
  alone; there is nothing to migrate or repair if a stored star table ever drifted from the
  completions it was supposed to mirror.

## Deterministic star placement

`JourneyStarPolicy.create(entry)` derives a star's `StarPosition`/`StarVisualVariant` from the
completion's own ID via `stableHash` (`domain.journey.StableHash.kt`) — a from-scratch 32-bit
FNV-1a hash over UTF-16 code units, deliberately **not** `String.hashCode()`. `String.hashCode()`'s
algorithm is only specified for the JVM; Kotlin/Native and Kotlin/JS are free to implement it
differently, which would silently put the same family's same completion at a different star
position on Android vs. iOS. Every derived value — x, y, and the small/medium/large size variant —
comes only from this hash, never from `kotlin.random.Random` or any platform API, so the same
completion ID always produces the same star, on any platform, on any run.

Positions stay within a safe margin (`0.08f..0.92f`, not the full `0f..1f` a bare `StarPosition`
allows) so stars never render flush against a constellation's edge.

This determinism is verified cross-platform, not just assumed: `JourneyStarPolicyTest` includes a
golden-fixture test asserting `stableHash`'s exact output for a fixed seed against a value computed
independently (outside Kotlin, from the documented FNV-1a algorithm), and that same test class runs
— and passes — on both `:shared:testAndroidHostTest` and `:shared:iosSimulatorArm64Test`.

## Constellation arrangement and collisions

`JourneyConstellationPolicy.arrange(entries)` limits the decorative overview to the most recent
`MAX_CONSTELLATION_STARS` (40) completions — a family with a long history still gets a readable
sky, not one star per completion ever made. The full timeline is unaffected by this cap; it reads
`JourneyEntry` directly.

Star identity and position never depend on the order `entries` is passed in: recency selection
breaks `completedAt` ties by completion ID, and collision resolution (`resolveCollisions`) always
processes candidates in completion-ID order internally, regardless of the caller's own list order
— only the *returned* list is newest-first, for convenience. Collision avoidance is intentionally
simple: a handful of deterministic nudges (again derived from `stableHash`, never randomness) away
from anything already placed, capped at a fixed attempt count — not a physics simulation.

## Milestones

`JourneyMilestone` (`FIRST_STAR`/`THREE_STARS`/`SEVEN_STARS`/`FOURTEEN_STARS`/`THIRTY_STARS`, at
1/3/7/14/30 completions) is a typed enum with no user-facing copy on it — `achievedJourneyMilestones(count)`
returns every milestone reached so far, and a UI layer picks which one to show gentle copy for
(`JourneyMilestone.copy()` mapper in `feature.journey.mapper`). There is no competitive rank and no
streak — a missed day never removes a previously-earned milestone, since milestones key off total
completion count, not a consecutive-day counter (streaks don't exist in this codebase yet at all,
see `JourneySummary`'s own KDoc).

## Journey timeline screen (Step 10.6)

`feature.journey` (`model`/`presentation`/`mapper`/`ui`) is the first functional Journey
destination — one of `MainShell`'s four bottom-nav tabs.

- **Constellation header**: the derived stars above, plus total completion count and the latest
  achieved milestone's copy. Stars use `QuestCategoryUi.color()` where a category resolved, a
  neutral color otherwise — color is never the *only* signal (each star's category is also
  reflected in its timeline entry's own text label). The whole starfield carries one combined
  `contentDescription` (`Modifier.semantics(mergeDescendants = true)`) rather than one per star —
  no individual star is an accessibility control. Twinkling is a per-star alpha animation, skipped
  entirely (static, full-alpha) when `MaterialTheme.togetherlyReduceMotion` is set.
- **Timeline**: newest-first, one card per entry (date/time, quest title or
  `journey_missing_quest_title` — "Family adventure" — when the quest can't be resolved, category,
  reactions, note, photo thumbnail, voice playback). A missing quest never hides the completion and
  never invents replacement content for it.
- **Photo**: loaded via `PrivateMediaStorage.openPhotoThumbnail` (Step 10.7 addition — see
  [private-media.md](private-media.md)), never the full-resolution photo; a decode failure shows a
  placeholder without hiding the rest of that entry's memory.
- **Voice**: `VoicePlaybackController` is injected directly into `JourneyViewModel` (not
  Route-bound, the same pattern `CompletionMemoryViewModel` uses for its own playback) — one clip
  plays at a time by the controller's own contract (starting a new `play()` call always stops
  whatever was playing first). `JourneyUiState.Content.playingVoiceId` is non-null only while a
  clip is actively *playing*, not paused — the state has room for exactly one such flag.
  `JourneyRoute`'s `DisposableEffect` stops playback the instant the Route leaves composition
  (reusing the ordinary `StopVoiceClicked` action, not a `ViewModel.onCleared()` override — see
  `JourneyViewModel`'s own KDoc for why `onCleared()` isn't a safe place to launch a suspend stop
  call, since `viewModelScope` is already cancelled by the time it runs).
- **Fetch/retry**: `JourneyViewModel.load()` uses `JourneyRepository.getJourney()` (one-shot), not
  `observeJourney()` — matching `TodayViewModel`'s own fetch-then-explicit-retry convention, so
  `RetryClicked` is a real, repeatable re-fetch. The trade-off: a memory saved elsewhere while this
  tab stays mounted (tabs persist across switches within `MainShell`) won't appear until the tab is
  next re-entered; nothing in this step's spec asked for live cross-screen reactivity.
- **Performance**: a `LazyColumn` (the constellation header is its first item), stable item keys
  from completion ID, no full-resolution decode in the row itself, no per-recomposition
  recalculation of star positions (they're computed once by the `ViewModel`, carried as already-resolved
  state).

Editing, sharing, and a memory detail view are explicitly out of scope for this step.

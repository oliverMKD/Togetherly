# Quest Mode

Quest Mode (`com.togetherly.feature.questmode`, backed by `com.togetherly.domain.questmode` and
`com.togetherly.domain.completion`) is the focused, full-screen experience a family enters after
starting a quest from Quest Detail. This document covers the timer deadline decision, the
deliberate no-pause MVP scope, what "backgrounding" and process death actually mean for the timer,
phone-down behavior, keep-screen-on rules, exit/abandon behavior, the completion transaction,
process-death recovery, and where a future notification integration would attach. It closes with
the Step 9.7 audit results.

## Timer deadline decision

The timer is never a countdown that ticks a stored value down. It is a wall-clock deadline derived
once from persisted data:

```text
timerEndAt = activeSession.startedAt + quest.timer.duration
```

[`QuestTimerPolicy`](../shared/src/commonMain/kotlin/com/togetherly/domain/questmode/QuestTimerPolicy.kt)
is pure and synchronous — it never reads a clock itself; a caller always supplies `now`. Given
`session`, `questTimer` and `now`, it resolves one of three states
([`QuestTimerState`](../shared/src/commonMain/kotlin/com/togetherly/domain/questmode/QuestTimerState.kt)):
`NotRequired` (no timer configured), `Running` (`remaining`/`progress` derived fresh from `now`), or
`Finished`. A `now` earlier than `startedAt` (clock skew, a resumed process reading a slightly stale
clock) is treated as "no time has elapsed yet" rather than producing a negative elapsed value.

[`DefaultQuestCountdownEngine`](../shared/src/commonMain/kotlin/com/togetherly/domain/questmode/DefaultQuestCountdownEngine.kt)
wraps this in a cold `Flow` that re-resolves the *entire* state from `AppClock.now()` on every tick
— never `remaining -= 1.second` — so a delayed tick, a backgrounded app, or a process recreated
mid-countdown never accumulates drift: whatever the real elapsed wall-clock time turns out to be,
the next emission reflects it exactly. The `Flow` completes after its one `Finished` emission.

This is why:

- The timer continues logically while the app is backgrounded, with no background service.
- The timer continues after process death, with no periodic database writes to keep it "warm."
- Reopening the app recalculates remaining time from `AppClock`, not from any in-memory state.
- The timer may finish while the app is closed; reopening shows the finished state immediately.
- Timezone changes never alter the deadline — the entire calculation is `Instant`-based
  (`kotlin.time.Instant`); no `LocalDate`/`TimeZone` value is read anywhere on this path.

## No-pause MVP decision

Quest Mode does not support pausing the timer in this version, and no presentation-only fake pause
exists (freezing the *displayed* time without freezing the real deadline would silently lie about
how much time is actually left). A real pause feature needs persisted pause state — at minimum a
`pausedAt`/accumulated-pause-duration column on `active_quest_session` — which is a schema
migration, not a presentation change. This is deliberately deferred rather than built ahead of a
concrete need.

## Background limitation

There is no background service, wake lock, or platform timer (`Handler`, `NSTimer`) anywhere in
this feature. While the app process stays alive in the background, the countdown's `viewModelScope`
coroutine keeps running and ticking normally — Android does not suspend a live process's
coroutines merely because it isn't in the foreground. If the OS kills the process (or the user
force-stops the app) while a timer is running, nothing continues ticking; the next section covers
exactly what happens when the app is reopened.

## Phone-down behavior

`QuestModeAction.PhoneDownClicked`/`ExitPhoneDownClicked` toggle `QuestModeUiState.Content.phoneDown`
— presentation-only state, never persisted. While phone-down is active:

- [`QuestModePhoneDownScreen`](../shared/src/commonMain/kotlin/com/togetherly/feature/questmode/presentation/QuestModePhoneDownScreen.kt)
  renders a near-black, minimal surface (still-running timer digits, a short title/body, tap
  anywhere to return) instead of the full instructions screen.
- Keep-screen-on is **disabled** while phone-down (see below) — the whole point is to encourage
  putting the phone face-down, not to keep the screen lit.
- The countdown keeps running unaffected; phone-down only changes what's rendered, never the timer
  math.

## Keep-screen-on rules

[`shouldKeepScreenOn`](../shared/src/commonMain/kotlin/com/togetherly/feature/questmode/presentation/QuestModeRoute.kt)
is enabled only when **all** of: the quest's content requests it
(`QuestModeContentUi.keepScreenOnRequested`), the timer is actually `Running` (never for an
untimed quest or a `Finished` one), and phone-down is off. `KeepScreenOnEffect` is resolved and
applied at the `QuestModeRoute` boundary only — never inside the `ViewModel`, which stays
platform-independent.

- **Android**: sets/clears `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` on the hosting
  `Activity`'s window, found by walking the `ContextWrapper` chain (never an unsafe `as Activity`
  cast — the composition's `Context` is not guaranteed to already *be* one). Restores the flag's
  prior state exactly on dispose, never unconditionally clearing a flag this effect didn't itself
  set. No wake lock, no foreground service, orientation is never touched.
- **iOS**: sets/restores `UIApplication.sharedApplication.idleTimerDisabled`, capturing and
  restoring the previous value on dispose the same way.

## Exit and abandon behavior

The top bar's close ("✕") action (`QuestModeAction.BackClicked`) opens `ExitConfirmationDialog`
with three outcomes — never abandons directly:

- **Keep for later** (`KeepInProgressClicked`) — navigates back; the active session is untouched.
- **Continue quest** (`DialogDismissed`) — dismisses the dialog; nothing changes.
- **Abandon quest** (`AbandonClicked`) — opens a second, explicitly destructive confirmation
  (`AbandonConfirmationDialog`) rather than abandoning on the first tap. Only its own confirm action
  (`AbandonConfirmed`) calls `AbandonQuest`, which clears the active session and nothing else — it
  never creates a completion and never touches the daily quest selection.

The system back gesture/button is **not** intercepted by `BackHandler` anywhere in Quest Mode — it
pops the navigation back stack directly, the same net effect as "Keep for later" (the active
session is a repository row, untouched by navigation alone), just without showing the confirmation
dialog. This is a known, audited asymmetry (Step 9.7): the two paths are equally safe for data, but
inconsistent for UX. Making them consistent would mean adding a `BackHandler` to intercept the
system gesture — a new interaction, not a bug fix, so it is intentionally left as-is pending a
product decision.

Closing Quest Mode through any of these paths never silently deletes data: only `AbandonConfirmed`
clears the session, and only after its own explicit second confirmation.

## Completion transaction

`QuestModeAction.CompleteClicked` calls `CompleteQuest`, which reads the active session, builds a
`QuestCompletion`, then commits through `QuestSessionTransaction.completeActiveSession` — saving
the completion and clearing the active session as one atomic step (see
[persistence.md](persistence.md#transactional-boundaries)). A duplicate `CompleteClicked` while a
request is already in flight is a no-op (`isCompletingGuard`/`content.isCompleting`); on success,
`QuestModeEvent.NavigateToCompletion(completionId)` fires exactly once, and the caller navigates to
`RootDestination.CompletionCelebration`, popping `QuestDetail`/`QuestMode` off the back stack (see
[navigation.md](navigation.md)) so Back from the celebration can never reopen the now-completed
session. A write failure leaves the active session exactly as it was, with a retryable in-place
error — no navigation happens on failure. This holds regardless of the timer's state: `CompleteQuest`
never reads or gates on `QuestTimerState` — manual completion is equally valid before the deadline,
after it, or with no timer at all. A `Finished` timer never auto-completes the quest by itself.

## Process-death recovery

Recovery after process death relies entirely on the timer-deadline decision above plus the
persisted `active_quest_session` row — there is no separate "resume" code path to get wrong. A
freshly constructed `QuestModeViewModel` (a real process restart, or the equivalent in a test —
see `QuestModeViewModelTest`'s `processDeath*` cases) calls `LoadQuestMode(completionId)` on
`ScreenStarted`, which re-reads the active session from `CompletionRepository`, resolves
`QuestTimerPolicy` against the *current* clock, and renders `Running` or `Finished` accordingly.
Completing from a recovered instance is exactly the same `CompleteQuest` call as any other — it is
not aware, and does not need to be aware, that the process was ever recreated.

There is currently **no automatic redirect** on app relaunch straight into an active Quest Mode
session — Bootstrap only ever decides `Onboarding` vs. `Main` (see
[navigation.md](navigation.md#bootstrap-the-family-profile-is-the-source-of-truth)). A user who
force-quit mid-quest returns to Today and must navigate back into the same quest (Quest Detail's
existing session-conflict handling) to resume; there is also no "Continue previous quest" option
there yet, only Replace/Cancel (a known limitation carried over from Step 8.6). Neither gap loses
data — the session and timer both recover correctly the moment Quest Mode is reached — but reaching
it again today requires a manual path, not an automatic one. Documented here as a candidate for
future polish, not implemented in Step 9.7 (audit steps verify and fix bugs; they do not add new
navigation behavior).

## Future notification integration

No notification is scheduled, requested, or promised anywhere in this feature. A future "timer
finished" notification (for when the app is fully backgrounded/killed) would need real platform
scheduling — `AlarmManager`/`WorkManager` on Android, `UNUserNotificationCenter` on iOS — which
does not exist yet and is out of scope until a concrete need is prioritized.

## Step 9.7 audit results

**Lifecycle**: configuration change relies on the `ViewModel` surviving via `ViewModelStore`
(`hasStarted`/`countdownJob`/`timerFinishedEmitted` guards prevent any duplicate collector or
duplicate `Finished` event across recomposition); background/resume relies on the recalculation
model above; process death is covered by three new `QuestModeViewModelTest` cases that construct a
second, independent `Fixture` sharing the same repositories with a clock advanced past what the
first (destroyed) instance ever saw, verifying `Running` recovery mid-timer, `Finished` recovery
past the deadline, and no duplicate completion afterward; clock-forward/backward and progress
clamping are covered by `QuestTimerPolicyTest`. Timezone independence is structural (no `TimeZone`
import anywhere on the timer path), not test-covered directly.

**Exit/completion**: all itemized scenarios have direct `QuestModeViewModelTest` coverage,
including the two new gaps found during this audit — `DialogDismissed` leaving the session
untouched, and `CloseClicked` doing the same — plus explicit before-/after-deadline manual
completion tests and the no-navigation-on-failure case. "Starting another quest requires conflict
handling" is Quest Detail's `ReplaceActiveQuestSession` (Step 8.6), not retested here. "App restart
returns to an active session" is the documented gap above.

**Accessibility**: found and fixed one real gap — the `Finished` timer text had no `liveRegion`, so
TalkBack would never announce it unless focus already happened to be there; it now carries
`LiveRegionMode.Polite` (and only there — the `Running` text deliberately still has none, to avoid
a per-second announcement). Also localized two previously-hardcoded English literals
(`"Expanded"`/`"Collapsed"` hint state descriptions) and pointed the error screen's close button at
its own dedicated string resource instead of reusing the icon's content-description string. Every
other checklist item (reading order, phone-down screen-reader exit, large-font reachability via
`TogetherlyScreen`'s independent scroll region, dialog focus via Material3's built-in `AlertDialog`
and the platform `Dialog` window boundary, destructive-action labeling/color, touch target
minimums) was verified structurally with no change needed.

**Platform**: Android — no unsafe cast, no wake lock, no foreground service, orientation untouched,
haptics API-level-gated and `runCatching`-wrapped, confirmed by reading `KeepScreenOnEffect.android.kt`
and `QuestFeedbackController.android.kt` directly. iOS — idle timer restore verified,
`runCatching`-wrapped haptics verified; found and fixed one gap — `QuestModePhoneDownScreen` had no
safe-area handling, so its centered content could in principle land under a notch/home indicator on
some devices; its content column now applies `WindowInsets.safeDrawing` while the full-bleed black
background still extends edge-to-edge. VoiceOver itself was not manually exercised on a simulator
(no macOS UI available in this environment) — it relies on the same shared semantics tree already
audited for Android/TalkBack, which Compose Multiplatform maps to `UIAccessibility`.

**Privacy**: no logging, analytics, or telemetry call of any kind exists anywhere in
`feature.questmode`, `domain.questmode`, `domain.completion`, or `feature.completion` — confirmed
by exhaustive grep, not just spot-checking known call sites.

**Architecture**: every itemized invariant (pure timer policy, injected-clock countdown engine,
recalculation instead of decrementing, platform-API-free `ViewModel`, feedback resolved only at the
route boundary, no repository call from a Composable, active session as sole authority, completion
ID as the only nav argument, transactional completion, no pause state, no notification promise, no
background service, no UI import in `domain`) was verified by direct inspection of every relevant
file's imports — none required a change.

**Documentation**: this file is new; `docs/navigation.md`, `docs/persistence.md`, and
`docs/architecture.md` were updated alongside it.

**Validation**: `./gradlew clean`, `./gradlew allTests`, `./gradlew build`,
`./gradlew :androidApp:assembleDebug` (this project has no `composeApp` module — see
[architecture.md](architecture.md) for the actual module names). iOS was compiled
(`compileKotlinIosSimulatorArm64`) but not run on a simulator/device — no macOS UI available in this
environment; manual Android testing was likewise not performed interactively in this environment.

# Togetherly lifecycle and state restoration

This pass hardens the app around backgrounding, configuration changes, process recreation, and the
quest/completion/purchase/deletion flows that can be interrupted mid-flight.

## Core restoration rules

- Quest Mode always reloads from the persisted active session, not from an in-memory countdown.
- Timer state is derived from persisted timestamps (`startedAt + duration`) through
  `QuestTimerPolicy`.
- A completed active session does not complete twice.
- A cancelled or abandoned session does not become completed later.
- Phone-down is treated as transient UI state and restores safely to a non-risky default.
- Terminal Quest Mode events are guarded at the route boundary so they are not replayed after a
  restored process re-runs the same screen logic.

## Verified behavior

- Active sessions survive valid process recreation.
- Timer progress resumes from the persisted start time after process death.
- Expired timers reopen as finished on recovery.
- Quest completion remains idempotent when the same restored route is re-entered.
- Navigation never trusts a stale quest ID over the persisted active session.
- Deletion flows keep their own destructive state isolated so they cannot resume midway through
  unrelated navigation.
- RevenueCat access continues to reconcile from customer-info updates while the paywall is open.

## Automated checks

- `shared/src/commonTest/kotlin/com/togetherly/feature/questmode/presentation/QuestModeViewModelTest.kt`
- `shared/src/commonTest/kotlin/com/togetherly/domain/questmode/DefaultQuestCountdownEngineTest.kt`
- `shared/src/androidDeviceTest/kotlin/com/togetherly/feature/questmode/presentation/QuestModeRouteEffectsTest.kt`
- `shared/src/iosTest/kotlin/com/togetherly/core/notification/IosReminderSchedulerTest.kt`
  - Covers the iOS notification adapter boundary with a fake center; it does not verify real OS
    delivery.

## Manual verification checklist

| Platform | Precondition | Steps | Expected result | Status |
| --- | --- | --- | --- | --- |
| Android | Quest Mode open with an active session and timed quest | Background the app, return to foreground, then rotate the device | The same active session remains visible and the timer resumes from persisted timestamps | Automated pass |
| Android | Quest Mode open with an active session and timed quest | Enable Developer options, turn on “Don’t keep activities” or kill the process with adb, then reopen the app | Quest Mode restores from the persisted active session and does not restart the timer from zero | Pending device verification |
| Android | Quest Mode open from the task switcher | Leave the app, then restore it from recents | The task returns to the correct screen stack without losing the active quest context | Pending device verification |
| Android | RevenueCat purchase flow open and waiting for completion | Terminate the app while the purchase sheet or customer-info reconciliation is in progress, then reopen | Purchase state is reconciled from RevenueCat customer information; an already authorized session stays authorized | Pending device verification |
| Android | Photo capture flow started from a memory screen | Terminate the app during capture, then reopen and inspect the memory entry | The app handles the interrupted capture without crashing; no duplicate or half-written memory appears | Pending device verification |
| Android | Voice capture flow started from a memory screen | Terminate the app during capture, then reopen and inspect the memory entry | The app handles the interrupted capture without crashing; no duplicate or half-written memory appears | Pending device verification |
| Android | Local-data deletion flow started from settings | Start deletion, terminate the app mid-flow, then reopen | Deletion does not resume halfway through navigation and does not leave the app in an inconsistent destructive state | Pending device verification |
| iOS | Quest Mode open with an active session and timed quest | Background and foreground the app, then simulate app termination and relaunch | The active session restores from persisted state and does not complete twice | Pending device verification |

## Notes

- The lifecycle policy is intentionally conservative: the app restores authoritative persisted state
  and suppresses duplicate terminal events, but it does not try to preserve every transient UI
  affordance across process death.
- The Android instrumentation route-effects test now uses
  `androidx.compose.ui.test.v2.runComposeUiTest` with a `SaveableStateHolder` wrapper so the
  recreation path matches the app’s navigation model and the deprecation warning is resolved
  without redesigning Quest Mode.
- iOS reminder scheduling is unit-tested through an adapter boundary; actual notification delivery
  still needs a real device or simulator with notification permission and a live clock.

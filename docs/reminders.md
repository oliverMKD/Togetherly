# Local daily reminders (Step 13.4)

Togetherly has no backend — reminders are scheduled entirely on-device via each platform's own
local notification API. There is no cross-platform notification library dependency; both actuals
call the system frameworks directly (`AlarmManager`/`NotificationManager` on Android,
`UNUserNotificationCenter` on iOS).

## Architecture

- `ReminderPreference` (`domain/family/ReminderPreference.kt`, shipped in Step 13.1) is the single
  source of truth for a family's reminder intent — `enabledDays: Set<DayOfWeek>` +
  `localTime: LocalTime`, non-null meaning enabled. There is no separate `ReminderSchedule` type
  with its own `enabled: Boolean` — see `ReminderScheduler`'s own KDoc for why that would only
  represent a state that can't happen.
- `ReminderScheduler` (`core/notification/ReminderScheduler.kt`) is the one platform boundary for
  turning a `ReminderPreference` into an actual scheduled notification — `schedule`/`cancel`/
  `refresh`, each returning `DataResult<Unit>`. Bound per-platform via Koin's `platformModule`
  (`AndroidReminderScheduler`/`IosReminderScheduler`), same convention as `QuestFeedbackController`.
- `NotificationPermissionStatusProvider` (a one-shot "what's the current status" read) and
  `NotificationPermissionController`/`rememberNotificationPermissionController` (a Compose-lifecycle
  -bound *request*, same shape as `MicrophonePermissionRequester`) are deliberately two separate
  contracts — the Reminder screen can display the current permission state on load without that
  display ever triggering a system prompt.
- `feature/reminder/` (`ReminderViewModel`/`ReminderScreen`/`ReminderRoute`) is the parent-facing
  editor, reached from the Family tab root, reusing `ObserveFamilySettings`/`UpdateReminderPreference`
  (Step 13.1) for persistence.
- `BootstrapViewModel` calls `ReminderScheduler.refresh` once, best-effort, on every app process
  start (alongside its existing pending-media sweep) — the self-healing path for a schedule an
  Android reboot cleared, or a stale schedule after a time-zone change.
- `DeleteAllLocalData` (Step 13.7) calls `ReminderScheduler.cancel()` once, best-effort, as part of
  a full local-data wipe — see [local-data-deletion.md](local-data-deletion.md). Neither
  `DeleteMemories` nor `ResetQuestHistory` touch reminders at all; a family's reminder preference
  is preserved by both of those narrower actions.

## Permission flow

Notification permission is **never** requested at app startup — only after a parent explicitly
turns the "Remind us" toggle on (`ReminderViewModel.onEnabledChanged`). Turning it off never
touches permission at all. A denied/permanently-denied result never silently flips the toggle back
off — the family's own intent is preserved, and the screen shows an inline "open Settings" prompt
instead, distinguishing three separate facts on screen: the reminder preference being enabled, the
OS permission being granted, and (implicitly, via that same inline warning) whether a reminder will
actually fire.

## Android specifics

- One `AlarmManager.setInexactRepeating(RTC_WAKEUP, triggerAt, INTERVAL_DAY * 7, pendingIntent)`
  per selected day of week — inexact, so no `SCHEDULE_EXACT_ALARM` permission is ever requested.
  Each day's `PendingIntent` uses a stable request code (`4200 + DayOfWeek.ordinal`), so
  re-scheduling (an edit) always replaces the same alarm rather than accumulating duplicates.
- `ReminderNotificationReceiver` (a manifest-registered, unexported `BroadcastReceiver`) builds the
  notification when an alarm fires, creating `family reminders` notification channel
  (`IMPORTANCE_DEFAULT`) idempotently.
- `ReminderBootReceiver` (`RECEIVE_BOOT_COMPLETED`) reschedules from whatever `ReminderPreference`
  is currently persisted — `AlarmManager` alarms don't survive a reboot.
- `POST_NOTIFICATIONS` (API 33+) is the only dangerous permission involved, requested only via the
  explicit-enable flow above.

## iOS specifics

- One `UNCalendarNotificationTrigger(dateMatching:, repeats: true)` per selected day of week — iOS
  computes the next weekly occurrence natively from `weekday`/`hour`/`minute` date components, so
  no next-occurrence math is needed on this platform (unlike Android's `AlarmManager`, which needs
  a concrete trigger instant up front).
- Each day's `UNNotificationRequest` uses a stable identifier (`togetherly.reminder.<DAY_NAME>`) —
  `addNotificationRequest` with an existing identifier replaces the prior pending request, so
  re-scheduling can never create duplicates.
- iOS only ever shows its own system permission prompt once per install; every later
  authorization request just replays the stored answer — `IosNotificationPermissionStatusProvider`/
  `rememberNotificationPermissionController`'s actual both account for this (a denial is reported as
  permanently denied, never "denied, can ask again").

## Automated test coverage

- `NextReminderOccurrenceTest` (commonTest) — the pure next-occurrence function Android's scheduler
  uses, fully deterministic.
- `ReminderViewModelTest` (commonTest) — permission requested only after explicit enable, a denied
  result never forcing the toggle off, day/time updates, empty-day/no-time validation, successful
  save calling `schedule`, disabling calling `cancel`, save failure never scheduling, the discard
  dialog, and the settings-navigation event.
- `AndroidReminderSchedulerTest` (`androidDeviceTest`) / `IosReminderSchedulerTest` (`iosTest`) —
  real-device/simulator smoke tests: schedule/re-schedule/cancel never throw. Neither asserts that a
  notification actually appears — that needs a human watching the device, which is what the manual
  procedure below is for. **Real OS notification delivery is never asserted in an automated test.**

## Manual test procedure

### Android

1. Install a debug build on a device or emulator running API 26+ (minSdk) — ideally also test once
   on API 33+ specifically, since that's where the `POST_NOTIFICATIONS` runtime prompt exists.
2. Open the Family tab → Reminders. Confirm the screen loads with reminders off and no permission
   prompt appears just from opening the screen.
3. Turn "Remind us" on. Confirm the system notification permission prompt appears immediately (API
   33+ only — below that, confirm no prompt appears and the permission status shows "Allowed").
4. Deny the prompt. Confirm the toggle stays on, and an inline message plus an "Open settings"
   button appear. Tap it — confirm it opens this app's own system Settings page.
5. From Settings, manually allow notifications, return to the app, and confirm the permission
   status updates to "Allowed" the next time the screen is opened (or immediately, if the app
   re-checks on resume).
6. Select one or two days and a time roughly 2 minutes in the future. Save.
7. Background the app (or lock the device) and wait for the scheduled time. Confirm a notification
   titled "A little time together?" appears, tapping it opens the app.
8. Edit the reminder to a different time/day set and save again. Confirm only the new schedule
   fires — no duplicate/stale notification from the old configuration.
9. Turn reminders off and save. Confirm no notification fires at the previously-scheduled time.
10. With a reminder enabled and saved, reboot the device (or use `adb shell am broadcast -a
    android.intent.action.BOOT_COMPLETED`). Confirm the reminder still fires afterward.

### iOS

1. Install a debug build on a simulator or device (deployment target 16.0).
2. Repeat steps 2–5 above — iOS shows its own system prompt only once ever; if you deny it, the app
   can only offer "Open Settings" (`UIApplicationOpenSettingsURLString`) from then on, never
   re-prompt in-app.
3. Select days/time roughly 2 minutes out, save, background the app, and confirm the notification
   ("A little time together?" / "Your Togetherly quest is ready whenever your family is.") appears
   and opens the app when tapped.
4. Edit and re-save; confirm no duplicate fires (stable per-day identifiers replace, not
   accumulate).
5. Disable and save; confirm nothing fires afterward.
6. iOS has no reboot-clears-alarms concern (`UNUserNotificationCenter` schedules survive a device
   restart on its own) — no boot-rescheduling step to verify here.

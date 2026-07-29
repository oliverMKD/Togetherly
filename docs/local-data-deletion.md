# Local data management and deletion (Step 13.7)

Togetherly has no backend and no accounts — everything the app knows about a family lives in the
local `TogetherlyDatabase` (see [persistence.md](persistence.md)) and the private media directory
(see [private-media.md](private-media.md)). This document is the single source of truth for what
each parent-facing deletion action actually does, what it deliberately does not do, and how to
verify it manually. For the local privacy model these actions operate inside, see
[privacy.md](privacy.md).

The Data Management screen (`feature/family/presentation/DataManagementScreen.kt`, reached from
the Family tab) offers exactly three actions. Every button label is the action's own name —
**never** an ambiguous label like a bare "Reset".

## Delete memories

Clears: every completion's note, and every attached photo/voice file (database metadata row +
the actual file, via `PrivateMediaCommitter.deleteCommitted`).

Preserves: quest completions themselves (the fact that a quest happened, and any family
reactions to it), saved quests, the family profile and every preference, and RevenueCat purchase
history.

**Why completions survive this action:** a memory (note + photo/voice) is not a separate database
table from a quest completion in this schema — see
[persistence.md's three tiers of database deletion](persistence.md#three-tiers-of-database-deletion).
Rather than silently deleting both (which the domain genuinely could not separate before this
step), `MemoryCleaner.clearAllMemoryContent()` was added specifically to clear a completion's
`note`/`memory_media` while leaving the `quest_completion` row and its `completion_reaction` rows
untouched. Reactions are treated as completion metadata, not memory content, and are therefore
never cleared by this action.

Coordinator: `DeleteMemories` (`domain/localdata/usecase/DeleteMemories.kt`).

## Reset quest history

Deletes: today's/any daily selection, every dismissal, the active quest session (if any), and
every quest completion — including that completion's memory (note + media, files included).

Preserves: saved quests, the family profile, and every preference (quest preferences, reminder
preference, memory preferences, privacy preferences).

**Why saved quests are preserved:** a family's curated "quests we want to come back to" list is a
deliberate collection, not a byproduct of playing — it is not itself a history record the way a
completion or a dismissal is. Preserving it is the safer, more conservative default per this
feature's own product intent (prefer preserving unless a saved quest is explicitly redefined as
history somewhere else).

Coordinator: `ResetQuestHistory` (`domain/localdata/usecase/ResetQuestHistory.kt`).

## Delete all local data

Deletes everything above, plus the family profile and every preference, and returns the app to
onboarding.

It never:

- Cancels an App Store or Google Play subscription.
- Revokes Family Plus.
- Deletes store purchase history.
- Deletes anything outside Togetherly's own private storage (the database file, and the
  `<app-private-storage>/media` directory — see [private-media.md](private-media.md)).
- Calls RevenueCat `logIn()`/`logOut()` — see [RevenueCat data boundary](#revenuecat-data-boundary)
  below.

It also (Step 14.1) resets telemetry consent to `NotAsked` and clears the locally persisted
consent record — see [docs/telemetry.md](telemetry.md#local-data-deletion-resets-consent) for
exactly what that does and does not erase (in particular: never analytics/diagnostics data already
transmitted before deletion, if any was ever sent).

Coordinator: `DeleteAllLocalData` (`domain/localdata/usecase/DeleteAllLocalData.kt`) — the one
application-level coordinator responsible for deletion ordering; no ViewModel performs
multi-repository deletion logic itself.

### Ordering: database first, files best-effort after

`DeleteAllLocalData` does not run its steps in the order this feature's own task spec lists them —
it wipes the database *first*, as the one all-or-nothing, transactional, critical step, and only
performs the remaining steps (cancel reminders, delete media files, clear the entitlement cache)
once that succeeds:

1. Read every completion's media *before* deleting anything, so the files can still be found
   afterward.
2. `FamilyDataCleaner.deleteAllFamilyData()` — one atomic transaction. **If this fails, everything
   below is skipped and the failure is returned immediately** — nothing else has touched real state
   yet, so a family that hits this error still has every reminder, file, and cached entitlement
   exactly as before. This is what "never claim deletion succeeded if critical data remains" means
   here.
3. Only once step 2 succeeds: cancel local reminders (`ReminderScheduler.cancel()`), best-effort
   delete every file read in step 1 (`PrivateMediaCommitter.deleteCommitted`, counting individual
   failures), and clear the locally cached entitlement snapshot
   (`EntitlementRepository.clearCache()`).

`DeleteMemories`/`ResetQuestHistory` follow the same two-phase shape at their own smaller scale:
read media → clear/delete the database rows (the one all-or-nothing step) → best-effort delete
files. A crash between the database step and the file step can only ever leave an orphaned file
behind, never a database reference to a file that's already gone — the same "an orphaned file is
the safe failure mode" reasoning `PendingMediaOrphanCleaner` already established (see
[private-media.md](private-media.md)).

### Partial failure

A file deletion can fail (permissions, filesystem error) independently of the database step that
already succeeded. All three coordinators return `DataResult<Int>` — the `Int` is the count of
individual file deletions that failed, `0` meaning fully clean. **A non-zero count is still an
overall success**: the database rows (what the action's own name promises to delete) are
unconditionally gone by that point, so returning an error would incorrectly suggest the deletion
as a whole didn't happen. `DataManagementViewModel` shows a softer "some files couldn't be removed"
notice instead of the plain success message when this count is non-zero, but never blocks
navigation or claims failure. A missing file is not a failure at all — `PrivateMediaStorage.deleteCommitted`
already treats a not-found file as success (idempotent), the same as everywhere else in this
codebase that deletes committed media.

### Concurrency

Each of the three use cases (`DeleteMemories`, `ResetQuestHistory`, `DeleteAllLocalData`) guards
itself with its own `kotlinx.coroutines.sync.Mutex`: a concurrent invocation is rejected with
`AppError.Validation(ValidationError.INVALID_STATE)` rather than queued or interleaved. This is
defense in depth on top of (not instead of) `DataManagementViewModel`'s own UI-level busy-state,
which already disables every destructive entry point and blocks back navigation (including the
system/gesture back button, via `BackHandler`) while a deletion is running.

## RevenueCat data boundary

- Deleting local data is **not** a subscription cancellation. A subscription is managed entirely
  by the App Store or Google Play; nothing in this app can cancel one.
- Customer Center (reached from Family Plus management) is the only in-app surface for managing a
  subscription — see `docs/revenuecat-setup.md`.
- Store purchase history remains with Apple or Google regardless of anything this app deletes.
- `EntitlementRepository.clearCache()` clears only the *locally cached* offline entitlement
  snapshot (a `database_metadata` row) and resets in-memory state to free — it never touches
  RevenueCat's own server-side record. It fires a background refresh immediately afterward
  (non-blocking), so a still-genuinely-entitled family sees Family Plus access reappear on its own
  once the provider reconciles.
- After returning through onboarding, ordinary app flow (`FamilyPlusManagementViewModel`'s own
  `observeAccess()` subscription, `RevenueCatEntitlementRepository`'s permanent
  `observeCustomerAccess()` collector) refreshes entitlement state the normal way — no special
  post-deletion refresh path exists or is needed.
- **This app never calls RevenueCat `logIn()`/`logOut()`, anywhere, including during deletion.**
  Every family is identified only by RevenueCat's own auto-generated anonymous on-device identity
  — see `RevenueCatConfigurator`'s own KDoc. Deleting local data does not migrate, reset, or
  otherwise touch that identity; a fresh anonymous ID is never manufactured. Restoring Family Plus
  access after a full local wipe works exactly the way "Restore purchases" always has, through the
  same anonymous identity RevenueCat already recognizes on that device.

## Returning to onboarding

`BootstrapViewModel` decides onboarding-vs-ready purely from whether a family profile exists (see
that class's own KDoc) — there is no separate "has onboarded" flag to clear. Deleting the family
profile *is* clearing onboarding state. After a successful "Delete all local data",
`TogetherlyNavHost` clears the entire root back stack down through `Main` and navigates directly to
`Onboarding` — it does not rely on re-entering `Bootstrap` (which is removed from the back stack
the moment the app first reaches `Main`), since that destination may no longer be present to pop
back to.

## Manual testing checklist

No emulator/simulator was available while implementing this feature — the checklist below is what
a developer with a device/simulator should walk through before shipping:

1. Seed a family with: a profile, quest preferences, an enabled reminder, at least two completed
   quests where at least one has a note, a photo, and a voice memory attached, at least one saved
   quest, and (if a RevenueCat sandbox account is available) an active Family Plus subscription.
2. **Delete memories** — confirm the single confirmation dialog appears, explains the effect, and
   that Cancel leaves everything untouched. Confirm; verify notes/photos/voice recordings are gone
   from the Journey timeline but completions themselves, saved quests, and the family profile
   remain.
3. **Reset quest history** — same dialog flow; verify today's quest/dismissals/completions are
   gone, saved quests and the family profile remain, and a new quest can be selected for today
   afterward.
4. **Delete all local data** — verify the two-stage flow: stage one's explanatory dialog (mentions
   irreversibility, that purchases are separate, and that the app returns to setup), then stage
   two's hold-to-confirm parental gate. Confirm; verify the app lands on onboarding, back
   navigation does not return to Main/Family screens, and (if a device/simulator with the App
   Store/Play Store is available) that the subscription itself is still active in the platform's
   own subscription management UI.
5. Complete onboarding again as a "new" family; if a subscription was active before the wipe,
   verify Family Plus access reappears (either automatically once RevenueCat reconciles, or via
   "Restore purchases" on the Family Plus management screen).
6. Repeat step 4 with the device offline (airplane mode) to confirm the local wipe still succeeds
   and the app still reaches onboarding — network access is never required for local deletion.
7. Attempt a rapid double-tap on each destructive confirm button; verify only one deletion runs
   (no duplicate/interleaved operation, no crash).
8. With a screen reader enabled, verify each destructive button's label and each dialog's
   title/body are announced, and that the busy/loading state during an active deletion is
   announced too.
9. With large system text enabled, verify every confirmation dialog remains fully readable and its
   buttons stay reachable (nothing is clipped or pushed off-screen).

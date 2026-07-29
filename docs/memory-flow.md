# Completion Memory Flow

The completion memory flow (Steps 10.1–10.4) is how a family optionally attaches a note,
reactions, a photo, and a voice recording to a quest they just completed. This document covers the
draft model, the save/skip/discard contract, the capture screen's own behavior, and where private
media (Step 10.2/10.3) fits in. For the storage layer itself see
[private-media.md](private-media.md); for how a saved memory later appears in Journey see
[journey.md](journey.md).

## Every field is optional

A `QuestCompletion` is valid with no memory content at all — `note`, `reactions`, and `media` all
default to empty/absent. The user can tap Skip at any point without losing the completion itself;
nothing about memory capture can fail the completion that was already recorded when the quest was
finished (Step 9.x).

## Draft model

`CompletionMemoryDraft` (`domain.completion`) is a presentation-independent, in-progress edit —
constructing one never touches storage or creates a permanent record. It carries at most one
`PendingMediaReference` (photo) and one `PendingVoiceReference` (voice + duration), never a
domain `MemoryMedia` value, since a draft only ever describes *not-yet-committed* files. Both
reference types are opaque strings under the private media root — never an Android `Uri`, an iOS
`NSURL`, a public URL, a photo-library identifier, or raw bytes (see
[private-media.md](private-media.md)).

## Save

`SaveCompletionMemory` normalizes the note (blank → `null`, non-blank → `MemoryNote`, which rejects
surrounding whitespace and over-length input), commits any newly-staged photo/voice via
`PrivateMediaCommitter`, and writes the updated `QuestCompletion` in one call. `MediaEdit<T>`
(`Unchanged`/`Remove`/`Replace`) is how a save distinguishes "nothing new was staged, keep what's
there" from "delete the existing one" — a plain nullable reference can't express both at once.
Media is committed **before** the database write, and only deleted (a superseded file) **after** it
succeeds — see that class's own KDoc for the full compensating-cleanup contract on a partial
failure. A save failure of any kind preserves the in-progress draft exactly as the user left it;
nothing is reset.

`CompletionMemoryViewModel.onSaveClicked` guards against a duplicate tap synchronously (checks
`isSaving`/`isRecording` before doing anything else, the same guard convention documented in
[architecture.md](architecture.md#presentation-conventions)) and stops any active voice playback
before saving.

## Skip and discard

Both `SkipClicked` and a confirmed Back-with-changes call `DiscardCompletionMemoryDraft`, which
deletes only the draft's *pending* files (`PrivateMediaCommitter.deletePending`) — it has no
`CompletionRepository` dependency at all, so it is structurally incapable of touching the
already-persisted completion. An already-committed memory (from a prior save) is never touched by
Skip/Discard; only newly-staged, not-yet-saved media is cleaned up.

`CompletionMemoryViewModel.onBackClicked` shows a discard-confirmation dialog only when there are
actual unsaved changes (`hasUnsavedChanges()` compares current note/reactions/pending-media state
against what the screen loaded with) — Back with no changes exits immediately, no dialog.

## Capture screen (Step 10.4)

`feature.memory` (`model`/`presentation`/`mapper`/`ui`) is the Compose screen wiring the above
together with the photo picker and voice recorder:

- **Reactions**: a multi-select set of `FamilyReaction` (label + emoji, mapped in
  `FamilyReactionMapper`), toggle-only, no minimum/maximum count enforced by the UI.
- **Note**: a single multiline field with a character limit (`MemoryNote.MAX_LENGTH`); a failed
  save due to an over-length/invalid note surfaces as a field-level `noteError`, never the generic
  `saveError`.
- **Photo**: add/replace/remove, previewed via a private thumbnail decoded transiently
  (`PrivatePhotoThumbnail` + `decodeToImageBitmap` expect/actual — Skia on iOS, `BitmapFactory` on
  Android, since Android's Compose target has no direct Skia access) — raw bytes are never stored
  in `CompletionMemoryUiState`, only loaded on demand through a Route-supplied suspend lambda.
- **Voice**: record/stop/cancel/play/pause/remove, capped at 60 seconds
  (`VoiceRecordingLimits.MAXIMUM_DURATION`). A permanently-denied microphone permission now (Step
  10.7 fix) shows a real "Open Settings" action — see below.
- **Privacy copy**: `memory_privacy_reassurance` ("Stored privately with Togetherly on this
  device.") — see [private-media.md](private-media.md#backup-and-cloud-sync-privacy-step-107-finding--fix)
  for why this claim is now actually backed by a backup exclusion, not just implied by directory
  choice.

### Microphone permission Settings fix (Step 10.7)

`memory_voice_open_settings_action` ("Open Settings") existed as a string resource since Step 10.4,
but no button was ever wired to it — a permanently-denied microphone permission left the user with
no in-app path to fix it. Fixed by adding `CompletionMemoryUiState.microphonePermissionPermanentlyDenied`
(set on `MicrophonePermissionResult.PermanentlyDenied`, cleared on any new Record attempt) and a
platform-neutral `rememberAppSettingsLauncher()` (`core.media`, `expect`/`actual`) — Android opens
`Settings.ACTION_APPLICATION_DETAILS_SETTINGS` for the app's own package; iOS opens
`UIApplicationOpenSettingsURLString`. The launcher is resolved only at `CompletionMemoryRoute`'s
boundary, the same Route-only rule every other platform launcher in this app follows — the
`ViewModel` only ever sees the boolean flag, never a platform intent/URL type.

## Navigation

Reached only from Completion Celebration's "Add a memory" action (`CompletionCelebrationAction.AddMemoryClicked`
→ `NavigateToMemory(completionId)`), never from the celebration's error-state Close button (that
button uses the separate `ContinueClicked`/`NavigateToToday` pair specifically so a completion this
screen never confirmed actually loaded can't be sent into memory capture). Both Save and Skip pop
back down to `Main` — see `TogetherlyNavHost`'s `composable<CompletionMemory>` block.

## Memory settings (Step 13.5)

`MemoryPreferences` (`domain/family/MemoryPreferences.kt`) is four independent toggles a family
controls from the Family tab's Memory Settings screen: `allowPhotos`, `allowVoiceMemories`,
`allowTextNotes`, and `showMemoryPromptAfterQuests` (whether the "Add a memory" prompt is offered
at all after a completion). All four default to `true`, so an install that predates this setting
never silently loses a capability it already had.

Enforcement is entirely forward-looking: disabling a toggle only changes what's *offered* the next
time a family completes a quest — `CompletionCelebrationViewModel` reads `showMemoryPromptAfterQuests`
to decide whether "Add a memory" is even shown, and `CompletionMemoryViewModel` reads
`allowPhotos`/`allowVoiceMemories`/`allowTextNotes` to hide the corresponding capture control on
the memory-capture screen itself. Neither ViewModel ever inspects, hides, or deletes an
*already-saved* completion's existing note/photo/voice/reactions because a toggle was turned off —
disabling photo capture, for example, does not retroactively hide or remove photos a family already
saved before disabling it. The only way to actually remove existing memory content is the separate,
explicit "Delete memories" action — see [Bulk deletion](#bulk-deletion-step-137) below.

## Bulk deletion (Step 13.7)

The Family tab's "Delete memories" action clears every completion's note and media at once —
`MemoryCleaner.clearAllMemoryContent()`, coordinated by `DeleteMemories`
(`domain/localdata/usecase/DeleteMemories.kt`). It intentionally does not reuse `DeleteCompletion`
or `SaveCompletionMemory`: both of those operate on one completion at a time and (in
`SaveCompletionMemory`'s case) require a caller-supplied replacement draft, neither of which fits
"clear every memory, in bulk, with nothing left to replace it with." See
[local-data-deletion.md](local-data-deletion.md) for exactly what this action does and does not
delete, including why the completion itself always survives it.

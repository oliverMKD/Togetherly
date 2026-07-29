# Privacy, Filesystem, Lifecycle, and Accessibility Audit (Step 10.7)

This is the Step 10.7 audit of everything built across Steps 10.1–10.6 (reactions, notes, private
photo/voice memories, derived Journey stars, the Journey timeline). No new product features were
added in this step — every change below is either a verification with no code change, or a fix for
a genuine gap the audit found. For the features themselves, see
[memory-flow.md](memory-flow.md), [private-media.md](private-media.md), and
[journey.md](journey.md).

## Findings and fixes

Four real gaps were found and fixed:

1. **Orphan pending-media cleanup was never invoked.** `PendingMediaOrphanCleaner.deleteExpiredPending`
   existed since Step 10.2, bound in Koin, but no call site anywhere in the app ever called it — a
   pending photo/voice file abandoned by a killed memory-capture session would never be swept.
   Fixed: called once, best-effort, from `BootstrapViewModel`'s `init` (Bootstrap runs exactly once
   per process). See [private-media.md](private-media.md#orphan-cleanup).
2. **Private media was not excluded from platform backup on either OS**, despite the in-app copy
   ("Stored privately with Togetherly on this device.") implying on-device-only storage. Fixed on
   Android (`backup_rules.xml`/`data_extraction_rules.xml`, excluding `media/`) and iOS
   (`NSURLIsExcludedFromBackupKey` set on the media directory). See
   [private-media.md](private-media.md#backup-and-cloud-sync-privacy-step-107-finding--fix).
3. **`DeleteCompletion` never cleaned up committed media files** (deferred since Step 4.4, before
   `PrivateMediaCommitter` existed, and never revisited) and had **zero test coverage**. Deleting a
   completion left its photo/voice files on disk forever. Fixed: it now deletes the Room row first,
   then best-effort deletes the completion's own committed media, mirroring
   `SaveCompletionMemory`'s "delete a file only once nothing references it" ordering. Six new tests
   added (`DeleteCompletionTest`), including that unrelated completions' files are untouched and
   that a delete failure never triggers media deletion.
4. **"Open Settings" for a permanently-denied microphone permission was dead copy.** The string
   existed since Step 10.4; no button called it. Fixed with a new `rememberAppSettingsLauncher()`
   (`core.media`, `expect`/`actual`) and a `CompletionMemoryUiState.microphonePermissionPermanentlyDenied`
   flag. See [memory-flow.md](memory-flow.md#microphone-permission-settings-fix-step-107).

## End-to-end scenarios

All six scenarios below trace through existing automated coverage (`SaveCompletionMemoryTest`,
`DiscardCompletionMemoryDraftTest`, `CompletionMemoryViewModelTest`, `DeleteCompletionTest`,
`JourneyStarPolicyTest`/`JourneyConstellationPolicyTest`) plus the reasoning documented in
[memory-flow.md](memory-flow.md) and [journey.md](journey.md); none required new product code
beyond the four fixes above.

- **Note and reactions**: complete → add reactions/note → save → the memory is part of the same
  `QuestCompletion` row from that point on, so it survives any repository re-read (a "restart") by
  construction, appears in Journey (one completion, one derived star — Step 10.5), never a second
  row.
- **Photo**: cancel leaves no pending file or draft reference (`PhotoPickerResult.Cancelled` is a
  no-op in the `ViewModel`); a real pick stages a pending file; save commits it to permanent
  private storage and Journey reads its thumbnail via `openPhotoThumbnail`; the picker's own
  platform URI is never stored anywhere (`PendingMediaReference`/`MediaReference` are opaque
  strings under the private root — see [private-media.md](private-media.md)).
- **Voice**: a denied permission shows a recoverable in-app message; permanent denial now has a
  working "Open Settings" path (fix #4); record → stop → preview → re-record replaces the pending
  clip (the previous pending file is deleted immediately, never left as an orphan until save); save
  commits exactly one voice file per completion (`QuestCompletion`'s own `init` enforces at most one
  photo and one voice); Journey plays it back after "restart" the same way any other committed
  reference resolves.
- **Discard**: `DiscardCompletionMemoryDraft` deletes only pending files, has no `CompletionRepository`
  dependency at all (structurally cannot touch the persisted completion), and adds no media
  metadata anywhere.
- **Save failure**: `SaveCompletionMemory` commits media *before* the database write and only
  deletes a superseded file *after* it succeeds — a database write failure never leaves a
  database reference to a missing file, and compensating cleanup removes whatever was newly
  committed during that failed attempt while an existing, unrelated committed memory is left
  completely untouched.
- **Deletion**: fixed in this step (finding #3, above) — a deleted completion's Room metadata, its
  committed photo/voice files, and its derived Journey star (which never persisted independently in
  the first place — see [journey.md](journey.md#no-journey_star-table--a-derived-read-model)) are
  all gone; an unrelated completion's files are untouched (`DeleteCompletionTest.unrelatedCompletionsAndTheirFilesAreUntouched`).

## Filesystem security audit

Verified directly against `PrivateMediaPaths`/`AndroidPrivateMediaRoot`/`IosPrivateMediaRoot`:

- Every reference stays inside the private media root; `isSafeRelativeReference` rejects blank,
  absolute, backslash-containing paths and any `.`/`..` segment — no directory traversal.
  Filenames use generated IDs only — never a family name, quest title, or original picker filename.
- No world-readable file: Android uses `filesDir` (app-private by default), iOS uses `Application
  Support` (sandboxed to the app) — neither is shared/public storage.
- No absolute path is ever stored in Room — `MediaReference`/`PendingMediaReference` hold only
  root-relative strings.
- **Backup**: addressed by fix #2, above — this project no longer claims "stored privately on this
  device" without also actually excluding that storage from platform backup.

## Permission audit

- No broad Android storage permission anywhere in the manifest (`AndroidManifest.xml` grants only
  `VIBRATE` and `RECORD_AUDIO`) — the system Photo Picker
  (`ActivityResultContracts.PickVisualMedia`) needs no storage permission of its own.
- iOS's `PHPickerViewController` needs no full-library access, and `iosApp/Info.plist` has no
  `NSPhotoLibraryUsageDescription` key (confirming the app never asked for one).
- `RECORD_AUDIO`/`NSMicrophoneUsageDescription` are requested only when the user taps Record
  (`CompletionMemoryAction.RecordVoiceClicked` → `RequestMicrophonePermission`), never during
  onboarding or at app startup — verified by grepping `feature.onboarding` for any
  permission-requester import (none found).
- A permission result (`MicrophonePermissionResult`) is never persisted as domain state — it only
  ever flows through a `ViewModel` action into transient `UiState` fields
  (`mediaError`/`microphonePermissionPermanentlyDenied`), never written to `QuestCompletion` or any
  Room entity.
- Permanent denial now provides real Settings guidance (fix #4).

## Metadata audit

Verified by reading `AndroidImageNormalizer`/`IosImageNormalizer` directly: both always
decode-then-re-encode a picked image (`BitmapFactory`/`Bitmap.compress` on Android,
`UIImage`/`UIImageJPEGRepresentation` on iOS) rather than copying the original bytes. Re-encoding
through a bitmap round trip produces a plain JPEG with no EXIF block — GPS, device model, original
filename, and author/owner fields are all structurally absent from the committed file, not merely
stripped by a separate, skippable step. This was already correct since Step 10.2; no change needed.

## Audio lifecycle audit

Verified against `VoiceRecorder`'s own contract KDoc and both platform implementations: the
recorder is released on stop, cancel, internal error, and the automatic 60-second cutoff — every
terminal path releases it, none can leak it. `VoicePlaybackController.play`/`playPending` always
stop and release whatever was already playing first, so at most one clip plays at once, on either
platform. `JourneyRoute`'s `DisposableEffect` (Step 10.6/10.7) stops playback when Journey leaves
composition; `CompletionMemoryViewModel.onSaveClicked` stops playback before saving. No background
recording exists anywhere — recording only runs while its owning screen is composed.

## Database audit

Enforced directly in `QuestCompletion`'s own `init` block: at most one `MemoryMedia.Photo` and one
`MemoryMedia.Voice` per completion (`requireAtMostOnePhotoAndVoice`), unique media IDs
(`requireUniqueValues`), and a completion remains valid with zero media at all. `MediaReference`/
`PendingMediaReference` are opaque path strings — no media bytes are ever stored in Room. Cascade
metadata deletion is fix #3, above. Memory replacement (`SaveCompletionMemory` with
`MediaEdit.Replace`) updates the same completion row in place — it was already structurally
incapable of duplicating rows (the domain model has exactly one `media: List<MemoryMedia>` field,
replaced wholesale on save, not appended to).

## Accessibility audit

- Reactions are ordinary `FilterChip`-backed selections (`TogetherlyChoiceChip`) with correct
  selected/checkbox semantics built into the design-system component, not hand-rolled.
- The note field's optionality is conveyed through its own placeholder copy
  (`memory_note_placeholder`, "Write a note (optional)") rather than a separate accessibility-only
  label.
- Photo thumbnail/replace/remove controls all carry explicit `contentDescription`s
  (`memory_photo_thumbnail_description`, `TogetherlyIconButton`'s required parameter for any
  icon-only control).
- Recording state is announced via visible text (`memory_voice_recording_label`), not a
  `liveRegion` re-announcing every second — the same restraint `docs/quest-mode.md`'s own Step 9.7
  audit already established for a running timer (only a *terminal* state change deserves a live
  region, not continuous progress).
- Voice play/pause uses a single icon button whose `contentDescription` switches between
  `memory_voice_play_action`/`_pause_action` (and the Journey-timeline equivalents) based on actual
  playback state — never a static label describing the wrong action.
- Save's loading state uses each button's own built-in `loading` parameter
  (`TogetherlyPrimaryButton`/`TogetherlySecondaryButton`/`TogetherlyTextButton`), which already
  handles the correct disabled/busy semantics project-wide.
- The discard-confirmation dialog is Material3's own `AlertDialog`, inheriting its platform-correct
  focus trapping and dismissal semantics, the same convention `docs/quest-mode.md`'s exit-dialog
  audit already verified.
- Journey's timeline reads in ordinary top-to-bottom `LazyColumn` order (newest first); its
  constellation header carries exactly one combined `contentDescription`
  (`journey_constellation_content_description`) rather than one announcement per decorative star,
  and no star is independently focusable.
- Reduced motion: Journey's star twinkle animation is skipped entirely (static full alpha) when
  `MaterialTheme.togetherlyReduceMotion` is set — verified by reading `JourneyConstellationHeader`'s
  `rememberTwinkleAlpha` directly.
- Large font: both `CompletionMemoryScreenPreviews`/`JourneyScreenPreviews` include a
  `@Preview(fontScale = 2f)` case; `TogetherlyScreen`'s independent scroll region (established in
  `docs/quest-mode.md`'s own audit) already covers reachability at large font sizes project-wide.
- Dark mode / 48dp touch targets: both screens' previews include dark-theme variants, and every
  interactive control here is built on `TogetherlyIconButton`/`TogetherlyPrimaryButton`/etc., which
  already enforce `MaterialTheme.togetherlySize.minimumTouchTarget` (48dp) project-wide — no
  per-feature opt-out exists.

No accessibility gap requiring a fix was found this pass (contrast with `docs/quest-mode.md`'s own
Step 9.7 audit, which did find and fix a `liveRegion` gap) — every checklist item above was already
correctly built during Steps 10.1–10.6, or is covered by design-system defaults that apply here
automatically.

## Architecture audit

- `domain.completion`/`domain.journey` contain no platform media type — `MediaReference`/
  `PendingMediaReference`/`PendingVoiceReference` are plain value classes wrapping `String`/`Duration`.
- Neither `CompletionMemoryViewModel` nor `JourneyViewModel` imports a picker/permission/platform
  media type — both receive only domain types and plain Kotlin callbacks; the photo picker and
  microphone permission requester are resolved exclusively at each feature's `Route` boundary
  (`CompletionMemoryRoute`), never inside a `ViewModel` or a reusable screen piece.
- A picker's platform URI never enters `domain` — `PhotoImportSource`/the picker contract convert
  it into private storage before any domain type is constructed.
  `PendingMediaReference`/`MediaReference` stay clearly distinct types throughout (a pending
  reference can never be handed to a method expecting a committed one, or vice versa, without a
  compile error).
- Media commit uses compensating cleanup (`SaveCompletionMemory`); Journey stars are derived, never
  stored (`journey.md`); the Journey UI reads photo thumbnails, never full-resolution decodes, in
  its list rows; voice playback stays platform-isolated behind `VoicePlaybackController`; photo and
  microphone permission handling stay at the platform/UI boundary (`core.media`), never inside a
  use case or repository.
- No design-system component resolves anything from Koin (`grep -rn koinInject
  shared/src/commonMain/kotlin/com/togetherly/designsystem` — no matches); `koinViewModel`/
  `koinInject` calls exist only at each feature's `Route`.
- No memory content (note text, reactions, photo/voice bytes or references) appears in any
  `println`/`Log.`/`NSLog` call anywhere in `feature.memory`, `feature.journey`, or
  `domain.completion` — confirmed by exhaustive grep, not spot-checking known call sites (this
  project has no analytics/telemetry integration at all yet, so there is nothing to audit on that
  front beyond confirming its absence).

## Documentation

This file, [memory-flow.md](memory-flow.md), [private-media.md](private-media.md), and
[journey.md](journey.md) are new in this step. [architecture.md](architecture.md) was updated
alongside them (new `feature.memory`/`feature.journey` package entries, `core.media` note).

## Validation

- `./gradlew :shared:allTests` — full suite, Android host tests + iOS Simulator tests, all passing
  throughout this step's fixes (the two new test files —
  `com.togetherly.domain.completion.usecase.DeleteCompletionTest`,
  plus the extended `CompletionMemoryViewModelTest`/`BootstrapViewModelTest` — all pass on both
  targets).
- `./gradlew :androidApp:assembleDebug` — confirms the whole Android app (manifest, new
  backup-rules XML resources, the new `core.media.AppSettingsLauncher` platform code) still
  assembles.
- `:shared:compileKotlinIosSimulatorArm64` — confirms the new iOS-specific code (`NSURL`/
  `UIApplication` cinterop in `AppSettingsLauncher.ios.kt` and `IosPrivateMediaRoot`) compiles
  correctly against the real Apple SDK headers, not just common code.
- iOS was not run on a simulator/device interactively (no macOS UI available in this environment);
  Android was not manually exercised interactively either — both platforms were validated through
  compilation, the full automated test suite on both targets, and direct code reading, consistent
  with how `docs/quest-mode.md`'s own Step 9.7 audit was validated.

## See also

- The in-app Privacy summary screen (`feature/family/presentation/PrivacyScreen.kt`, Step 13.5) is
  the parent-facing, plain-language counterpart to this audit — reachable from the Family tab.
- [local-data-deletion.md](local-data-deletion.md) (Step 13.7) documents the parent-facing actions
  that act on everything this audit describes: "Delete memories" (photos, voice, notes),
  "Reset quest history" (completions, daily selections, dismissals), and "Delete all local data"
  (everything, including the family profile) — along with the RevenueCat data boundary those
  actions must never cross.

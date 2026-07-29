# Private Media (Photo + Voice)

Private media (`data.media`, backed by `domain.completion`) is the app-private photo/voice storage
layer completion memories use (Steps 10.2–10.3). This document covers the directory layout, the
pending/committed lifecycle, image normalization, audio format, path-safety validation, orphan
cleanup, and the Step 10.7 filesystem/backup privacy audit.

## Directory layout

- **Android**: `<filesDir>/media` (`AndroidPrivateMediaRoot`) — internal app-specific storage,
  never `MediaStore`, never a directory requiring a storage permission.
- **iOS**: `<Application Support>/media` (`IosPrivateMediaRoot`) — never `Documents` (user-visible
  in the Files app) and never a public Photos-library writeback.

Both roots are resolved through the common `PrivateMediaRoot` interface; every other class in
`data.media` reaches the filesystem only through `PrivateMediaPaths` (pure path construction, no
I/O) plus a platform-specific read/write implementation — never by constructing a path inline.

Every reference the domain layer holds — `PendingMediaReference`, `MediaReference` — is a relative
path string under that root, validated by `PrivateMediaPaths.isSafeRelativeReference` before any
filesystem call: blank, absolute (leading `/`), backslash-containing, or any path segment equal to
`.`/`..` is rejected outright. Filenames are always built from generated IDs
(`MemoryMediaId`/a generated pending ID) — never a family name, quest title, note content, or the
original picker filename.

## Pending → committed lifecycle

1. **Pending**: `createPendingPhoto`/`VoiceRecorder` write into `pending/` under the media root.
   A pending file represents an in-progress edit only — no domain `MemoryMedia` value can ever
   reference it (see `CompletionMemoryDraft`'s own KDoc, Step 10.1).
2. **Commit**: `commitPhoto`/`commitVoice` promote a pending file into `completions/<completionId>/`
   permanent storage, returning a domain `MemoryMedia.Photo`/`.Voice`. A committed photo also gets a
   thumbnail file (`thumb-<mediaId>.jpg`), derived from the photo's own reference by naming
   convention (`PrivateMediaPaths.thumbnailReferenceFor`) rather than stored as a second
   independent, driftable reference.
3. **Delete**: `deleteCommitted`/`deletePending` are reference-agnostic — the same method deletes a
   photo, its thumbnail, or a voice file; deleting a photo reference always deletes its thumbnail
   alongside it.

`SaveCompletionMemory` (Step 10.1) only ever deletes a superseded committed file *after* the
database write that stops referencing it succeeds, and only ever deletes a newly-committed file on
its own failure path (compensating cleanup) — see that class's own KDoc. `DeleteCompletion` follows
the same ordering (Step 10.7 fix, see below).

## Image normalization

`ImageNormalizer` (`AndroidImageNormalizer`/`IosImageNormalizer`) always decodes the picker's raw
bytes to a platform bitmap (`Bitmap`/`UIImage`), corrects EXIF orientation into the pixels
themselves, downscales if needed, and **re-encodes** a fresh JPEG — it never copies the picker's
original bytes verbatim. This is what makes the metadata audit below true: a decode/re-encode round
trip produces a JPEG with no EXIF block at all, so GPS coordinates, device model, and any other
original metadata are structurally impossible to carry through, not merely stripped by a separate
step that could be missed.

## Audio format

Both platforms record AAC audio in an `.m4a` container (`MediaRecorder`'s `MPEG_4`/`AAC` output on
Android, `AVAudioRecorder`'s `kAudioFormatMPEG4AAC` on iOS) — see `VoiceRecordingLimits` for the
60-second cap enforced during capture.

## Path-safety and validation tests

`PrivateMediaPaths`' segment/reference validation, `thumbnailReferenceFor`'s naming-convention
derivation, and both platforms' pending→committed→delete flows (including compensating cleanup on
a failed commit) are covered by `AndroidPrivateMediaStorageTest`/`IosPrivateMediaStorageTest`-style
suites plus `SaveCompletionMemoryTest` at the use-case layer.

## Orphan cleanup

`PendingMediaOrphanCleaner.deleteExpiredPending(now, thresholdAge = 24.hours)` deletes pending files
older than the threshold — a stray file left behind by a killed/crashed memory-capture session
(picker returned, app killed before Save/Skip/Discard ran) never accumulates forever. This
contract existed since Step 10.2 but **was never actually invoked anywhere in the app** until the
Step 10.7 audit found the gap: it is now called once, best-effort, from `BootstrapViewModel`'s
`init` block (Bootstrap runs exactly once per app process). A sweep failure never blocks or
degrades the onboarding/ready decision that's `BootstrapViewModel`'s actual job.

The broader orphan-consistency policy this project follows (per the Step 10.7 spec) is
intentionally conservative:

- Automatically delete expired **pending** files (implemented, as above).
- A committed file with no database reference, or a database reference to a missing committed
  file, is **not** automatically resolved by any code in this app today — there is no periodic
  consistency sweep for committed media. The preferred policy going forward is "report for safe
  cleanup," never "silently delete a database record because its file is missing" (a missing
  committed file should show a "media unavailable" state while the completion itself stays intact,
  not disappear the whole memory).

## Backup and cloud-sync privacy (Step 10.7 finding + fix)

Before this audit, private media participated in each platform's default backup path despite the
in-app copy ("Stored privately with Togetherly on this device.") implying on-device-only storage:

- **Android**: `android:allowBackup="true"` with no exclusion rules means Auto Backup includes the
  *entire* app-private `filesDir` by default, `<filesDir>/media` included. Fixed by adding
  `androidApp/src/main/res/xml/backup_rules.xml` (`full-backup-content`, API 23–30) and
  `data_extraction_rules.xml` (`cloud-backup`/`device-transfer`, API 31+), both excluding
  `media/`, wired via `android:fullBackupContent`/`android:dataExtractionRules` in the manifest.
- **iOS**: `Application Support` (where `IosPrivateMediaRoot` stores media) is *not* automatically
  excluded from iCloud/iTunes backup the way `Library/Caches` is. Fixed by setting
  `NSURLIsExcludedFromBackupKey` on the media directory's `NSURL` right after creating it
  (`IosPrivateMediaRoot.rootPath()`).

With both fixes in place, the existing privacy copy is now actually true rather than merely implied.
Neither platform's Room/SQLite database file is addressed here — this document only covers the
`data.media` private-media root; see [persistence.md](persistence.md) for database backup
considerations if any are added later.

## What still leaves no trace

- No `MediaStore` insertion, no `getExternalFilesDir`, no shared/public storage path anywhere in
  `data.media`.
- No broad Android storage permission (`READ_EXTERNAL_STORAGE`/`READ_MEDIA_IMAGES`) — the system
  Photo Picker (`ActivityResultContracts.PickVisualMedia`) and iOS's `PHPickerViewController` both
  hand the picked item to the app without the app itself needing library-wide access, and neither
  requires a corresponding `Info.plist`/manifest permission entry (`iosApp/Info.plist` has no
  `NSPhotoLibraryUsageDescription` key, confirming this).
- `RECORD_AUDIO` (Android) / `NSMicrophoneUsageDescription` (iOS) are the only permissions this
  feature needs, requested only when the user taps Record — never at onboarding or app startup.

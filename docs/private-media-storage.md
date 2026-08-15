# Togetherly private media storage

This audit covers the app-private photo and voice memory lifecycle implemented under
`shared/src/androidMain/kotlin/com/togetherly/data/media/` and
`shared/src/iosMain/kotlin/com/togetherly/data/media/`.

## Storage boundary

- Media is stored under app-private storage only.
- Android keeps media under the app's private files directory, in a dedicated `media/` subtree.
- iOS keeps media under `<Application Support>/media`.
- Stored references remain relative paths only.
- Path traversal and absolute paths are rejected before any filesystem access.
- Filenames are generated from IDs, not family names, quest names, note content, or analytics fields.
- Pending filenames now fall back to numeric suffixes on collision.

## Backup and file protection

- Android release builds set `android:allowBackup="false"`. The backup XML remains in the tree as
  a defense-in-depth declaration, but the manifest itself disables cloud backup and device-to-device
  restoration for the app.
- Android backup rules explicitly exclude the app-private storage domains the app uses: `root`,
  `file`, `external`, `device_root`, and `device_file`, plus the Room database files
  `togetherly.db`, `togetherly.db-wal`, and `togetherly.db-shm` under both `database` and
  `device_database`.
- Togetherly does not use `SharedPreferences`, so that backup domain is not applicable in this
  release policy.
- iOS applies `NSFileProtectionCompleteUntilFirstUserAuthentication` to the Application Support
  storage tree used for the database and settings data, and `NSFileProtectionComplete` to private
  media.
- iOS excludes the Application Support tree from backup, so private media and database-backed
  family data do not participate in platform backup/transfer.

## Lifecycle coverage

- Capture/import: `AndroidPrivateMediaStorage.createPendingPhoto`
- Temporary files: pending photo files plus `.dims` sidecars
- Final storage: `commitPhoto` and `commitVoice`
- Metadata persistence: photo dimensions sidecar and committed photo/voice metadata
- Playback/display: `openPhoto` and `openPendingPhoto`
- Replacement: committed photo writes replace the target path deterministically
- Single-memory deletion: `deleteCommitted`
- Delete-all-memories: covered by the higher-level completion-memory cleanup tests
- Delete-all-local-data: covered by the higher-level local-data cleanup tests
- Interrupted operations: partial writes now clean up their own artifacts
- Orphan cleanup: `deleteExpiredPending`

## Behavioral guarantees

- Failed photo commits clean up partial committed files and leave the pending draft available for retry.
- Failed photo creation cleans up the pending file and its sidecar.
- Failed voice commits clean up partial committed files.
- Missing files return typed storage errors instead of crashing.
- Invalid references return validation errors instead of touching disk.
- `deleteExpiredPending` removes expired pending files and their sidecars.

## Tests

Covered by `shared/src/androidHostTest/kotlin/com/togetherly/data/media/AndroidPrivateMediaStorageTest.kt`:

- successful photo storage
- successful voice storage
- duplicate filenames
- interrupted write
- metadata failure after file creation
- missing file
- invalid path
- path traversal
- deleting one memory
- deleting all memories
- repeated deletion
- orphan cleanup

The real `MediaRecorder`/`MediaPlayer` flow is still covered separately by the instrumented Android device tests.

## Remaining manual verification

- Real camera picker integration
- Real microphone recording on device
- Real playback on device
- Storage behavior under low-disk conditions
- Device-level recovery after OS kill / process death

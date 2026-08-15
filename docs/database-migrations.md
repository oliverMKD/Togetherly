# Togetherly database migrations

Room schema export is enabled and tracked under `shared/schemas/com.togetherly.data.local.database.TogetherlyDatabase/`.

Current database version: 4.

## Table inventory

### Family profiles

- `family_profile`
- `family_age_band`
- `family_interest`
- `family_duration_preference`
- `family_energy_preference`
- `family_reminder_day`

### Daily selections

- `daily_quest`

### Dismissals

- `dismissed_quest`

### Saved quests

- `saved_quest`

### Active sessions

- `active_quest_session`

### Completions

- `quest_completion`
- `completion_reaction`
- `memory_media`

### Settings / metadata

- `database_metadata`

This table stores the schema bookkeeping row plus the telemetry consent cache and the entitlement cache.

## Migration history

- `1 → 2`: adds `allowPhotos`, `allowVoiceMemories`, `defaultSaveNote`, and `diagnosticsEnabled` to `family_profile`.
- `2 → 3`: adds `family_energy_preference`.
- `3 → 4`: drops `defaultSaveNote`, adds `allowTextNotes`, and adds `showMemoryPromptAfterQuests`.

There is no destructive-migration fallback in production. Missing migration paths fail loudly.

## Integrity rules

- Stable primary keys are preserved across upgrades.
- Optional fields stay nullable where the schema requires it.
- New non-null fields are backfilled with explicit defaults in the migration SQL.
- Foreign-key relationships from family and completion children are enforced by Room/SQLite.
- Media metadata stays in `memory_media`; the actual media files are separate filesystem state and are not rewritten by migration code.
- Interrupted migration is not silently repaired by deleting data.

## Test coverage

Current coverage in `TogetherlyDatabaseMigrationTest`:

- schema 1 can be created from the exported fixture
- `1 → 2` preserves an existing family and backfills settings defaults
- `2 → 3` preserves existing rows and creates an empty energy-preference table
- `3 → 4` preserves existing rows and backfills the new memory prompt columns
- schema 4 round-trips:
  - empty/null optional fields
  - active quest session
  - completion note
  - photo and voice metadata
  - telemetry consent cache
  - entitlement cache
  - invalid foreign-key inserts are rejected

## Verification status

The latest build/test run completed successfully for:

- `:shared:testAndroidHostTest`
- `:shared:compileKotlinIosSimulatorArm64`
- `:androidApp:assembleDebug`
- `:androidApp:compileReleaseKotlin`
- `:androidApp:lintDebug`

Android device-test compilation remains a separate pre-existing blocker in this repository outside the migration changes themselves; keep that in mind if you try to execute the instrumented migration test source set directly.

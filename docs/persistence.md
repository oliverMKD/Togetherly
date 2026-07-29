# Persistence

Togetherly's local storage is a single Room database (`TogetherlyDatabase`, package
`com.togetherly.data.local.database`), used identically on Android and iOS via Room's KMP
`BundledSQLiteDriver` (never the OS-provided SQLite). This document covers the database's tables,
who owns each one, how cross-table writes stay atomic, what a full data wipe actually deletes, the
migration policy, and how corrupted rows are handled. For the quest-catalogue content pipeline
(JSON-backed, not a database table), see [content-system.md](content-system.md). For the domain/DI
layering these repositories sit inside, see [architecture.md](architecture.md). For how Quest Mode's
timer relies on the `active_quest_session` row specifically, see [quest-mode.md](quest-mode.md).

## Tables and ownership

| Table | Entity | Owner | Notes |
|---|---|---|---|
| `database_metadata` | `DatabaseMetadataEntity` | schema/migration bookkeeping + the cached entitlement snapshot | Not family data — never touched by [FamilyDataCleaner](#full-family-data-deletion), except the one `entitlement_access_snapshot` key a full local-data wipe clears via `EntitlementCache.clear()` (see [local-data-deletion.md](local-data-deletion.md)). |
| `family_profile` | `FamilyProfileEntity` | `FamilyRepository` | Exactly one row (MVP: one family per device). |
| `family_age_band`, `family_interest`, `family_duration_preference`, `family_reminder_day` | — | `FamilyRepository` | FK `CASCADE` to `family_profile`; replaced wholesale on every profile save. |
| `daily_quest` | `DailyQuestEntity` | `DailyQuestRepository` | One row per `localDate`; `questId` is a plain string (see [Catalogue IDs](#catalogue-ids-have-no-database-foreign-key)). |
| `dismissed_quest` | `DismissedQuestEntity` | `DailyQuestRepository` | Append-only history; `questId` likewise has no FK. |
| `saved_quest` | `SavedQuestEntity` | `SavedQuestRepository` | `questId` likewise has no FK. |
| `active_quest_session` | `ActiveQuestSessionEntity` | `CompletionRepository` / `QuestSessionTransaction` | Singleton row (`slotId = 0`, MVP supports one active session). `startedAt` is the sole input to Quest Mode's timer-deadline math (`startedAt + quest.timer.duration`) — this row alone is what lets a timer recover correctly after process death, with no extra write path required. See [quest-mode.md](quest-mode.md#process-death-recovery). |
| `quest_completion` | `QuestCompletionEntity` | `CompletionRepository` | A family memory: `note` is private, never logged. |
| `completion_reaction`, `memory_media` | — | `CompletionRepository` | FK `CASCADE` to `quest_completion`. `memory_media.localReference` is an app-controlled reference, never bytes, a URI, or a public URL — see [Media metadata versus files](#media-metadata-versus-media-files). |

No table stores the quest catalogue itself — quest content is JSON, loaded and parsed by
`BundledQuestRepository`. There is no `quest` table to have a foreign key to.

## Family settings architecture

`FamilySettings` (`domain/family/FamilySettings.kt`) is everything a parent-facing settings screen
needs, composed from **one** underlying storage row — `profile: FamilyProfile`,
`questPreferences: QuestPreferences`, `reminderPreference: ReminderPreference?`,
`memoryPreferences: MemoryPreferences`, `privacyPreferences: PrivacyPreferences` — never a second
source of truth for the profile fields also exposed through `FamilyRepository` directly.
`FamilySettingsRepository` (`RoomFamilySettingsRepository`) is the one repository that reads/writes
all five together; each has its own narrow update use case (`UpdateQuestPreferences`,
`UpdateReminderPreference`, `UpdateMemoryPreferences`, `UpdatePrivacyPreferences`) and its own
parent-facing editor screen reached from the Family tab (Quest Preferences, Reminder, Memory
Settings, Privacy — plus the profile editor itself, which goes through `FamilyRepository`/
`UpdateFamilyProfile` instead, not `FamilySettingsRepository`). `ObserveFamilySettings` is the one
read path every settings screen's own `ViewModel` uses instead of composing the four repository
reads itself.

At the table level, every one of these fields lives on `family_profile` and its FK-cascaded
preference child tables (see [Tables and ownership](#tables-and-ownership) above) — there is no
separate table per preference category. See [reminders.md](reminders.md) for reminder scheduling
specifically, [memory-flow.md](memory-flow.md#memory-settings-step-135) for memory preferences, and
[privacy.md](privacy.md) for the privacy summary these settings feed.

## Transactional boundaries

Every multi-table or check-then-write operation runs inside one Room transaction, via
`TogetherlyDatabase.useWriterConnection { transactor -> transactor.immediateTransaction { ... } }`
— **not** `RoomDatabase.withTransaction`, which is part of the classic Android/JVM-only `room-ktx`
extension and does not exist on the KMP-common API surface (confirmed by inspecting Room
2.8.4's published Kotlin metadata directly). SQLite allows exactly one writer at a time, and
Room's writer connection serializes every `immediateTransaction` block onto it — two concurrent
callers never interleave their statements; the second one's transaction genuinely doesn't start
until the first has fully committed or rolled back.

- **`RoomFamilyRepository.saveProfile`** — replaces the profile row and all four preference child
  tables together (`FamilyDao.replaceFamilyProfile`, a `@Transaction` DAO method with a body).
- **`RoomCompletionRepository.saveCompletion`** — replaces a completion's primary row, then
  unconditionally re-derives its reaction and media rows from scratch
  (`CompletionDao.replaceCompletion`), so the stored set always exactly mirrors what was supplied.
- **`QuestSessionTransaction`** (`RoomQuestSessionTransaction`) — the atomic check-and-write
  boundary for the active-session lifecycle:
  - `startActiveSession`: checks whether a session is already active and inserts inside the same
    transaction. Succeeds when none exists; succeeds without writing again when the active
    session already has the same `completionId` (a retried start is idempotent); returns a typed
    `ACTIVE_SESSION_CONFLICT` when a *different* session is active. Two concurrent starts can
    never both "win" — SQLite's single-writer serialization guarantees exactly one insert and one
    conflict.
  - `completeActiveSession`: re-verifies the active session still matches the completion being
    saved (by `completionId`), then saves the completion and clears the active session as one
    step. Both commit or neither does. If the active session changed since the caller last read
    it (or was cleared), this returns a typed `ACTIVE_SESSION_MISMATCH` instead of completing a
    session the caller no longer has an accurate view of.
  - `StartQuest`/`CompleteQuest` (the use cases) never do a plain repository read followed by a
    later, separate write for the active-session state — that would leave a real race window. The
    only read-then-write gap that exists is between `CompleteQuest`'s own initial
    `getActiveSession()` read (needed to build the `QuestCompletion`) and its eventual
    `completeActiveSession` call; the mismatch check inside that transaction is exactly what
    closes that gap safely, rather than pretending it doesn't exist.
- **`FamilyDataCleaner`** (`RoomFamilyDataCleaner`), **`QuestHistoryCleaner`** (`RoomQuestHistoryCleaner`)
  and **`MemoryCleaner`** (`RoomMemoryCleaner`) — see below and
  [local-data-deletion.md](local-data-deletion.md); every delete each performs runs in one
  transaction.

## Three tiers of database deletion

Three distinct, deliberately non-overlapping contracts exist for clearing database rows, from
narrowest to broadest — see [local-data-deletion.md](local-data-deletion.md) for the full
parent-facing behavior each backs, including the file-deletion and RevenueCat-cache steps that sit
above these at the use-case layer:

- **`MemoryCleaner.clearAllMemoryContent()`** (`RoomMemoryCleaner`) — clears every completion's
  `note` and deletes every `memory_media` row, in one transaction. Never touches the
  `quest_completion` row itself or `completion_reaction` — a completion (and a family's reactions
  to it) survives even when its memory content is cleared, since they are not the same thing in
  this schema (see [Media metadata versus media files](#media-metadata-versus-media-files)).
- **`QuestHistoryCleaner.resetQuestHistory()`** (`RoomQuestHistoryCleaner`) — deletes `daily_quest`,
  `dismissed_quest`, the `active_quest_session` row, and every `quest_completion` (cascading its
  own `completion_reaction`/`memory_media` rows), in one transaction. Never touches `family_profile`,
  its preference tables, or `saved_quest`.
- **`FamilyDataCleaner.deleteAllFamilyData()`** (`RoomFamilyDataCleaner`) — the superset: everything
  `QuestHistoryCleaner` deletes, plus the family profile (cascading its four preference tables) and
  every saved quest, all in one transaction. Reached only through the `DeleteAllFamilyData` use
  case, never called directly from presentation code and never hidden inside `FamilyRepository`
  (`FamilyRepository.deleteProfile()` remains the separate, narrower contract that only ever
  deletes the profile and its preference rows).

None of the three ever deletes:

- **The bundled quest catalogue** — not a database table at all (JSON-backed).
- **`database_metadata`'s schema/migration bookkeeping rows** — not family data. (The one exception,
  the cached entitlement snapshot, is cleared separately by `EntitlementCache.clear()` — see
  [local-data-deletion.md](local-data-deletion.md).)

Each is idempotent: calling it again with nothing left to delete still succeeds (every underlying
`DELETE` is a no-op on an empty table, never an error).

## Media metadata versus media files

`memory_media` stores only metadata: an app-controlled `localReference` string, a `type`
(`photo`/`voice`), and a voice `durationMillis`. It never stores photo bytes, voice bytes, a
public URL, or a platform content URI. Deleting the actual file a `localReference` points to is a
separate step from deleting its metadata row — `PrivateMediaCommitter.deleteCommitted` (validated
against `PrivateMediaPaths.isSafeRelativeReference` first) is that step, and it is always run
*after* the metadata row is gone, never before (see
[local-data-deletion.md](local-data-deletion.md#ordering-database-first-files-best-effort-after)
for why). None of the three cleaners above ever calls it themselves — they only ever remove
*metadata* rows (via `DELETE`/FK `CASCADE`). Every call site that also needs the files gone
(`DeleteCompletion`, `DeleteMemories`, `ResetQuestHistory`, `DeleteAllLocalData`) reads the
affected `MemoryMedia` *before* invoking the cleaner, then deletes each file itself afterward —
neither a cleaner nor a `CompletionRepository` method reaches into file storage directly.

## Catalogue IDs have no database foreign key

`daily_quest.questId`, `dismissed_quest.questId`, and `saved_quest.questId` are plain string
columns with no `foreignKeys` declaration. This is deliberate, not an oversight: the quest
catalogue is not a database table (see [content-system.md](content-system.md)), so there is
nothing to declare a foreign key *to* — doing so would mean fabricating a fake catalogue table
just to satisfy Room's FK syntax. A `questId` that matches nothing in the current catalogue is
valid storage; resolving it against the catalogue (and deciding what to show when it resolves to
nothing) is a read-time concern for whichever use case joins the two, not a write-time constraint
in the schema. `RoomJourneyRepository` is the concrete example: it joins completions to catalogue
content in memory, and a completion whose `questId` no longer resolves keeps `JourneyEntry.quest =
null` rather than being rejected at write time or hidden at read time.

## Migration policy

`TogetherlyDatabase` is version 4. Schema export is enabled
(`room { schemaDirectory("$projectDir/schemas") }`) and every exported version is committed at
`shared/schemas/com.togetherly.data.local.database.TogetherlyDatabase/{1,2,3,4}.json`. No
destructive-migration fallback is configured anywhere — a missing migration path throws loudly
instead of silently deleting a family's data. `Migrations.kt` holds `MIGRATION_1_2` (adds Family
Settings columns), `MIGRATION_2_3` (adds `family_energy_preference`), and `MIGRATION_3_4` (drops
`defaultSaveNote`, adds `allowTextNotes`/`showMemoryPromptAfterQuests`) — none of Step 13.7's own
deletion additions needed a schema migration, since every new deletion contract only issues
`DELETE`/`UPDATE` statements against tables that already exist.

**The moment any build has been installed by a real external tester or user, the current schema
version becomes immutable.** Any further change to an existing table's columns/indices, or removal
of a table, is the next Room `Migration`, never an in-place edit to already-shipped entities.

Migration-test scaffolding exists and is exercised today: `TogetherlyDatabaseMigrationTest` uses
`androidx.room.testing.MigrationTestHelper`'s driver-based constructor (required because this
project uses `BundledSQLiteDriver`, not the legacy `SupportSQLiteOpenHelper`-based API) to open
each exported schema version and validate the migration chain against Room's live generated
schema. See that test's own KDoc for the exact steps to extend it the day a version 5 migration is
needed.

## Corruption policy

- **Invalid row mapping returns a typed corrupted-storage error** — every entity→domain mapper
  (`com.togetherly.data.local.mapper`) wraps domain-object construction in `mapStorageCatching`,
  converting a `DomainValidationException` into `AppError.Storage(StorageError.DATA_CORRUPTED)`.
  An unrecognized stored enum/type key (`com.togetherly.data.local.keys`) is likewise never
  defaulted — every `when` branch that doesn't match returns `null`, converted to the same typed
  corrupted-storage error one level up.
- **Corrupt memories are never silently deleted.** No mapper, repository, or DAO method deletes a
  row because it failed to parse — a corrupted row stays exactly where it is, and the read that
  encountered it returns a typed error instead of pretending the row doesn't exist.
- **Unknown enum values are never silently substituted** with a default/fallback value — see
  above; the only two outcomes are "parsed successfully" or "typed error", never a guessed value.
- **The database is never automatically reset.** Confirmed by omission: `buildTogetherlyDatabase`
  never calls `fallbackToDestructiveMigration()`/`fallbackToDestructiveMigrationOnDowngrade()`, on
  any platform.
- **A future recovery seam, not a recovery implementation.** Nothing here offers a user-facing
  "reset corrupted data" flow, and none should be inferred from this policy. `database_metadata`
  (`DatabaseMetadataDao`) already exists as a small, tested, general-purpose key/value table and is
  the natural place a future recovery flow could record e.g. when corruption was last detected, or
  a flag a dedicated recovery use case could check — without any such logic existing yet. When that
  flow is eventually built, it should still go through an explicit, typed use case a user
  consciously triggers, the same pattern `FamilyDataCleaner`/`DeleteAllFamilyData` already
  establishes for other destructive operations — never an automatic, implicit reset triggered by a
  read failure.

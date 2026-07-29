# Open-source licenses (Step 13.6)

Togetherly shows an in-app "Open-source licenses" screen
(`feature/family/presentation/OpenSourceLicensesScreen.kt`), reached from the Family tab's Legal
destination. Its content comes from `feature/family/model/OpenSourceLicense.kt`'s
`OPEN_SOURCE_LICENSES` — a hand-curated, offline, static list, not a generated Compose resource.

## Why hand-curated instead of an automatic generator plugin

This project's own established precedent (see `docs/reminders.md` — no cross-platform notification
library was added when the platform frameworks were simple enough to call directly) is to avoid a
new third-party build dependency when a direct alternative is simple and reliable. An automatic
license-generation Gradle plugin (e.g. `app.cash.licensee`) is KMP-compatible and would be a
reasonable choice if this list needs to scale significantly, but was not added here — instead:

- The list itself is a plain, offline Kotlin `List` (works with no network access, matching the
  in-app notice's own "available offline" requirement).
- A real, documented Gradle task regenerates the *comparison data* (see below), so the list can be
  verified against what Gradle actually resolves rather than drifting silently.

## Regenerating / verifying the list

Run:

```
./gradlew :shared:printResolvedRuntimeDependencies
```

This prints every resolved `group:name:version` coordinate on `androidRuntimeClasspath` (Android's
resolved runtime classpath is used as the representative set — this module declares no
Android-only dependencies, so it matches what commonMain pulls in too). Compare the output against
`OPEN_SOURCE_LICENSES` in `feature/family/model/OpenSourceLicense.kt`:

- A new coordinate in the task output that isn't represented in `OPEN_SOURCE_LICENSES` (grouped by
  its owning library where one dependency pulls in several artifacts, e.g. RevenueCat's Android SDK
  transitively pulling in OkHttp/Okio/Coil/commonmark) means the curated list needs a new entry.
- A coordinate that disappears means an entry can be removed.

Test-only dependencies (Turbine, `kotlin-test`, `androidx.test.*`) are deliberately excluded from
the curated list, since they never ship inside the built app.

## Notices offline

`OPEN_SOURCE_LICENSES` ships as compiled Kotlin code inside the app itself — the licenses screen
needs no network access to render, satisfying "ensure notices are available offline."

# Content System

Togetherly's quest catalogue is bundled JSON, shipped inside the app, loaded through a strict
parse → validate → map pipeline, and exposed to the rest of the app only as domain models. This
document describes that system: where the file lives, its schema, how to add content safely, and
how validation and versioning work.

## Catalogue file location

```
shared/src/commonMain/composeResources/files/content/quest-catalogue-en-v1.json
```

It's a Compose Multiplatform resource under `files/`, which means it's read generically via
`Res.readBytes("files/content/quest-catalogue-en-v1.json")` rather than through a generated
per-file accessor. The exact path string is declared once, as
`QUEST_CATALOGUE_RESOURCE_PATH` in `content/resource/QuestCatalogueResource.kt` — nothing else in
the codebase should hardcode it.

Never place catalogue JSON under a Kotlin source directory (`commonMain/kotlin/...`) — it must
live under `composeResources` so it's packaged as a resource, not compiled as code.

## Pipeline

```
Bundled JSON resource
  → QuestCatalogueResource        (content.resource)   reads raw bytes → UTF-8 text
  → QuestCatalogueParser          (content.schema)      strict kotlinx.serialization decode → DTOs
  → QuestCatalogueValidator       (content.validation)  full structural + cross-reference pass
  → QuestCatalogueMapper          (content.mapper)      DTO → domain models (only reached if valid)
  → QuestCatalogueLoader          (content.loader)      orchestrates the above, caches success
  → BundledQuestRepository        (content.repository)  implements domain QuestRepository
  → domain use cases              (domain.*.usecase)    never see a DTO or JSON value
```

Every layer's types are `internal` to the `shared` module except the final
`domain.quest.repository.QuestRepository` interface and the `FamilyQuest`/`QuestPack` domain
models it returns. No DTO, no `kotlinx.serialization.json.Json`, and no generated Compose
Resources API (`Res`, `MissingResourceException`, ...) is reachable from `domain.*` code — the
content boundary is one-way.

Validation always runs before mapping, and a validation failure is rejected before mapping is
ever attempted — there is no code path in production that maps an unvalidated DTO.

## Schema structure

The root object:

| Field | Type | Notes |
|---|---|---|
| `schemaVersion` | Int | Must equal `QuestCatalogueSchema.CURRENT_SCHEMA_VERSION` (currently `1`) |
| `catalogueVersion` | Int | Positive; content authors bump this per content change |
| `locale` | String | `^[a-z]{2}(-[A-Z]{2})?$`, e.g. `"en"` |
| `packs` | `QuestPackDto[]` | Non-empty |
| `quests` | `FamilyQuestDto[]` | Non-empty |

A pack (`QuestPackDto`): `id`, `version`, `title`, `description`, `category` (nullable — `null`
means a mixed-category pack), `access` (`{ "type": "free" | "premium", "entitlementId"? }`),
`questIds`, `artworkKey`, `sortOrder`.

A quest (`FamilyQuestDto`): `id`, `version`, `title`, `summary`, `instructions`
(`{ "order", "text" }[]`, 1-indexed, no gaps or duplicates), `category`, `ageBands`
(`"6-8" | "9-11" | "12-13"`), `durationMinutes`, `location` (`"indoor" | "outdoor" | "either"`),
`preparation` (`"none" | "simple-materials" | "advanced"`), `energy`
(`"calm" | "moderate" | "active"`), `materials` (default `[]`), `hints` (default `[]`, 0-2),
`completionPrompt`, `safetyNote` (optional), `packId`, `access`, `timer`
(optional, `{ "durationSeconds", "keepScreenOn" }`), `cooldownDays` (default `30`).

`category` values: `talk`, `create`, `move`, `kindness`, `discover`, `silly`, `memories`. All enum
strings are lowercase and hyphenated exactly as shown — never a Kotlin enum name like `DISCOVER`
or `AGE_6_TO_8`. Enum values are matched with explicit `when` blocks, never `enumValueOf()`, so an
unexpected value is always a reported schema violation, not a silent crash.

## How to add a quest

1. Pick a stable, lowercase, kebab-case `id` prefixed by its category, e.g. `talk-my-new-quest`.
   IDs must never encode display copy or list position (no `quest-01`), and must never change once
   shipped — titles can be edited freely without breaking references.
2. Fill in every required field from the table above. Keep instructions to 2-5 steps, each
   understandable on first read.
3. Pick `ageBands` deliberately — a quest is only recommended to a family when it supports *every*
   age band present in that family, so an unnecessarily narrow set shrinks how often a quest can
   be selected.
4. Add `"packId": "<target-pack-id>"` (e.g. `quick-wins`, `everyday-together`, or one of the
   Family Plus packs — see [Explore](explore.md) for the current pack list) and list the quest's
   ID in that pack's `questIds`.
5. Run the content-safety checklist below before committing.
6. Bump `catalogueVersion` (see Versioning).
7. Run the validation commands below.

## How to add a pack

1. Pick a stable, lowercase, kebab-case `id`.
2. Set `category` to a specific category only if every quest in the pack shares it — otherwise
   leave it `null` (a mixed pack — this is the norm; every bundled pack today is mixed-category).
3. `artworkKey` must start with `"packs/"` — this is the one enforced naming convention
   (`CrossReferenceValidation.kt`); there's no broader asset-naming scheme defined yet.
4. List every quest ID that belongs to the pack. Each quest must also declare this pack's ID in
   its own `packId` — a mismatch either way is a validation error, not a warning.
5. Set `sortOrder`; packs are returned sorted by it.

## ID conventions

- Lowercase kebab-case, category-prefixed (`talk-reverse-interview`).
- Stable forever once shipped — never renamed, even if the title changes.
- No list positions (`quest-01`) or display copy embedded in the ID.
- No duplicate IDs, checked catalogue-wide for both quests and packs.
- No production ID may start with `test` or `sample` — reserved for fixtures.

## Access rules

Every quest and pack declares `access: { type, entitlementId? }`.

- `"free"` access must **not** carry an `entitlementId`.
- `"premium"` access **must** carry a non-blank `entitlementId` — the mapper uses whatever ID is
  given verbatim; it never hardcodes a specific entitlement like `"family_plus"`.
- A `"free"` pack containing a `"premium"` quest is a validation error
  (`CONTRADICTORY_STATE`), not a warning — free packs must only ever contain free quests.
- Premium packs remain visible (never hidden or excluded from results) to free families — only the
  individual quests/packs are marked `locked`, computed at presentation time via
  `QuestAccessPolicy`. The catalogue itself never decides who can unlock what; RevenueCat
  entitlements do. See [Explore](explore.md) for how locking is evaluated and surfaced.

## Content-safety checklist

Before adding or changing a quest, confirm it:

- Never involves fire, sharp tools, traffic, unsupervised cooking, deep water (beyond a bowl/sink),
  climbing furniture, food tasting with unknown allergens, blindfolded movement, leaving children
  unsupervised outdoors, contacting strangers, publishing photos, or sharing private information.
- Requires no purchase and no specialized equipment; materials (if any) are common household items.
- Doesn't force personal disclosure, and avoids embarrassment, humiliation, winners/losers, or
  competitive framing where practical.
- Works for a single parent and a single child — never assumes two parents, a specific gender, or
  more than one child.
- Avoids religious or culture-specific assumptions.
- Reads as understandable on first read, startable within about two minutes, and not like
  homework or a chore.
- Add a `safetyNote` only when genuinely contextual (e.g. clearing a dance floor, using a bowl
  instead of a bathtub) — never as boilerplate repeated on every quest.

## Validation commands

```
./gradlew allTests
./gradlew build
./gradlew :androidApp:assembleDebug
```

`QuestCatalogueValidator` collects every problem in one pass rather than stopping at the first —
run these after every content change and read the *entire* report, not just the first failure.
Errors (`CatalogueIssueSeverity.ERROR`) block loading; warnings (`SUSPICIOUS_CONTENT`, duplicate
pack `sortOrder`) don't, but should still be reviewed, not reflexively silenced.

## Catalogue versioning

Two independent counters, both in the JSON root:

- **`schemaVersion`** changes only when the *structure* of the JSON changes — a field added,
  removed, renamed, or its meaning changed. `QuestCatalogueJson` is strict
  (`ignoreUnknownKeys = false`), so any schema drift between the file and the DTOs fails parsing
  loudly rather than silently dropping data. Bumping this is a breaking pipeline change: DTOs,
  mappers and possibly the validator all need matching updates.
- **`catalogueVersion`** changes whenever *content* changes — a new quest, an edited instruction, a
  reworded title — with the JSON structure itself untouched. This is the counter content authors
  bump on every content pull request.

Rule of thumb: if you touched `content/model/*.kt`, that's a schema change. If you only touched
the JSON file's quest/pack content, that's a catalogue version change.

## Current limitations

- **One pack per quest.** Every quest declares exactly one `packId`, and every pack's `questIds`
  must agree with it — a quest cannot currently belong to more than one pack. If multi-pack
  membership becomes a real product need, this is a deliberate constraint to revisit, not an
  oversight (see `CrossReferenceValidation.kt`'s own KDoc).
- **English only, one bundled file.** There is no locale-switching or remote content mechanism yet
  (deliberately out of scope through at least Step 5.6).
- **45 quests across 6 packs (21 free, 24 Family Plus), no seasonal packs, no AI-generated
  content.** See [Explore](explore.md) for the current pack-by-pack breakdown. Production-scale
  content beyond this is future work.

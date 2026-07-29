# Deterministic Quest Recommendation Engine

`com.togetherly.domain.recommendation` (`shared/src/commonMain`) is a pure, synchronous, fully
deterministic scoring/filtering policy for choosing today's quest. It does no I/O, no AI, no
networking, no analytics, and never reads the system clock or any platform-specific hashing —
every time-dependent or random-seeming input is supplied explicitly by the caller, so the same
`QuestRecommendationRequest` always produces the same `QuestRecommendationResult`, on any platform.

## Filtering pipeline

`DeterministicQuestRecommendationPolicy.recommend` runs an ordered hard-filter pipeline, each stage
short-circuiting to a distinct `NoRecommendationReason` if it empties the candidate set:

1. **Empty catalogue** → `EMPTY_CATALOGUE` if `availableQuests` is empty.
2. **Age-band eligibility** (`FamilyQuest.supports`) → `NO_AGE_COMPATIBLE_QUEST` unless a quest
   supports *every* age band the family has children in (a superset check, not "any overlap").
3. **Context and preparation filters** → `NO_CONTEXT_COMPATIBLE_QUEST`. Duration, location, energy
   and category are hard-excluded *only when the request's `QuestContext` explicitly sets them* —
   an absent filter never excludes anything. Preparation is the one exception: it is **always**
   hard-filtered, against an explicit context value if given, or otherwise against the family
   profile's own `PreparationPreference` ceiling (`NONE`→`NONE`, `SIMPLE_MATERIALS`→`SIMPLE_MATERIALS`,
   `ANY`→`ADVANCED`) — a family never sees a quest above what they've said they're willing to
   prepare for, even with no explicit context filter active for today.
4. **Dismissal/cooldown exclusion** → `ALL_CANDIDATES_IN_COOLDOWN`. A quest is excluded if it was
   dismissed within `RecommendationConfig.dismissalCooldown` (default 30 days), or if its own
   `cooldownDays` hasn't elapsed since its most recent completion (`cooldownDays <= 0` means never
   excluded on completion recency).

Whatever survives all four stages is scored and the highest-scoring quest wins (see tie-breaking
below). Input list order never affects the result — every stage is order-independent, and the
final selection sorts by score, deterministic tie-break key, then quest ID.

## Score configuration

`RecommendationConfig` is the single source of truth for every weight — nothing is hardcoded
inline in the policy:

| Reason | Default weight |
|---|---|
| `MatchesFamilyInterest` | +30 |
| `MatchesPreferredDuration` | +25 |
| `AddsCategoryVariety` | +20 |
| `NewToFamily` | +15 |
| `MatchesLocationPreference` | +10 |
| `MatchesPreparationPreference` | +10 |
| `MatchesPreferredEnergy` | +10 |
| `MatchesRequestedContext` | +10 |
| recent-category-repeat penalty | −15 |

`MatchesPreferredEnergy` (Step 13.3) scores the same way as `MatchesLocationPreference` — a normal,
non-explicit family preference bonus, only when `FamilyProfile.preferredEnergyLevels` is non-empty
and contains the quest's own `EnergyLevel`. It is **never** a hard filter (unlike preparation,
which is always a ceiling): an empty preference set simply never contributes the bonus, and a
non-matching preference never excludes a quest — preferences influence ranking, they never make a
recommendation impossible or permanently hide content from Explore.

`AddsCategoryVariety` is true when a category is empty in `categoryCompletionCounts` or below the
average completion count across all seven categories — this is what keeps a family's quest history
from converging on one or two categories over time. Every candidate's final `score` equals the sum
of the weights for whatever `reasons` it earned; this is asserted directly in
`DeterministicQuestRecommendationPolicyTest`.

## Deterministic tie-breaking

Equal-scoring candidates are broken by an FNV-1a 64-bit hash of
`"$familyId|$localDate|$selectionIndex|$questId"` (`StableHash.kt`), never `String.hashCode()` or
any other JVM/Native-divergent hash — `String.hashCode()`'s value is unspecified across Kotlin
platforms and would silently produce a different "random" winner on Android vs. iOS for the same
input. If two candidates still tie (astronomically unlikely with a 64-bit hash, but handled
explicitly), the quest ID's own string ordering is the final, fully deterministic tiebreaker.

## Cooldown behavior

`RecommendationConfig.dismissalCooldown` (a `kotlin.time.Duration`, default 30 days) is read in
exactly two places, and only two: the policy's own dismissal-window check, and
`RecommendationHistoryBuilder`'s `DailyQuestRepository.getRecentDismissals(since = now - dismissalCooldown)`
query. There is no second, independently-drifting window anywhere else — changing the config
changes both consistently. Per-quest `cooldownDays` is a separate, catalogue-authored value checked
against completion recency, not dismissal recency.

## No-match behavior

`NoRecommendationReason` has four values (`EMPTY_CATALOGUE`, `NO_AGE_COMPATIBLE_QUEST`,
`NO_CONTEXT_COMPATIBLE_QUEST`, `ALL_CANDIDATES_IN_COOLDOWN`), each mapped onto a distinct
`ContentError` by `NoRecommendationReasonMapper.toAppError()` — shared by every daily-selection use
case (`GetOrSelectDailyQuest`, `SelectDailyQuestForContext`, `RerollDailyQuest`) so a given failure
reason always produces the same typed error regardless of which use case hit it. Presentation-layer
copy per reason (Step 8.5's "adjust filters" / "clear filters" / "keep current quest" messaging)
lives in `com.togetherly.feature.today.mapper.TodayErrorMapper`, deliberately *not* in the shared
generic `AppError.toUiText()` mapper — see that file's own KDoc for why.

## Tests

`DeterministicQuestRecommendationPolicyTest` (30 tests) and `StableHashTest` (3 tests) cover: every
filter stage individually and in combination, every scoring reason and its weight, tie-breaking
determinism (including an explicit "same fixture always produces the same quest ID" assertion,
input-order independence, and a fixed known-input hash value), and every `NoRecommendationReason`.

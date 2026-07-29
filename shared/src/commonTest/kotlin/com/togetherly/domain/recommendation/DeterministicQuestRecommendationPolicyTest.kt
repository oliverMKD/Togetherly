package com.togetherly.domain.recommendation

import com.togetherly.domain.daily.QuestContext
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.family.FamilyProfile
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.PreparationLevel
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestLocation
import com.togetherly.domain.quest.validFamilyQuest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

private val FAMILY_ID = FamilyId("family-1")
private val NOW = Instant.parse("2026-06-15T08:00:00Z")
private val TODAY = LocalDate(2026, 6, 15)
private val EMPTY_CONTEXT = QuestContext(null, null, null, null, null)

/**
 * [interests]/[preferredDurations] default to a category/band that [validFamilyQuest]'s own
 * defaults never use (`DISCOVER`/20 minutes), so tests isolating a *different* scoring dimension
 * don't accidentally also trigger the interest/duration bonus — [FamilyProfile] itself requires
 * both non-empty, so `emptySet()` was never a valid "neutral" default.
 */
private fun testProfile(
    childAgeBands: Set<AgeBand> = setOf(AgeBand.AGE_6_TO_8),
    interests: Set<QuestCategory> = setOf(QuestCategory.MEMORIES),
    preferredDurations: Set<DurationBand> = setOf(DurationBand.FIVE_MINUTES),
    locationPreference: LocationPreference = LocationPreference.BOTH,
    preparationPreference: PreparationPreference = PreparationPreference.ANY,
    preferredEnergyLevels: Set<EnergyLevel> = emptySet(),
) = FamilyProfile(
    id = FAMILY_ID,
    displayName = null,
    childAgeBands = childAgeBands,
    interests = interests,
    preferredDurations = preferredDurations,
    locationPreference = locationPreference,
    preparationPreference = preparationPreference,
    preferredEnergyLevels = preferredEnergyLevels,
    reminderPreference = null,
    createdAt = NOW,
    updatedAt = NOW,
)

private fun request(
    familyProfile: FamilyProfile = testProfile(),
    context: QuestContext = EMPTY_CONTEXT,
    availableQuests: List<FamilyQuest>,
    history: RecommendationHistory = RecommendationHistory.EMPTY,
    localDate: LocalDate = TODAY,
    selectionIndex: Int = 0,
    now: Instant = NOW,
) = QuestRecommendationRequest(familyProfile, context, availableQuests, history, localDate, selectionIndex, now)

private fun weightFor(reason: RecommendationReason, config: RecommendationConfig = RecommendationConfig.DEFAULT): Int =
    when (reason) {
        RecommendationReason.MatchesFamilyInterest -> config.matchesFamilyInterest
        RecommendationReason.MatchesPreferredDuration -> config.matchesPreferredDuration
        RecommendationReason.MatchesRequestedContext -> config.matchesRequestedContext
        RecommendationReason.NewToFamily -> config.newToFamily
        RecommendationReason.AddsCategoryVariety -> config.addsCategoryVariety
        RecommendationReason.MatchesLocationPreference -> config.matchesLocationPreference
        RecommendationReason.MatchesPreparationPreference -> config.matchesPreparationPreference
        RecommendationReason.MatchesPreferredEnergy -> config.matchesPreferredEnergy
        RecommendationReason.LeastRecentlyCompleted -> 0
    }

class DeterministicQuestRecommendationPolicyTest {

    private val policy = DeterministicQuestRecommendationPolicy()

    // -- Filtering ----------------------------------------------------------------------------

    @Test
    fun emptyCatalogueReturnsNoMatch() {
        val result = policy.recommend(request(availableQuests = emptyList()))

        assertEquals(QuestRecommendationResult.NoMatch(NoRecommendationReason.EMPTY_CATALOGUE), result)
    }

    @Test
    fun ageConstraintsAreNeverRelaxedEvenWhenNothingQualifies() {
        val tooOld = validFamilyQuest(ageBands = setOf(AgeBand.AGE_12_TO_13))

        val result = policy.recommend(
            request(
                familyProfile = testProfile(childAgeBands = setOf(AgeBand.AGE_6_TO_8)),
                availableQuests = listOf(tooOld),
            ),
        )

        assertEquals(QuestRecommendationResult.NoMatch(NoRecommendationReason.NO_AGE_COMPATIBLE_QUEST), result)
    }

    @Test
    fun questMustSupportEveryConfiguredAgeBandNotJustOne() {
        val partialMatch = validFamilyQuest(id = QuestId("quest-1"), ageBands = setOf(AgeBand.AGE_6_TO_8))
        val fullMatch = validFamilyQuest(
            id = QuestId("quest-2"),
            ageBands = setOf(AgeBand.AGE_6_TO_8, AgeBand.AGE_9_TO_11),
        )

        val result = policy.recommend(
            request(
                familyProfile = testProfile(childAgeBands = setOf(AgeBand.AGE_6_TO_8, AgeBand.AGE_9_TO_11)),
                availableQuests = listOf(partialMatch, fullMatch),
            ),
        ) as QuestRecommendationResult.Success

        assertEquals(fullMatch.id, result.quest.id)
    }

    @Test
    fun explicitDurationExcludesNonMatchingQuests() {
        val fiveMinutes = validFamilyQuest(id = QuestId("q1"), durationMinutes = 5)
        val twentyMinutes = validFamilyQuest(id = QuestId("q2"), durationMinutes = 20)

        val result = policy.recommend(
            request(
                context = EMPTY_CONTEXT.copy(duration = DurationBand.TWENTY_MINUTES),
                availableQuests = listOf(fiveMinutes, twentyMinutes),
            ),
        ) as QuestRecommendationResult.Success

        assertEquals(twentyMinutes.id, result.quest.id)
    }

    @Test
    fun explicitIndoorAllowsEitherButExcludesOutdoor() {
        val indoor = validFamilyQuest(id = QuestId("q1"), location = QuestLocation.INDOOR)
        val outdoor = validFamilyQuest(id = QuestId("q2"), location = QuestLocation.OUTDOOR)
        val either = validFamilyQuest(id = QuestId("q3"), location = QuestLocation.EITHER)

        val result = policy.recommend(
            request(
                context = EMPTY_CONTEXT.copy(location = QuestLocation.INDOOR),
                availableQuests = listOf(indoor, outdoor, either),
            ),
        ) as QuestRecommendationResult.Success

        assertTrue(result.quest.id in setOf(indoor.id, either.id))
    }

    @Test
    fun explicitOutdoorExcludesIndoor() {
        val indoor = validFamilyQuest(id = QuestId("q1"), location = QuestLocation.INDOOR)
        val outdoor = validFamilyQuest(id = QuestId("q2"), location = QuestLocation.OUTDOOR)

        val result = policy.recommend(
            request(
                context = EMPTY_CONTEXT.copy(location = QuestLocation.OUTDOOR),
                availableQuests = listOf(indoor, outdoor),
            ),
        ) as QuestRecommendationResult.Success

        assertEquals(outdoor.id, result.quest.id)
    }

    @Test
    fun explicitPreparationIsTreatedAsAnUpperLimit() {
        val none = validFamilyQuest(id = QuestId("q1"), preparation = PreparationLevel.NONE)
        val advanced = validFamilyQuest(id = QuestId("q2"), preparation = PreparationLevel.ADVANCED)

        val result = policy.recommend(
            request(
                context = EMPTY_CONTEXT.copy(preparation = PreparationLevel.NONE),
                availableQuests = listOf(none, advanced),
            ),
        ) as QuestRecommendationResult.Success

        assertEquals(none.id, result.quest.id)
    }

    @Test
    fun normalPreparationPreferenceIsAlsoTreatedAsAHardMaximum() {
        val none = validFamilyQuest(id = QuestId("q1"), preparation = PreparationLevel.NONE)
        val advanced = validFamilyQuest(id = QuestId("q2"), preparation = PreparationLevel.ADVANCED)

        val result = policy.recommend(
            request(
                familyProfile = testProfile(preparationPreference = PreparationPreference.NONE),
                availableQuests = listOf(none, advanced),
            ),
        ) as QuestRecommendationResult.Success

        assertEquals(none.id, result.quest.id)
    }

    @Test
    fun explicitEnergyMatchesExactly() {
        val calm = validFamilyQuest(id = QuestId("q1"), energy = EnergyLevel.CALM)
        val active = validFamilyQuest(id = QuestId("q2"), energy = EnergyLevel.ACTIVE)

        val result = policy.recommend(
            request(
                context = EMPTY_CONTEXT.copy(energy = EnergyLevel.CALM),
                availableQuests = listOf(calm, active),
            ),
        ) as QuestRecommendationResult.Success

        assertEquals(calm.id, result.quest.id)
    }

    @Test
    fun explicitCategoryExcludesOtherCategories() {
        val talk = validFamilyQuest(id = QuestId("q1"), category = QuestCategory.TALK)
        val move = validFamilyQuest(id = QuestId("q2"), category = QuestCategory.MOVE)

        val result = policy.recommend(
            request(
                context = EMPTY_CONTEXT.copy(preferredCategory = QuestCategory.MOVE),
                availableQuests = listOf(talk, move),
            ),
        ) as QuestRecommendationResult.Success

        assertEquals(move.id, result.quest.id)
    }

    @Test
    fun explicitContextIsNeverRelaxedEvenWhenNothingQualifies() {
        val calmOnly = validFamilyQuest(energy = EnergyLevel.CALM)

        val result = policy.recommend(
            request(
                context = EMPTY_CONTEXT.copy(energy = EnergyLevel.ACTIVE),
                availableQuests = listOf(calmOnly),
            ),
        )

        assertEquals(QuestRecommendationResult.NoMatch(NoRecommendationReason.NO_CONTEXT_COMPATIBLE_QUEST), result)
    }

    @Test
    fun recentlyDismissedQuestIsExcludedWhenAnAlternativeExists() {
        val dismissed = validFamilyQuest(id = QuestId("q1"))
        val alternative = validFamilyQuest(id = QuestId("q2"))
        val history = RecommendationHistory.EMPTY.copy(
            recentlyDismissed = listOf(QuestHistoryEntry(dismissed.id, dismissed.category, NOW - 1.days)),
        )

        val result = policy.recommend(
            request(availableQuests = listOf(dismissed, alternative), history = history),
        ) as QuestRecommendationResult.Success

        assertEquals(alternative.id, result.quest.id)
    }

    @Test
    fun dismissalOutsideTheCooldownWindowNoLongerExcludes() {
        val quest = validFamilyQuest(id = QuestId("q1"))
        val history = RecommendationHistory.EMPTY.copy(
            recentlyDismissed = listOf(QuestHistoryEntry(quest.id, quest.category, NOW - 40.days)),
        )

        val result = policy.recommend(request(availableQuests = listOf(quest), history = history))

        assertTrue(result is QuestRecommendationResult.Success)
    }

    @Test
    fun questInCompletionCooldownIsExcluded() {
        val quest = validFamilyQuest(id = QuestId("q1"), cooldownDays = 7)
        val history = RecommendationHistory.EMPTY.copy(
            recentlyCompleted = listOf(QuestHistoryEntry(quest.id, quest.category, NOW - 2.days)),
        )

        val result = policy.recommend(request(availableQuests = listOf(quest), history = history))

        assertEquals(QuestRecommendationResult.NoMatch(NoRecommendationReason.ALL_CANDIDATES_IN_COOLDOWN), result)
    }

    @Test
    fun questPastItsCooldownIsEligibleAgain() {
        val quest = validFamilyQuest(id = QuestId("q1"), cooldownDays = 7)
        val history = RecommendationHistory.EMPTY.copy(
            recentlyCompleted = listOf(QuestHistoryEntry(quest.id, quest.category, NOW - 8.days)),
        )

        val result = policy.recommend(request(availableQuests = listOf(quest), history = history))

        assertTrue(result is QuestRecommendationResult.Success)
    }

    @Test
    fun zeroCooldownMeansImmediatelyReusable() {
        val quest = validFamilyQuest(id = QuestId("q1"), cooldownDays = 0)
        val history = RecommendationHistory.EMPTY.copy(
            recentlyCompleted = listOf(QuestHistoryEntry(quest.id, quest.category, NOW)),
        )

        val result = policy.recommend(request(availableQuests = listOf(quest), history = history))

        assertTrue(result is QuestRecommendationResult.Success)
    }

    // -- Scoring ------------------------------------------------------------------------------

    @Test
    fun matchingFamilyInterestIncreasesLikelihoodOfSelection() {
        val interestMatch = validFamilyQuest(id = QuestId("q1"), category = QuestCategory.CREATE)
        val noInterestMatch = validFamilyQuest(id = QuestId("q2"), category = QuestCategory.SILLY)

        val result = policy.recommend(
            request(
                familyProfile = testProfile(interests = setOf(QuestCategory.CREATE)),
                availableQuests = listOf(interestMatch, noInterestMatch),
            ),
        ) as QuestRecommendationResult.Success

        assertEquals(interestMatch.id, result.quest.id)
        assertTrue(RecommendationReason.MatchesFamilyInterest in result.reasons)
    }

    @Test
    fun matchingPreferredDurationIncreasesLikelihoodOfSelection() {
        val preferredDuration = validFamilyQuest(id = QuestId("q1"), durationMinutes = 10)
        val otherDuration = validFamilyQuest(id = QuestId("q2"), durationMinutes = 20)

        val result = policy.recommend(
            request(
                familyProfile = testProfile(preferredDurations = setOf(DurationBand.TEN_MINUTES)),
                availableQuests = listOf(preferredDuration, otherDuration),
            ),
        ) as QuestRecommendationResult.Success

        assertEquals(preferredDuration.id, result.quest.id)
        assertTrue(RecommendationReason.MatchesPreferredDuration in result.reasons)
    }

    @Test
    fun underrepresentedCategoryIncreasesLikelihoodOfSelection() {
        val rareCategory = validFamilyQuest(id = QuestId("q1"), category = QuestCategory.SILLY)
        val commonCategory = validFamilyQuest(id = QuestId("q2"), category = QuestCategory.TALK)
        val history = RecommendationHistory.EMPTY.copy(categoryCompletionCounts = mapOf(QuestCategory.TALK to 10))

        val result = policy.recommend(
            request(availableQuests = listOf(rareCategory, commonCategory), history = history),
        ) as QuestRecommendationResult.Success

        assertEquals(rareCategory.id, result.quest.id)
        assertTrue(RecommendationReason.AddsCategoryVariety in result.reasons)
    }

    @Test
    fun neverCompletedQuestIsPreferredOverARecentlyCompletedOne() {
        val neverCompleted = validFamilyQuest(id = QuestId("q1"), category = QuestCategory.TALK)
        val completedBefore = validFamilyQuest(id = QuestId("q2"), category = QuestCategory.MOVE)
        val history = RecommendationHistory.EMPTY.copy(
            recentlyCompleted = listOf(QuestHistoryEntry(completedBefore.id, completedBefore.category, NOW - 10.days)),
        )

        val result = policy.recommend(
            request(availableQuests = listOf(neverCompleted, completedBefore), history = history),
        ) as QuestRecommendationResult.Success

        assertEquals(neverCompleted.id, result.quest.id)
        assertTrue(RecommendationReason.NewToFamily in result.reasons)
    }

    @Test
    fun recommendationReasonsMatchTheScore() {
        val quest = validFamilyQuest(id = QuestId("q1"), category = QuestCategory.CREATE, durationMinutes = 10)
        val profile = testProfile(
            interests = setOf(QuestCategory.CREATE),
            preferredDurations = setOf(DurationBand.TEN_MINUTES),
        )

        val result = policy.recommend(
            request(familyProfile = profile, availableQuests = listOf(quest)),
        ) as QuestRecommendationResult.Success

        val expectedScore = result.reasons.sumOf { weightFor(it) }
        assertEquals(expectedScore, result.score)
    }

    @Test
    fun theRecentCategoryRepeatPenaltyLowersScoreEvenThoughItHasNoPositiveReason() {
        val justCompletedCategory = QuestCategory.TALK
        val candidate = validFamilyQuest(id = QuestId("q1"), category = justCompletedCategory)
        val history = RecommendationHistory.EMPTY.copy(
            recentlyCompleted = listOf(QuestHistoryEntry(QuestId("some-other-quest"), justCompletedCategory, NOW - 1.days)),
        )

        val result = policy.recommend(
            request(availableQuests = listOf(candidate), history = history),
        ) as QuestRecommendationResult.Success

        val sumOfPositiveReasons = result.reasons.sumOf { weightFor(it) }
        assertTrue(result.score < sumOfPositiveReasons)
    }

    @Test
    fun matchingPreferredEnergyIncreasesLikelihoodOfSelection() {
        val calm = validFamilyQuest(id = QuestId("q1"), energy = EnergyLevel.CALM)
        val active = validFamilyQuest(id = QuestId("q2"), energy = EnergyLevel.ACTIVE)

        val result = policy.recommend(
            request(
                familyProfile = testProfile(preferredEnergyLevels = setOf(EnergyLevel.CALM)),
                availableQuests = listOf(calm, active),
            ),
        ) as QuestRecommendationResult.Success

        assertEquals(calm.id, result.quest.id)
        assertTrue(RecommendationReason.MatchesPreferredEnergy in result.reasons)
    }

    @Test
    fun anEmptyEnergyPreferenceNeverContributesTheBonusButStillProducesAResult() {
        val quest = validFamilyQuest(id = QuestId("q1"), energy = EnergyLevel.ACTIVE)

        val result = policy.recommend(
            request(familyProfile = testProfile(preferredEnergyLevels = emptySet()), availableQuests = listOf(quest)),
        ) as QuestRecommendationResult.Success

        assertTrue(RecommendationReason.MatchesPreferredEnergy !in result.reasons)
    }

    /**
     * Unlike preparation, a family's normal energy preference is never a hard filter — a quest
     * whose energy doesn't match still wins when it's the only candidate (safe fallback to the
     * wider catalogue, never an empty Today).
     */
    @Test
    fun nonMatchingEnergyPreferenceNeverExcludesAQuest() {
        val onlyOption = validFamilyQuest(id = QuestId("q1"), energy = EnergyLevel.ACTIVE)

        val result = policy.recommend(
            request(familyProfile = testProfile(preferredEnergyLevels = setOf(EnergyLevel.CALM)), availableQuests = listOf(onlyOption)),
        )

        assertTrue(result is QuestRecommendationResult.Success)
        assertEquals(onlyOption.id, result.quest.id)
    }

    @Test
    fun explicitEnergyRequestTakesPrecedenceOverTheNormalEnergyPreferenceBonus() {
        // context.energy already hard-filtered to CALM elsewhere (explicitEnergyMatchesExactly) —
        // this proves the *scoring* bonus doesn't double-count when the ask was already explicit.
        val quest = validFamilyQuest(id = QuestId("q1"), energy = EnergyLevel.CALM)

        val result = policy.recommend(
            request(
                familyProfile = testProfile(preferredEnergyLevels = setOf(EnergyLevel.CALM)),
                context = EMPTY_CONTEXT.copy(energy = EnergyLevel.CALM),
                availableQuests = listOf(quest),
            ),
        ) as QuestRecommendationResult.Success

        assertTrue(RecommendationReason.MatchesPreferredEnergy !in result.reasons)
        assertTrue(RecommendationReason.MatchesRequestedContext in result.reasons)
    }

    // -- Free/Family Plus access behavior ------------------------------------------------------

    /**
     * The policy has no concept of [com.togetherly.domain.quest.QuestAccess] at all — access
     * enforcement happens downstream, at [com.togetherly.domain.completion.usecase.StartQuest]
     * time, never here (see this class's own KDoc: filtering/scoring is purely age/context/
     * cooldown/preference-based). A premium-only quest is scored and can win exactly like a free
     * one; whether a free family can actually *start* it as directly-startable is a separate
     * concern this policy deliberately doesn't decide — preferences never grant or restrict access.
     */
    @Test
    fun premiumAccessQuestsAreScoredIdenticallyToFreeQuestsRegardlessOfPreferenceMatch() {
        val premiumMatch = validFamilyQuest(
            id = QuestId("q1"),
            energy = EnergyLevel.CALM,
            access = QuestAccess.Premium(EntitlementId("family_plus")),
        )
        val freeNonMatch = validFamilyQuest(id = QuestId("q2"), energy = EnergyLevel.ACTIVE, access = QuestAccess.Free)

        val result = policy.recommend(
            request(
                familyProfile = testProfile(preferredEnergyLevels = setOf(EnergyLevel.CALM)),
                availableQuests = listOf(premiumMatch, freeNonMatch),
            ),
        ) as QuestRecommendationResult.Success

        // The premium quest wins purely on preference match — its QuestAccess never factored in.
        assertEquals(premiumMatch.id, result.quest.id)
        assertEquals(QuestAccess.Premium(EntitlementId("family_plus")), result.quest.access)
    }

    // -- Determinism ----------------------------------------------------------------------------

    @Test
    fun sameInputsProduceTheSameQuest() {
        val quests = (1..5).map {
            validFamilyQuest(id = QuestId("quest-$it"), category = QuestCategory.entries[it % QuestCategory.entries.size])
        }
        val req = request(availableQuests = quests)

        val first = policy.recommend(req)
        val second = policy.recommend(req)

        assertEquals(first, second)
    }

    @Test
    fun inputListOrderingDoesNotChangeTheResult() {
        val quests = (1..5).map { validFamilyQuest(id = QuestId("quest-$it")) }

        val forward = policy.recommend(request(availableQuests = quests))
        val reversed = policy.recommend(request(availableQuests = quests.reversed()))

        assertEquals(forward, reversed)
    }

    @Test
    fun increasingSelectionIndexCanChangeTheWinnerAmongTiedCandidates() {
        // Two quests identical in every scored dimension tie on score — the only thing that can
        // differ between them is the tie-break key, which incorporates selectionIndex.
        val a = validFamilyQuest(id = QuestId("tie-a"))
        val b = validFamilyQuest(id = QuestId("tie-b"))
        val quests = listOf(a, b)

        val winnersAcrossIndices = (0..20).map { index ->
            (policy.recommend(request(availableQuests = quests, selectionIndex = index)) as QuestRecommendationResult.Success)
                .quest.id
        }.toSet()

        assertEquals(setOf(a.id, b.id), winnersAcrossIndices)
    }

    @Test
    fun rerollExcludingTheJustDismissedQuestAlwaysSelectsTheAlternativeWhenOneExists() {
        val current = validFamilyQuest(id = QuestId("q1"))
        val alternative = validFamilyQuest(id = QuestId("q2"))
        val history = RecommendationHistory.EMPTY.copy(
            recentlyDismissed = listOf(QuestHistoryEntry(current.id, current.category, NOW)),
        )

        val result = policy.recommend(
            request(availableQuests = listOf(current, alternative), history = history, selectionIndex = 1),
        ) as QuestRecommendationResult.Success

        assertEquals(alternative.id, result.quest.id)
    }

    /**
     * A JVM-computed fixed fixture — the same [stableHash] inputs must produce the same tie-break
     * key on Android and iOS. See [StableHashTest] for the hash function's own fixed-value test;
     * this proves the full pipeline (filtering → scoring tie → tie-break) resolves to one
     * predictable winner end to end.
     */
    @Test
    fun deterministicFixtureProducesTheExpectedQuestId() {
        val a = validFamilyQuest(id = QuestId("fixture-a"))
        val b = validFamilyQuest(id = QuestId("fixture-b"))
        val c = validFamilyQuest(id = QuestId("fixture-c"))

        val result = policy.recommend(
            request(
                familyProfile = testProfile(childAgeBands = setOf(AgeBand.AGE_6_TO_8)),
                availableQuests = listOf(a, b, c),
                localDate = LocalDate(2026, 1, 1),
                selectionIndex = 0,
            ),
        ) as QuestRecommendationResult.Success

        // Computed once (see this project's own commit history / CI run) and pinned here — a
        // regression that silently changes the tie-break outcome for this fixed fixture should
        // fail loudly, not be "corrected" by re-running and re-pinning without understanding why.
        assertEquals(QuestId("fixture-a"), result.quest.id)
    }
}

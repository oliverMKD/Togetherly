package com.togetherly.domain.recommendation

import com.togetherly.domain.daily.QuestContext
import com.togetherly.domain.family.FamilyProfile
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.matches
import com.togetherly.domain.quest.supports
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * The production [QuestRecommendationPolicy] — pure, synchronous, and deterministic: the same
 * [QuestRecommendationRequest] always produces the same [QuestRecommendationResult], on Android
 * and iOS alike (see [stableHash]'s own KDoc for the cross-platform guarantee that rests on).
 *
 * ## Filtering pipeline (in order — each stage narrows the previous stage's survivors)
 * 1. [QuestRecommendationRequest.availableQuests] non-empty, else [NoRecommendationReason.EMPTY_CATALOGUE].
 * 2. [FamilyQuest.supports] every [FamilyProfile.childAgeBands], else [NoRecommendationReason.NO_AGE_COMPATIBLE_QUEST].
 *    Never relaxed — age compatibility is a safety constraint.
 * 3. Explicit [QuestContext] fields (duration, location, energy, preferred category) each hard-exclude
 *    when set — explicit context is always stricter than a normal family preference — **plus**
 *    preparation, which is *always* a hard ceiling (explicit [QuestContext.preparation] if given,
 *    the family's normal [FamilyProfile.preparationPreference] otherwise; see
 *    [asPreparationCeiling]'s own KDoc for why preparation alone gets this treatment even absent
 *    an explicit ask — a practical safety-adjacent constraint, not just a taste preference). Empty
 *    survivors here mean [NoRecommendationReason.NO_CONTEXT_COMPATIBLE_QUEST].
 * 4. Recently-dismissed (within [RecommendationConfig.dismissalCooldown] of [QuestRecommendationRequest.now])
 *    and per-quest completion cooldown ([FamilyQuest.cooldownDays], zero meaning immediately
 *    reusable) are excluded together. Empty survivors here mean
 *    [NoRecommendationReason.ALL_CANDIDATES_IN_COOLDOWN] — cooldown is never silently violated;
 *    a coordinating use case may later decide to relax it, this policy does not.
 *
 * ## Scoring and selection
 * Every surviving candidate is scored (see [RecommendationConfig]'s own KDoc for the full
 * weight-to-reason mapping) and sorted by score descending, then by a stable tie-break key, then
 * by [com.togetherly.domain.quest.QuestId] as a final deterministic fallback — never an unseeded
 * random choice. The tie-break key is [stableHash] of
 * `familyId|localDate|selectionIndex|questId`: changing [QuestRecommendationRequest.selectionIndex]
 * (as a reroll does) reshuffles tie order among similarly-scored candidates, while the *primary*
 * mechanism guaranteeing a reroll actually changes the outcome is that the just-rerolled quest is
 * already excluded via the dismissal filter above (see
 * [com.togetherly.domain.daily.usecase.RerollDailyQuest]) — selectionIndex is a secondary,
 * supplementary tie-breaker, not the main reason a reroll differs.
 */
class DeterministicQuestRecommendationPolicy(
    private val config: RecommendationConfig = RecommendationConfig.DEFAULT,
) : QuestRecommendationPolicy {

    override fun recommend(request: QuestRecommendationRequest): QuestRecommendationResult {
        if (request.availableQuests.isEmpty()) {
            return QuestRecommendationResult.NoMatch(NoRecommendationReason.EMPTY_CATALOGUE)
        }

        val ageCompatible = request.availableQuests.filter { it.supports(request.familyProfile.childAgeBands) }
        if (ageCompatible.isEmpty()) {
            return QuestRecommendationResult.NoMatch(NoRecommendationReason.NO_AGE_COMPATIBLE_QUEST)
        }

        val contextCompatible = ageCompatible.filter {
            it.passesContextAndPreparationFilters(request.context, request.familyProfile)
        }
        if (contextCompatible.isEmpty()) {
            return QuestRecommendationResult.NoMatch(NoRecommendationReason.NO_CONTEXT_COMPATIBLE_QUEST)
        }

        val available = contextCompatible.filterNot {
            it.isExcludedByDismissalOrCooldown(request.history, request.now, config.dismissalCooldown)
        }
        if (available.isEmpty()) {
            return QuestRecommendationResult.NoMatch(NoRecommendationReason.ALL_CANDIDATES_IN_COOLDOWN)
        }

        val winner = available
            .map { scoreCandidate(it, request, config) }
            .sortedWith(
                compareByDescending<ScoredCandidate> { it.score }
                    .thenBy { it.tieBreakKey }
                    .thenBy { it.quest.id.value },
            )
            .first()

        return QuestRecommendationResult.Success(
            quest = winner.quest,
            score = winner.score,
            reasons = winner.reasons,
        )
    }
}

private data class ScoredCandidate(
    val quest: FamilyQuest,
    val score: Int,
    val reasons: List<RecommendationReason>,
    val tieBreakKey: Long,
)

private fun FamilyQuest.passesContextAndPreparationFilters(
    context: QuestContext,
    familyProfile: FamilyProfile,
): Boolean {
    context.duration?.let { if (durationBandFor(durationMinutes) != it) return false }
    context.location?.let { if (!matches(it)) return false }
    context.energy?.let { if (energy != it) return false }
    context.preferredCategory?.let { if (category != it) return false }

    val preparationCeiling = context.preparation ?: familyProfile.preparationPreference.asPreparationCeiling()
    if (preparation.ordinal > preparationCeiling.ordinal) return false

    return true
}

private fun FamilyQuest.isExcludedByDismissalOrCooldown(
    history: RecommendationHistory,
    now: Instant,
    dismissalCooldown: Duration,
): Boolean {
    val dismissedRecently = history.recentlyDismissed.any { entry ->
        entry.questId == id && (now - entry.occurredAt) < dismissalCooldown
    }
    if (dismissedRecently) return true

    if (cooldownDays <= 0) return false
    val mostRecentCompletion = history.recentlyCompleted
        .filter { it.questId == id }
        .maxByOrNull { it.occurredAt }
        ?: return false
    return (now - mostRecentCompletion.occurredAt) < cooldownDays.days
}

private fun scoreCandidate(
    quest: FamilyQuest,
    request: QuestRecommendationRequest,
    config: RecommendationConfig,
): ScoredCandidate {
    var score = 0
    val reasons = mutableListOf<RecommendationReason>()

    if (quest.category in request.familyProfile.interests) {
        score += config.matchesFamilyInterest
        reasons += RecommendationReason.MatchesFamilyInterest
    }

    val matchesPreferredDuration = if (request.context.duration != null) {
        true // already guaranteed by the hard filter when duration was explicitly requested
    } else {
        durationBandFor(quest.durationMinutes) in request.familyProfile.preferredDurations
    }
    if (matchesPreferredDuration) {
        score += config.matchesPreferredDuration
        reasons += RecommendationReason.MatchesPreferredDuration
    }

    val hasOtherExplicitContext = request.context.location != null ||
        request.context.energy != null ||
        request.context.preparation != null ||
        request.context.preferredCategory != null
    if (hasOtherExplicitContext) {
        score += config.matchesRequestedContext
        reasons += RecommendationReason.MatchesRequestedContext
    }

    val ownCompletions = request.history.recentlyCompleted.filter { it.questId == quest.id }
    if (ownCompletions.isEmpty()) {
        score += config.newToFamily
        reasons += RecommendationReason.NewToFamily
    }

    if (isCategoryUnderrepresented(quest.category, request.history.categoryCompletionCounts)) {
        score += config.addsCategoryVariety
        reasons += RecommendationReason.AddsCategoryVariety
    }

    if (request.context.location == null && quest.matchesLocationPreference(request.familyProfile.locationPreference)) {
        score += config.matchesLocationPreference
        reasons += RecommendationReason.MatchesLocationPreference
    }

    if (request.context.preparation == null) {
        // Guaranteed true by the hard filter above (the family's own preference supplied the
        // ceiling) — scored anyway for transparency, matching RecommendationConfig's weight table.
        score += config.matchesPreparationPreference
        reasons += RecommendationReason.MatchesPreparationPreference
    }

    // Unlike preparation, energy is never a hard filter — an empty preferredEnergyLevels set (the
    // default) simply never contributes this bonus, for any quest. See RecommendationReason.MatchesPreferredEnergy.
    if (request.context.energy == null && quest.energy in request.familyProfile.preferredEnergyLevels) {
        score += config.matchesPreferredEnergy
        reasons += RecommendationReason.MatchesPreferredEnergy
    }

    val lastTwoCompletedCategories = request.history.recentlyCompleted
        .sortedByDescending { it.occurredAt }
        .take(2)
        .mapNotNull { it.category }
    if (quest.category in lastTwoCompletedCategories) {
        score += config.recentCategoryRepeatPenalty
    } else if (ownCompletions.isNotEmpty()) {
        reasons += RecommendationReason.LeastRecentlyCompleted
    }

    val tieBreakKey = stableHash(
        "${request.familyProfile.id.value}|${request.localDate}|${request.selectionIndex}|${quest.id.value}",
    )

    return ScoredCandidate(quest, score, reasons.toList(), tieBreakKey)
}

private fun isCategoryUnderrepresented(category: QuestCategory, counts: Map<QuestCategory, Int>): Boolean {
    if (counts.isEmpty()) return true
    val average = QuestCategory.entries.map { counts[it] ?: 0 }.average()
    val thisCount = counts[category] ?: 0
    return thisCount < average
}

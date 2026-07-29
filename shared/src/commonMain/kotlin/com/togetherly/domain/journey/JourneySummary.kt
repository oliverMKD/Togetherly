package com.togetherly.domain.journey

import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.validation.requireNonNegative
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Always derive this via [deriveJourneySummary] rather than constructing it directly — its
 * fields only make sense as a computation over actual completions, not arbitrary values. The
 * constructor stays internal (not private) so this module's own tests can exercise the
 * invariant checks directly, the same pattern used by [com.togetherly.domain.purchase.FamilyAccess].
 * No streak field exists yet — see [deriveJourneySummary]'s own KDoc.
 */
data class JourneySummary internal constructor(
    val totalCompletions: Int,
    val completionsByCategory: Map<QuestCategory, Int>,
    val activeDays: Set<LocalDate>,
    val memoriesWithPhoto: Int,
    val memoriesWithVoice: Int,
) {
    init {
        requireNonNegative(totalCompletions)
        completionsByCategory.values.forEach { requireNonNegative(it) }
        requireNonNegative(memoriesWithPhoto)
        requireNonNegative(memoriesWithVoice)
    }
}

/**
 * [timeZone] is required explicitly, not defaulted or read from the system, since converting
 * [JourneyEntry.completedAt] into a day requires one — the same reason streaks aren't modeled
 * yet without a defined timezone policy. A completion always counts toward [JourneySummary.totalCompletions]
 * even when its quest content is missing; only the per-category breakdown is limited to entries
 * whose quest resolved.
 */
fun deriveJourneySummary(
    entries: List<JourneyEntry>,
    timeZone: TimeZone,
): JourneySummary {
    val completionsByCategory = entries
        .mapNotNull { it.quest?.category }
        .groupingBy { it }
        .eachCount()
    val activeDays = entries
        .map { it.completedAt.toLocalDateTime(timeZone).date }
        .toSet()

    return JourneySummary(
        totalCompletions = entries.size,
        completionsByCategory = completionsByCategory,
        activeDays = activeDays,
        memoriesWithPhoto = entries.count { entry -> entry.completion.media.any { it is MemoryMedia.Photo } },
        memoriesWithVoice = entries.count { entry -> entry.completion.media.any { it is MemoryMedia.Voice } },
    )
}

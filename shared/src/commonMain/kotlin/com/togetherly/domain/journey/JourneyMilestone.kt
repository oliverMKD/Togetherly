package com.togetherly.domain.journey

/**
 * Gentle, typed completion-count milestones — never a competitive rank or a punitive streak.
 * Deliberately holds no user-facing copy: a UI layer (Step 10.6) maps each constant onto its own
 * localized string, the same separation [com.togetherly.core.error.AppError] keeps from
 * [com.togetherly.core.ui.UiText].
 */
enum class JourneyMilestone(val requiredCompletions: Int) {
    FIRST_STAR(1),
    THREE_STARS(3),
    SEVEN_STARS(7),
    FOURTEEN_STARS(14),
    THIRTY_STARS(30),
}

/** Every milestone a family has already reached at [totalCompletions] — never a "next milestone" countdown, which would read as a target to chase rather than progress made. */
fun achievedJourneyMilestones(totalCompletions: Int): Set<JourneyMilestone> =
    JourneyMilestone.entries.filterTo(mutableSetOf()) { totalCompletions >= it.requiredCompletions }

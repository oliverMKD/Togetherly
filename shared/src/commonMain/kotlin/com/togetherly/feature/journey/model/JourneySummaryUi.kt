package com.togetherly.feature.journey.model

import androidx.compose.runtime.Immutable
import com.togetherly.domain.journey.JourneyMilestone
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf

/**
 * [achievedMilestones] holds every milestone reached so far, not just the most recent one — the
 * constellation header (Step 10.6) picks whichever one it wants to show gentle copy for; this
 * model doesn't decide that itself.
 */
@Immutable
data class JourneySummaryUi(
    val totalCompletions: Int,
    val achievedMilestones: PersistentSet<JourneyMilestone> = persistentSetOf(),
)

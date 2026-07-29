package com.togetherly.feature.journey.mapper

import com.togetherly.core.ui.UiText
import com.togetherly.domain.journey.JourneyMilestone
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.journey_milestone_first_star
import togetherly.shared.generated.resources.journey_milestone_fourteen_stars
import togetherly.shared.generated.resources.journey_milestone_seven_stars
import togetherly.shared.generated.resources.journey_milestone_thirty_stars
import togetherly.shared.generated.resources.journey_milestone_three_stars

fun JourneyMilestone.copy(): UiText = UiText.Resource(
    when (this) {
        JourneyMilestone.FIRST_STAR -> Res.string.journey_milestone_first_star
        JourneyMilestone.THREE_STARS -> Res.string.journey_milestone_three_stars
        JourneyMilestone.SEVEN_STARS -> Res.string.journey_milestone_seven_stars
        JourneyMilestone.FOURTEEN_STARS -> Res.string.journey_milestone_fourteen_stars
        JourneyMilestone.THIRTY_STARS -> Res.string.journey_milestone_thirty_stars
    },
)

/** The single most-recently-reached milestone worth celebrating — never every achieved milestone at once, which would read as a checklist rather than a moment. */
fun Set<JourneyMilestone>.latestOrNull(): JourneyMilestone? = maxByOrNull { it.requiredCompletions }

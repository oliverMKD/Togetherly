package com.togetherly.domain.journey

import kotlin.test.Test
import kotlin.test.assertEquals

class JourneyMilestoneTest {

    @Test
    fun noCompletionsReachesNoMilestone() {
        assertEquals(emptySet(), achievedJourneyMilestones(0))
    }

    @Test
    fun oneCompletionReachesOnlyTheFirstStar() {
        assertEquals(setOf(JourneyMilestone.FIRST_STAR), achievedJourneyMilestones(1))
    }

    @Test
    fun betweenMilestonesKeepsThePreviousOnesAchieved() {
        assertEquals(
            setOf(JourneyMilestone.FIRST_STAR, JourneyMilestone.THREE_STARS),
            achievedJourneyMilestones(5),
        )
    }

    @Test
    fun everyMilestoneIsAchievedAtThirtyCompletions() {
        assertEquals(JourneyMilestone.entries.toSet(), achievedJourneyMilestones(30))
    }

    @Test
    fun exceedingThirtyStillHasEveryMilestoneAchieved() {
        assertEquals(JourneyMilestone.entries.toSet(), achievedJourneyMilestones(100))
    }
}

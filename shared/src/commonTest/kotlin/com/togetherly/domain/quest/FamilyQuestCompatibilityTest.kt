package com.togetherly.domain.quest

import com.togetherly.domain.family.AgeBand
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FamilyQuestCompatibilityTest {

    @Test
    fun questSupportsAllFamilyAgeBands() {
        val quest = validFamilyQuest(ageBands = setOf(AgeBand.AGE_6_TO_8, AgeBand.AGE_9_TO_11))

        assertTrue(quest.supports(setOf(AgeBand.AGE_6_TO_8, AgeBand.AGE_9_TO_11)))
    }

    @Test
    fun questFailsWhenOneConfiguredAgeBandIsUnsupported() {
        val quest = validFamilyQuest(ageBands = setOf(AgeBand.AGE_6_TO_8))

        assertFalse(quest.supports(setOf(AgeBand.AGE_6_TO_8, AgeBand.AGE_9_TO_11)))
    }

    @Test
    fun indoorOutdoorEitherMatchingWorksCorrectly() {
        val indoorQuest = validFamilyQuest(location = QuestLocation.INDOOR)
        val outdoorQuest = validFamilyQuest(location = QuestLocation.OUTDOOR)
        val eitherQuest = validFamilyQuest(location = QuestLocation.EITHER)

        assertTrue(indoorQuest.matches(QuestLocation.INDOOR))
        assertFalse(indoorQuest.matches(QuestLocation.OUTDOOR))

        assertTrue(outdoorQuest.matches(QuestLocation.OUTDOOR))
        assertFalse(outdoorQuest.matches(QuestLocation.INDOOR))

        assertTrue(eitherQuest.matches(QuestLocation.INDOOR))
        assertTrue(eitherQuest.matches(QuestLocation.OUTDOOR))
    }
}

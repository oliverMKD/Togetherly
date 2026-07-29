package com.togetherly.domain.explore.usecase

import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestSummary
import com.togetherly.domain.quest.QuestTitle
import com.togetherly.domain.quest.validFamilyQuest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchQuestsUseCaseTest {

    private val useCase = SearchQuestsUseCase()

    private val scavengerHunt = validFamilyQuest(
        id = QuestId("quest-1"),
        title = QuestTitle("Backyard Scavenger Hunt"),
        summary = QuestSummary("Find five hidden treasures together."),
        category = QuestCategory.DISCOVER,
    )
    private val dancePart = validFamilyQuest(
        id = QuestId("quest-2"),
        title = QuestTitle("Three-Song Dance Party"),
        summary = QuestSummary("Take turns picking a favorite song and dance it out."),
        category = QuestCategory.SILLY,
    )
    private val quests = listOf(scavengerHunt, dancePart)

    @Test
    fun emptyQueryReturnsTheUnfilteredCatalogue() {
        val result = useCase(quests, "")

        assertEquals(quests, result)
    }

    @Test
    fun blankQueryAfterTrimmingReturnsTheUnfilteredCatalogue() {
        val result = useCase(quests, "   ")

        assertEquals(quests, result)
    }

    @Test
    fun searchIsCaseInsensitive() {
        val lower = useCase(quests, "scavenger")
        val upper = useCase(quests, "SCAVENGER")
        val mixed = useCase(quests, "ScAvEngEr")

        assertEquals(listOf(scavengerHunt), lower)
        assertEquals(listOf(scavengerHunt), upper)
        assertEquals(listOf(scavengerHunt), mixed)
    }

    @Test
    fun matchesByTitle() {
        val result = useCase(quests, "Dance Party")

        assertEquals(listOf(dancePart), result)
    }

    @Test
    fun matchesBySummary() {
        val result = useCase(quests, "hidden treasures")

        assertEquals(listOf(scavengerHunt), result)
    }

    @Test
    fun matchesByCategoryStandingInForTags() {
        val result = useCase(quests, "silly")

        assertEquals(listOf(dancePart), result)
    }

    @Test
    fun surroundingWhitespaceIsTrimmedBeforeMatching() {
        val result = useCase(quests, "   dance party   ")

        assertEquals(listOf(dancePart), result)
    }

    @Test
    fun noMatchesReturnsAnEmptyList() {
        val result = useCase(quests, "underwater volcano")

        assertTrue(result.isEmpty())
    }
}

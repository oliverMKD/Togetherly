package com.togetherly.domain.explore.usecase

import com.togetherly.domain.explore.ExploreFilters
import com.togetherly.domain.explore.QuestAccessFilter
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestLocation
import com.togetherly.domain.quest.QuestSummary
import com.togetherly.domain.quest.QuestTitle
import com.togetherly.domain.quest.validFamilyQuest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilterQuestsUseCaseTest {

    private val useCase = FilterQuestsUseCase()

    private val calmIndoorQuest = validFamilyQuest(
        id = QuestId("quest-1"),
        title = QuestTitle("Quiet Story Time"),
        category = QuestCategory.TALK,
        energy = EnergyLevel.CALM,
        location = QuestLocation.INDOOR,
        durationMinutes = 10,
        ageBands = setOf(AgeBand.AGE_6_TO_8),
        access = QuestAccess.Free,
    )
    private val activeOutdoorQuest = validFamilyQuest(
        id = QuestId("quest-2"),
        title = QuestTitle("Backyard Relay Race"),
        category = QuestCategory.MOVE,
        energy = EnergyLevel.ACTIVE,
        location = QuestLocation.OUTDOOR,
        durationMinutes = 25,
        ageBands = setOf(AgeBand.AGE_9_TO_11),
        access = QuestAccess.Premium(EntitlementId("family_plus")),
    )
    private val quests = listOf(calmIndoorQuest, activeOutdoorQuest)

    @Test
    fun noFiltersReturnsEveryQuestInTheOriginalOrder() {
        val result = useCase(quests, ExploreFilters())

        assertEquals(quests, result)
    }

    @Test
    fun oneActiveFilterNarrowsToMatchingQuests() {
        val result = useCase(quests, ExploreFilters(category = QuestCategory.MOVE))

        assertEquals(listOf(activeOutdoorQuest), result)
    }

    @Test
    fun multipleActiveFiltersCombineWithAnd() {
        val matchesBoth = useCase(
            quests,
            ExploreFilters(energy = EnergyLevel.ACTIVE, location = QuestLocation.OUTDOOR),
        )
        val matchesNeitherTogether = useCase(
            quests,
            ExploreFilters(energy = EnergyLevel.ACTIVE, location = QuestLocation.INDOOR),
        )

        assertEquals(listOf(activeOutdoorQuest), matchesBoth)
        assertTrue(matchesNeitherTogether.isEmpty())
    }

    @Test
    fun durationFilterBucketsAgainstTheSharedDurationBandMapping() {
        val result = useCase(quests, ExploreFilters(duration = DurationBand.TEN_MINUTES))

        assertEquals(listOf(calmIndoorQuest), result)
    }

    @Test
    fun ageBandFilterMatchesAnySupportedBand() {
        val result = useCase(quests, ExploreFilters(ageBand = AgeBand.AGE_9_TO_11))

        assertEquals(listOf(activeOutdoorQuest), result)
    }

    @Test
    fun accessFilterNeverExcludesLockedPremiumContentFromResults() {
        // QuestAccessFilter.ALL (the default) must never hide premium content — access level
        // affects *availability* (locked/unlocked presentation), never whether it appears at all.
        val allAccess = useCase(quests, ExploreFilters(access = QuestAccessFilter.ALL))
        val premiumOnly = useCase(quests, ExploreFilters(access = QuestAccessFilter.PREMIUM))
        val freeOnly = useCase(quests, ExploreFilters(access = QuestAccessFilter.FREE))

        assertEquals(quests, allAccess)
        assertEquals(listOf(activeOutdoorQuest), premiumOnly)
        assertEquals(listOf(calmIndoorQuest), freeOnly)
    }

    @Test
    fun resultsAreDeterministicAcrossRepeatedCalls() {
        val filters = ExploreFilters(location = QuestLocation.OUTDOOR)

        val first = useCase(quests, filters)
        val second = useCase(quests, filters)
        val third = useCase(quests, filters)

        assertEquals(first, second)
        assertEquals(second, third)
    }

    @Test
    fun searchCombinedWithFiltersAppliesBothAsAnAnd() {
        val searchUseCase = SearchQuestsUseCase()
        val extraCalmQuest = validFamilyQuest(
            id = QuestId("quest-3"),
            title = QuestTitle("Calm Breathing Break"),
            summary = QuestSummary("A one-minute breathing exercise together."),
            category = QuestCategory.TALK,
            energy = EnergyLevel.CALM,
            location = QuestLocation.OUTDOOR,
        )
        val catalogue = quests + extraCalmQuest

        val searched = searchUseCase(catalogue, "calm")
        val result = useCase(searched, ExploreFilters(location = QuestLocation.OUTDOOR))

        assertEquals(listOf(extraCalmQuest), result)
    }
}
